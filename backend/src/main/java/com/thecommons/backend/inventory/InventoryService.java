package com.thecommons.backend.inventory;

import java.util.List;
import org.springframework.stereotype.Service;
import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public InventoryItem createItem(CreateInventoryItemRequest request) {
        if (inventoryRepository.existsByQrCode(request.qrCode())) {
            throw new IllegalArgumentException(
                "An inventory item with this QR code already exists"
            );
        }

        InventoryItem item = new InventoryItem(
                request.qrCode(),
                request.name(),
                request.category(),
                request.organization(),
                request.location(),
                request.quantity()
        );

        item.setLastUsed(request.lastUsed());
        item.setShared(request.shared());

        return inventoryRepository.save(item);
    }
}
