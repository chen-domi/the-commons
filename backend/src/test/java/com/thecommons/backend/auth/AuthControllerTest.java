package com.thecommons.backend.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecommons.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AuthController.class,
        properties = {
                "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
        })
@Import(SecurityConfig.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private BcOidcUserService bcOidcUserService;

    @Test
    void getCurrentUserReturnsDatabaseRole() throws Exception {
        AppUser appUser = new AppUser(
                "google-subject-123",
                "admin@bc.edu",
                "Database Name");
        appUser.changeGlobalRole(GlobalRole.ADMIN);
        when(appUserService.getByGoogleSubject("google-subject-123"))
                .thenReturn(appUser);

        mockMvc.perform(get("/api/auth/me")
                        .with(oidcLogin().idToken(token -> token
                                .subject("google-subject-123")
                                .claim("name", "Google Name")
                                .claim("email", "admin@bc.edu")
                                .claim("picture", "https://example.com/photo.jpg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Google Name"))
                .andExpect(jsonPath("$.email").value("admin@bc.edu"))
                .andExpect(jsonPath("$.pictureUrl")
                        .value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.globalRole").value("ADMIN"));

        verify(appUserService).getByGoogleSubject("google-subject-123");
    }
}
