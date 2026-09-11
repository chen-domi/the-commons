package com.thecommons.backend.organization;

public class OrganizationAlreadyExistsException extends RuntimeException {

    public OrganizationAlreadyExistsException(String name) {
        super("Organization already exists: " + name);
    }
}
