package com.thecommons.backend.auth;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("Authenticated user does not exist in the application database");
    }
}
