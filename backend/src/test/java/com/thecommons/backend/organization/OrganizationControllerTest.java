package com.thecommons.backend.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import com.thecommons.backend.auth.BcOidcUserService;
import com.thecommons.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = OrganizationController.class,
        properties = {
                "spring.security.oauth2.client.registration.google.client-id=test-client-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
        })
@Import(SecurityConfig.class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private OrganizationMembershipService membershipService;

    @MockitoBean
    private BcOidcUserService bcOidcUserService;

    @Test
    void getAllOrganizationsReturnsSafeOrganizationDetails() throws Exception {
        Organization organization = new Organization("UGBC", "secret-hash");
        ReflectionTestUtils.setField(organization, "id", 1L);
        when(organizationService.getAllOrganizations())
                .thenReturn(List.of(organization));

        mockMvc.perform(get("/api/organizations").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("UGBC"))
                .andExpect(jsonPath("$[0].joinCodeHash").doesNotExist());

        verify(organizationService).getAllOrganizations();
    }

    @Test
    void joinOrganizationUsesAuthenticatedGoogleSubject() throws Exception {
        Organization organization = new Organization("UGBC", "secret-hash");
        ReflectionTestUtils.setField(organization, "id", 1L);
        OrganizationMembership membership =
                new OrganizationMembership(null, organization);
        when(membershipService.joinOrganization(
                "google-subject-123",
                "UGBC",
                "1234"))
                .thenReturn(membership);

        mockMvc.perform(post("/api/organizations/join")
                        .with(oidcLogin().idToken(token ->
                                token.subject("google-subject-123")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "UGBC",
                                  "joinCode": "1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(1))
                .andExpect(jsonPath("$.organizationName").value("UGBC"))
                .andExpect(jsonPath("$.role").value("EBOARD"))
                .andExpect(jsonPath("$.joinCode").doesNotExist())
                .andExpect(jsonPath("$.joinCodeHash").doesNotExist());

        verify(membershipService).joinOrganization(
                "google-subject-123",
                "UGBC",
                "1234");
    }

    @Test
    void joinOrganizationRejectsNonFourDigitCode() throws Exception {
        mockMvc.perform(post("/api/organizations/join")
                        .with(oidcLogin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "UGBC",
                                  "joinCode": "12"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(membershipService, never()).joinOrganization(
                any(),
                any(),
                any());
    }

    @Test
    void joinOrganizationReturnsBadRequestWhenCodeIsIncorrect()
            throws Exception {
        when(membershipService.joinOrganization(
                "google-subject-123",
                "UGBC",
                "9999"))
                .thenThrow(new InvalidOrganizationJoinCodeException());

        mockMvc.perform(post("/api/organizations/join")
                        .with(oidcLogin().idToken(token ->
                                token.subject("google-subject-123")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationName": "UGBC",
                                  "joinCode": "9999"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_ORGANIZATION_JOIN_CODE"))
                .andExpect(jsonPath("$.message")
                        .value("Organization name or join code is incorrect"));
    }

    @Test
    void getMyOrganizationsUsesAuthenticatedGoogleSubject() throws Exception {
        Organization organization = new Organization("UGBC", "secret-hash");
        ReflectionTestUtils.setField(organization, "id", 1L);
        OrganizationMembership membership =
                new OrganizationMembership(null, organization);
        when(membershipService.getMemberships("google-subject-123"))
                .thenReturn(List.of(membership));

        mockMvc.perform(get("/api/organizations/mine")
                        .with(oidcLogin().idToken(token ->
                                token.subject("google-subject-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationId").value(1))
                .andExpect(jsonPath("$[0].organizationName").value("UGBC"))
                .andExpect(jsonPath("$[0].role").value("EBOARD"))
                .andExpect(jsonPath("$[0].joinCode").doesNotExist())
                .andExpect(jsonPath("$[0].joinCodeHash").doesNotExist());

        verify(membershipService).getMemberships("google-subject-123");
    }

    @Test
    void leaveOrganizationUsesAuthenticatedGoogleSubject() throws Exception {
        mockMvc.perform(delete("/api/organizations/UGBC/membership")
                        .with(oidcLogin().idToken(token ->
                                token.subject("google-subject-123")))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(membershipService).leaveOrganization(
                "google-subject-123",
                "UGBC");
    }
}
