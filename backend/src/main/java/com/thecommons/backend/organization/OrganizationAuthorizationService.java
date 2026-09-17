package com.thecommons.backend.organization;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.auth.AppUserRepository;
import com.thecommons.backend.auth.AuthenticatedUserNotFoundException;
import com.thecommons.backend.auth.GlobalRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAuthorizationService {

    private final AppUserRepository appUserRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationAuthorizationService(
            AppUserRepository appUserRepository,
            OrganizationMembershipRepository membershipRepository) {
        this.appUserRepository = appUserRepository;
        this.membershipRepository = membershipRepository;
    }

    public void requireCanManage(
            String googleSubject,
            String organizationName) {
        AppUser user = appUserRepository
                .findByGoogleSubject(googleSubject)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        if (user.getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        boolean isMember = membershipRepository
                .existsByUserAndOrganization_NameIgnoreCase(
                        user,
                        organizationName.trim());

        if (!isMember) {
            throw new AccessDeniedException(
                    "You cannot manage inventory for this organization");
        }
    }

    public void requireApplicationAccess(String googleSubject) {
        AppUser user = appUserRepository
                .findByGoogleSubject(googleSubject)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        if (user.getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (!membershipRepository.existsByUser(user)) {
            throw new AccessDeniedException(
                    "You must belong to an organization to access inventory");
        }
    }
}
