package com.thecommons.backend.organization;

public class InvalidOrganizationJoinCodeException extends RuntimeException {

    public InvalidOrganizationJoinCodeException() {
        super("Organization name or join code is incorrect");
    }
}
