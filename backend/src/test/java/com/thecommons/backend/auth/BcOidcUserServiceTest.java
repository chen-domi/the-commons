package com.thecommons.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class BcOidcUserServiceTest {

    @Mock
    private OAuth2UserService<OidcUserRequest, OidcUser>
            googleOidcUserService;

    @Mock
    private OidcUserRequest userRequest;

    @Mock
    private OidcUser user;

    @Mock
    private AppUserService appUserService;

    private BcOidcUserService bcOidcUserService;

    @BeforeEach
    void setUp() {
        bcOidcUserService = new BcOidcUserService(
                googleOidcUserService,
                appUserService);
        when(googleOidcUserService.loadUser(userRequest)).thenReturn(user);
    }

    @Test
    void loadUserAddsUserRoleForVerifiedBcUser() {
        AppUser appUser = new AppUser(
                "google-subject-123",
                "student@bc.edu",
                "BC Student");
        arrangeValidBcUser(appUser);

        OidcUser result = bcOidcUserService.loadUser(userRequest);

        assertTrue(result.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_USER")));
        verify(appUserService).synchronizeUser(user);
    }

    @Test
    void loadUserAddsAdminRoleForDatabaseAdmin() {
        AppUser appUser = new AppUser(
                "google-subject-123",
                "admin@bc.edu",
                "BC Admin");
        appUser.changeGlobalRole(GlobalRole.ADMIN);
        arrangeValidBcUser(appUser);

        OidcUser result = bcOidcUserService.loadUser(userRequest);

        assertTrue(result.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(appUserService).synchronizeUser(user);
    }

    @Test
    void loadUserRejectsUnverifiedEmail() {
        when(user.getClaimAsBoolean("email_verified")).thenReturn(false);
        when(user.getClaimAsString("hd")).thenReturn("bc.edu");

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> bcOidcUserService.loadUser(userRequest));

        assertEquals("invalid_bc_account",
                exception.getError().getErrorCode());
        verifyNoInteractions(appUserService);
    }

    @Test
    void loadUserRejectsNonBcDomain() {
        when(user.getClaimAsBoolean("email_verified")).thenReturn(true);
        when(user.getClaimAsString("hd")).thenReturn("gmail.com");

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> bcOidcUserService.loadUser(userRequest));

        assertEquals("invalid_bc_account",
                exception.getError().getErrorCode());
        verifyNoInteractions(appUserService);
    }

    private void arrangeValidBcUser(AppUser appUser) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject("google-subject-123")
                .build();

        when(user.getClaimAsBoolean("email_verified")).thenReturn(true);
        when(user.getClaimAsString("hd")).thenReturn("bc.edu");
        doReturn(Set.of(new SimpleGrantedAuthority("OIDC_USER")))
                .when(user).getAuthorities();
        when(user.getIdToken()).thenReturn(idToken);
        when(user.getUserInfo()).thenReturn(null);
        when(appUserService.synchronizeUser(user)).thenReturn(appUser);
    }
}
