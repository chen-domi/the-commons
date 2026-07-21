package com.thecommons.backend.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateQrCodeException extends RuntimeException {
    
    public DuplicateQrCodeException(String qrCode) {
        super("An inventory item with QR code " + qrCode + " already exists");
    }
}
