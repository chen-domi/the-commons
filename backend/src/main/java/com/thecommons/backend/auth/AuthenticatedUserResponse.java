package com.thecommons.backend.auth;

public record AuthenticatedUserResponse(
        String name,
        String email,
        String pictureUrl,
        GlobalRole globalRole) {
}
