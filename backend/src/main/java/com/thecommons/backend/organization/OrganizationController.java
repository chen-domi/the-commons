package com.thecommons.backend.organization;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationMembershipService membershipService;

    public OrganizationController(
            OrganizationService organizationService,
            OrganizationMembershipService membershipService) {
        this.organizationService = organizationService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<OrganizationResponse> getAllOrganizations() {
        return organizationService.getAllOrganizations().stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @PostMapping("/join")
    public OrganizationMembershipResponse joinOrganization(
            @AuthenticationPrincipal OidcUser oidcUser,
            @Valid @RequestBody JoinOrganizationRequest request) {
        OrganizationMembership membership = membershipService.joinOrganization(
                oidcUser.getSubject(),
                request.organizationName(),
                request.joinCode());

        return OrganizationMembershipResponse.from(membership);
    }

    @GetMapping("/mine")
    public List<OrganizationMembershipResponse> getMyOrganizations(
            @AuthenticationPrincipal OidcUser oidcUser) {
        return membershipService.getMemberships(oidcUser.getSubject()).stream()
                .map(OrganizationMembershipResponse::from)
                .toList();
    }
}
