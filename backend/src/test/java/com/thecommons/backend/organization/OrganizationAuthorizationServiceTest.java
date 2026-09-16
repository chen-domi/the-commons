package com.thecommons.backend.organization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.auth.AppUserRepository;
import com.thecommons.backend.auth.AuthenticatedUserNotFoundException;
import com.thecommons.backend.auth.GlobalRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrganizationAuthorizationServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    private OrganizationAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new OrganizationAuthorizationService(
                appUserRepository,
                membershipRepository);
    }

    @Test
    void allowsGlobalAdminWithoutMembership() {
        AppUser admin = new AppUser(
                "admin-subject",
                "admin@bc.edu",
                "Admin User");
        admin.changeGlobalRole(GlobalRole.ADMIN);

        when(appUserRepository.findByGoogleSubject("admin-subject"))
                .thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> authorizationService.requireCanManage(
                "admin-subject",
                "UGBC"));

        verifyNoInteractions(membershipRepository);
    }

    @Test
    void allowsVerifiedOrganizationMember() {
        AppUser user = new AppUser(
                "member-subject",
                "member@bc.edu",
                "Member User");

        when(appUserRepository.findByGoogleSubject("member-subject"))
                .thenReturn(Optional.of(user));
        when(membershipRepository
                .existsByUserAndOrganization_NameIgnoreCase(user, "UGBC"))
                .thenReturn(true);

        assertDoesNotThrow(() -> authorizationService.requireCanManage(
                "member-subject",
                " UGBC "));
    }

    @Test
    void rejectsUserWithoutOrganizationMembership() {
        AppUser user = new AppUser(
                "member-subject",
                "member@bc.edu",
                "Member User");

        when(appUserRepository.findByGoogleSubject("member-subject"))
                .thenReturn(Optional.of(user));
        when(membershipRepository
                .existsByUserAndOrganization_NameIgnoreCase(user, "UGBC"))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.requireCanManage(
                        "member-subject",
                        "UGBC"));
    }

    @Test
    void rejectsMissingApplicationUser() {
        when(appUserRepository.findByGoogleSubject("missing-subject"))
                .thenReturn(Optional.empty());

        assertThrows(
                AuthenticatedUserNotFoundException.class,
                () -> authorizationService.requireCanManage(
                        "missing-subject",
                        "UGBC"));

        verify(membershipRepository, never())
                .existsByUserAndOrganization_NameIgnoreCase(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }
}
