package com.thecommons.backend.inventory.exception;

public class InventoryItemNotCheckedOutException extends RuntimeException {

    public InventoryItemNotCheckedOutException(Long id) {
        super("Inventory item with ID " + id + " is not checked out");
    }
}
