package com.thecommons.backend.organization;

public record OrganizationMembershipResponse(
        Long organizationId,
        String organizationName,
        MembershipRole role) {

    public static OrganizationMembershipResponse from(
            OrganizationMembership membership) {
        return new OrganizationMembershipResponse(
                membership.getOrganization().getId(),
                membership.getOrganization().getName(),
                membership.getRole());
    }
}
