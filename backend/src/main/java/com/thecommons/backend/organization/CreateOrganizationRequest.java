package com.thecommons.backend.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateOrganizationRequest(
        @NotBlank String name,
        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "join code must be 4 digits")
        String joinCode) {
}
