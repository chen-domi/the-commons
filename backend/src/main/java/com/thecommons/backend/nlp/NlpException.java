package com.thecommons.backend.nlp;

import org.springframework.http.HttpStatus;

public class NlpException extends RuntimeException {
    private final HttpStatus status;

    public NlpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }

    public static NlpException unavailable() {
        return new NlpException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI form filling is unavailable. You can still enter the item manually.");
    }

    public static NlpException invalidResponse() {
        return new NlpException(HttpStatus.BAD_GATEWAY,
                "AI could not produce valid item details. Try a clearer description or enter the item manually.");
    }
}
