package com.thecommons.backend.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryItemRequest(
    @NotBlank String qrCode,
    @NotBlank String name,
    @NotBlank String category,
    @NotBlank String organization,
    @NotBlank String location,
    @NotNull @Min(0) Integer quantity,
    String lastUsed,
    boolean shared
) {
}
