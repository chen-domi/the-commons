package com.thecommons.backend.common.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thecommons.backend.inventory.exception.DuplicateQrCodeException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateQrCodeException.class)
    public ResponseEntity<ApiError> handleDuplicateQrCode(
            DuplicateQrCodeException exception) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_QR_CODE",
                exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }
}
