package com.thecommons.backend.borrowing;

public class InvalidBorrowingRequestException extends RuntimeException {

    public InvalidBorrowingRequestException(String message) {
        super(message);
    }
}
