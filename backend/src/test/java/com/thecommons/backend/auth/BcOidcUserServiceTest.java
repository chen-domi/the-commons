package com.thecommons.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
    void loadUserReturnsVerifiedBcUser() {
        when(user.getClaimAsBoolean("email_verified")).thenReturn(true);
        when(user.getClaimAsString("hd")).thenReturn("bc.edu");

        OidcUser result = bcOidcUserService.loadUser(userRequest);

        assertSame(user, result);
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
}
