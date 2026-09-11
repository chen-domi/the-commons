package com.thecommons.backend.organization;

public record OrganizationResponse(Long id, String name) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName());
    }
}
