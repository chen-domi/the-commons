package com.thecommons.backend.borrowing;

public class InvalidBorrowingRequestStateException extends RuntimeException {

    public InvalidBorrowingRequestStateException(
            Long id,
            BorrowingRequestStatus expected,
            BorrowingRequestStatus actual) {
        super("Borrowing request " + id + " must be " + expected + " but is " + actual);
    }
}
