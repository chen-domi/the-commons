package com.thecommons.backend.inventory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;
import com.thecommons.backend.inventory.dto.UpdateInventoryItemRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;

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

    @GetMapping("/{id}")
    public InventoryItem getItemById(@PathVariable Long id) {
        return inventoryService.getItemById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItem createItem(
            @Valid @RequestBody CreateInventoryItemRequest request) {
        return inventoryService.createItem(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        inventoryService.deleteItem(id);
    }

    @PutMapping("{id}")
    public InventoryItem updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        return inventoryService.updateItem(id, request);
    }
}
