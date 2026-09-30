package com.thecommons.backend.borrowing;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(int requested, int available) {
        super("Requested " + requested + " items but only " + available + " are available");
    }
}
