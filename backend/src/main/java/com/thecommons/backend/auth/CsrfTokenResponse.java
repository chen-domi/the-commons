package com.thecommons.backend.auth;

public record CsrfTokenResponse(String headerName, String token) {
}
