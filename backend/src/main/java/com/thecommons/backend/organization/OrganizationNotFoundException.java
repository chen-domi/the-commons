package com.thecommons.backend.organization;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(String name) {
        super("Organization '" + name + "' was not found");
    }
}
