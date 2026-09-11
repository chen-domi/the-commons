package com.thecommons.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OidcUser oidcUser;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(appUserRepository);
    }

    @Test
    void synchronizeUserCreatesUserOnFirstLogin() {
        when(oidcUser.getSubject()).thenReturn("google-subject-123");
        when(oidcUser.getEmail()).thenReturn("Student@bc.edu");
        when(oidcUser.getFullName()).thenReturn("BC Student");
        when(appUserRepository.findByGoogleSubject("google-subject-123"))
                .thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.synchronizeUser(oidcUser);

        assertEquals("google-subject-123", result.getGoogleSubject());
        assertEquals("student@bc.edu", result.getEmail());
        assertEquals("BC Student", result.getName());
        assertEquals(GlobalRole.USER, result.getGlobalRole());
        verify(appUserRepository).save(result);
    }

    @Test
    void synchronizeUserUpdatesReturningUser() {
        AppUser existingUser = new AppUser(
                "google-subject-123",
                "old@bc.edu",
                "Old Name");

        when(oidcUser.getSubject()).thenReturn("google-subject-123");
        when(oidcUser.getEmail()).thenReturn("Updated@bc.edu");
        when(oidcUser.getFullName()).thenReturn("Updated Name");
        when(appUserRepository.findByGoogleSubject("google-subject-123"))
                .thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(existingUser)).thenReturn(existingUser);

        AppUser result = appUserService.synchronizeUser(oidcUser);

        assertSame(existingUser, result);
        assertEquals("updated@bc.edu", result.getEmail());
        assertEquals("Updated Name", result.getName());
        verify(appUserRepository).save(existingUser);
    }
}
