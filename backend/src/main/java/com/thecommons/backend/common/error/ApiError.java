package com.thecommons.backend.common.error;

import java.time.Instant;

public record ApiError(
    Instant timestamp,
    int status,
    String code,
    String message
) {
}
