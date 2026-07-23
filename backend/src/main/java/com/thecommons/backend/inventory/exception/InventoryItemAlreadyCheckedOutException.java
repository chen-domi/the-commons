package com.thecommons.backend.inventory.exception;

public class InventoryItemAlreadyCheckedOutException extends RuntimeException {
    
    public InventoryItemAlreadyCheckedOutException(Long id) {
        super("Inventory item with ID " + id + " is already checked out");
    }
}
