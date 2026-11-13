package com.thecommons.backend.auth;

public record AuthenticatedUserResponse(
        String name,
        String email,
        String pictureUrl,
        GlobalRole globalRole,
        String previewOrganization) {
    public AuthenticatedUserResponse(String name, String email, String pictureUrl, GlobalRole globalRole) {
        this(name, email, pictureUrl, globalRole, null);
    }
}
