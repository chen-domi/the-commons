package com.thecommons.backend.inventory.exception;

public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(Long id) {
        super("Inventory item with ID " + id + " was not found");
    }
}
