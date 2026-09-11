package com.thecommons.backend.organization;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecommons.backend.auth.BcOidcUserService;
import com.thecommons.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AdminOrganizationController.class,
        properties = {
                "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
        })
@Import(SecurityConfig.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class AdminOrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private BcOidcUserService bcOidcUserService;

    @Test
    void createOrganizationAllowsAdmin() throws Exception {
        Organization organization = new Organization("UGBC", "secret-hash");
        ReflectionTestUtils.setField(organization, "id", 1L);
        when(organizationService.createOrganization("UGBC", "1234"))
                .thenReturn(organization);

        mockMvc.perform(post("/api/admin/organizations")
                        .with(oidcLogin().authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("UGBC"))
                .andExpect(jsonPath("$.joinCode").doesNotExist())
                .andExpect(jsonPath("$.joinCodeHash").doesNotExist());

        verify(organizationService).createOrganization("UGBC", "1234");
    }

    @Test
    void createOrganizationRejectsNormalUser() throws Exception {
        mockMvc.perform(post("/api/admin/organizations")
                        .with(oidcLogin().authorities(
                                new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(organizationService);
    }

    @Test
    void createOrganizationRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/admin/organizations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(organizationService);
    }

    private String validRequestJson() {
        return """
                {
                  "name": "UGBC",
                  "joinCode": "1234"
                }
                """;
    }
}
