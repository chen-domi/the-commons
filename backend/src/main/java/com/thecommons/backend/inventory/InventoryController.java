package com.thecommons.backend.inventory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryItem> getAllItems() {
        return inventoryService.getAllItems();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItem createItem(
        @Valid @RequestBody CreateInventoryItemRequest request
    ) {
        return inventoryService.createItem(request);
    }
    
}
