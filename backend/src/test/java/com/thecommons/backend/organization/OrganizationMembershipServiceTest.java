package com.thecommons.backend.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.auth.AppUserRepository;
import com.thecommons.backend.auth.AuthenticatedUserNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationMembershipServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    private OrganizationMembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new OrganizationMembershipService(
                appUserRepository,
                organizationService,
                membershipRepository);
    }

    @Test
    void joinOrganizationCreatesMembershipWhenCodeIsValid() {
        AppUser user = new AppUser(
                "google-subject-123",
                "student@bc.edu",
                "BC Student");
        Organization organization = new Organization("UGBC", "stored-hash");

        when(appUserRepository.findByGoogleSubject("google-subject-123"))
                .thenReturn(Optional.of(user));
        when(organizationService.verifyJoinCode("UGBC", "1234"))
                .thenReturn(organization);
        when(membershipRepository.findByUserAndOrganization(user, organization))
                .thenReturn(Optional.empty());
        when(membershipRepository.save(any(OrganizationMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationMembership result = membershipService.joinOrganization(
                "google-subject-123",
                "UGBC",
                "1234");

        assertSame(user, result.getUser());
        assertSame(organization, result.getOrganization());
        assertEquals(MembershipRole.EBOARD, result.getRole());
        verify(membershipRepository).save(result);
    }

    @Test
    void joinOrganizationReturnsExistingMembership() {
        AppUser user = new AppUser(
                "google-subject-123",
                "student@bc.edu",
                "BC Student");
        Organization organization = new Organization("UGBC", "stored-hash");
        OrganizationMembership existingMembership =
                new OrganizationMembership(user, organization);

        when(appUserRepository.findByGoogleSubject("google-subject-123"))
                .thenReturn(Optional.of(user));
        when(organizationService.verifyJoinCode("UGBC", "1234"))
                .thenReturn(organization);
        when(membershipRepository.findByUserAndOrganization(user, organization))
                .thenReturn(Optional.of(existingMembership));

        OrganizationMembership result = membershipService.joinOrganization(
                "google-subject-123",
                "UGBC",
                "1234");

        assertSame(existingMembership, result);
        verify(membershipRepository, never())
                .save(any(OrganizationMembership.class));
    }

    @Test
    void joinOrganizationRejectsMissingApplicationUser() {
        when(appUserRepository.findByGoogleSubject("missing-subject"))
                .thenReturn(Optional.empty());

        assertThrows(
                AuthenticatedUserNotFoundException.class,
                () -> membershipService.joinOrganization(
                        "missing-subject",
                        "UGBC",
                        "1234"));

        verifyNoInteractions(organizationService, membershipRepository);
    }

    @Test
    void joinOrganizationRejectsInvalidCodeWithoutSaving() {
        AppUser user = new AppUser(
                "google-subject-123",
                "student@bc.edu",
                "BC Student");

        when(appUserRepository.findByGoogleSubject("google-subject-123"))
                .thenReturn(Optional.of(user));
        when(organizationService.verifyJoinCode("UGBC", "wrong"))
                .thenThrow(new InvalidOrganizationJoinCodeException());

        assertThrows(
                InvalidOrganizationJoinCodeException.class,
                () -> membershipService.joinOrganization(
                        "google-subject-123",
                        "UGBC",
                        "wrong"));

        verifyNoInteractions(membershipRepository);
    }
}
