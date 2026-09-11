package com.thecommons.backend.organization;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.auth.AppUserRepository;
import com.thecommons.backend.auth.AuthenticatedUserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMembershipService {

    private final AppUserRepository appUserRepository;
    private final OrganizationService organizationService;
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationMembershipService(
            AppUserRepository appUserRepository,
            OrganizationService organizationService,
            OrganizationMembershipRepository membershipRepository) {
        this.appUserRepository = appUserRepository;
        this.organizationService = organizationService;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public OrganizationMembership joinOrganization(
            String googleSubject,
            String organizationName,
            String joinCode) {
        AppUser user = appUserRepository
                .findByGoogleSubject(googleSubject)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        Organization organization = organizationService
                .verifyJoinCode(organizationName, joinCode);

        return membershipRepository
                .findByUserAndOrganization(user, organization)
                .orElseGet(() -> membershipRepository.save(
                        new OrganizationMembership(user, organization)));
    }
}
