package com.thecommons.backend.borrowing;

public class BorrowingRequestNotFoundException extends RuntimeException {

    public BorrowingRequestNotFoundException(Long id) {
        super("Borrowing request with ID " + id + " was not found");
    }
}
