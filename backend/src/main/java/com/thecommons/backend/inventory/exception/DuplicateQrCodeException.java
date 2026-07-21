package com.thecommons.backend.inventory.exception;

public class DuplicateQrCodeException extends RuntimeException {

    public DuplicateQrCodeException(String qrCode) {
        super("An inventory item with QR code " + qrCode + " already exists");
    }
}
