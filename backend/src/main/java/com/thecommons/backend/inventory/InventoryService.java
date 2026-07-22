package com.thecommons.backend.inventory;

import java.util.List;
import org.springframework.stereotype.Service;
import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;
import com.thecommons.backend.inventory.dto.UpdateInventoryItemRequest;
import com.thecommons.backend.inventory.exception.DuplicateQrCodeException;
import com.thecommons.backend.inventory.exception.InventoryItemNotFoundException;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public InventoryItem getItemById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryItemNotFoundException(id));
    }

    public InventoryItem createItem(CreateInventoryItemRequest request) {
        if (inventoryRepository.existsByQrCode(request.qrCode())) {
            throw new DuplicateQrCodeException(request.qrCode());
        }

        InventoryItem item = new InventoryItem(
                request.qrCode(),
                request.name(),
                request.category(),
                request.organization(),
                request.location(),
                request.quantity());

        item.setLastUsed(request.lastUsed());
        item.setShared(request.shared());

        return inventoryRepository.save(item);
    }

    public void deleteItem(Long id) {
        InventoryItem item = getItemById(id);
        inventoryRepository.delete(item);
    }

    public InventoryItem updateItem(Long id, UpdateInventoryItemRequest request) {
        InventoryItem item = getItemById(id);

        item.setName(request.name());
        item.setCategory(request.category());
        item.setOrganization(request.organization());
        item.setLocation(request.location());
        item.setQuantity(request.quantity());
        item.setLastUsed(request.lastUsed());
        item.setShared(request.shared());

        return inventoryRepository.save(item);
    }
}
