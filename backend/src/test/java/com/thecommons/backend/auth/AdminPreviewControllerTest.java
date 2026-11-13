package com.thecommons.backend.auth;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.thecommons.backend.config.SecurityConfig;
import com.thecommons.backend.organization.Organization;
import com.thecommons.backend.organization.OrganizationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = {AdminPreviewController.class, AuthController.class}, properties = {
    "spring.security.oauth2.client.registration.google.client-id=test",
    "spring.security.oauth2.client.registration.google.client-secret=test"
})
@Import(SecurityConfig.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class AdminPreviewControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AppUserService users;
    @MockitoBean OrganizationRepository organizations;
    @MockitoBean BcOidcUserService oidcService;
    AppUser admin;

    @BeforeEach void setup() {
        admin = new AppUser("admin", "admin@bc.edu", "Admin");
        admin.changeGlobalRole(GlobalRole.ADMIN);
    }

    @Test void previewPersistsAcrossRequestsAndCanExitWithoutChangingRole() throws Exception {
        when(users.getByGoogleSubject("admin")).thenReturn(admin);
        when(organizations.findByNameIgnoreCase("4Boston"))
                .thenReturn(Optional.of(new Organization("4Boston", "hash")));
        var session = new MockHttpSession();
        var login = oidcLogin().idToken(token -> token.subject("admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(post("/api/auth/preview").session(session).with(login).with(csrf())
                .contentType("application/json").content("{\"organizationName\":\"4Boston\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session).with(login))
                .andExpect(jsonPath("$.globalRole").value("USER"))
                .andExpect(jsonPath("$.previewOrganization").value("4Boston"));
        mvc.perform(post("/api/admin/organizations").session(session).with(login).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/auth/preview").session(session).with(login).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session).with(login))
                .andExpect(jsonPath("$.globalRole").value("ADMIN"));
        org.junit.jupiter.api.Assertions.assertEquals(GlobalRole.ADMIN, admin.getGlobalRole());
    }

    @Test void regularUserCannotEnterOrExitPreview() throws Exception {
        when(users.getByGoogleSubject("user")).thenReturn(new AppUser("user", "user@bc.edu", "User"));
        var login = oidcLogin().idToken(token -> token.subject("user"));
        mvc.perform(post("/api/auth/preview").with(login).with(csrf())
                .contentType("application/json").content("{\"organizationName\":\"4Boston\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/auth/preview").with(login).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test void previewRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(post("/api/auth/preview").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/preview").with(oidcLogin())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/auth/preview").with(oidcLogin())).andExpect(status().isForbidden());
    }

    @Test void rejectsUnknownOrganization() throws Exception {
        when(users.getByGoogleSubject("admin")).thenReturn(admin);
        when(organizations.findByNameIgnoreCase("Missing")).thenReturn(Optional.empty());
        mvc.perform(post("/api/auth/preview").with(oidcLogin().idToken(token -> token.subject("admin")))
                .with(csrf()).contentType("application/json").content("{\"organizationName\":\"Missing\"}"))
                .andExpect(status().isNotFound());
    }
}
