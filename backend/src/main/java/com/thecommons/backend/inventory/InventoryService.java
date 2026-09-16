package com.thecommons.backend.inventory;

import java.util.List;

import org.springframework.stereotype.Service;

import com.thecommons.backend.inventory.dto.CheckOutInventoryItemRequest;
import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;
import com.thecommons.backend.inventory.dto.UpdateInventoryItemRequest;
import com.thecommons.backend.inventory.exception.DuplicateQrCodeException;
import com.thecommons.backend.inventory.exception.InventoryItemAlreadyCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotFoundException;
import com.thecommons.backend.organization.OrganizationAuthorizationService;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OrganizationAuthorizationService authorizationService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            OrganizationAuthorizationService authorizationService) {
        this.inventoryRepository = inventoryRepository;
        this.authorizationService = authorizationService;
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public InventoryItem getItemById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryItemNotFoundException(id));
    }

    public InventoryItem createItem(
            String googleSubject,
            CreateInventoryItemRequest request) {
        authorizationService.requireCanManage(
                googleSubject,
                request.organization());

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

    public void deleteItem(String googleSubject, Long id) {
        InventoryItem item = getItemById(id);
        authorizationService.requireCanManage(
                googleSubject,
                item.getOrganization());
        inventoryRepository.delete(item);
    }

    public InventoryItem updateItem(
            String googleSubject,
            Long id,
            UpdateInventoryItemRequest request) {
        InventoryItem item = getItemById(id);

        authorizationService.requireCanManage(
                googleSubject,
                item.getOrganization());
        authorizationService.requireCanManage(
                googleSubject,
                request.organization());

        item.setName(request.name());
        item.setCategory(request.category());
        item.setOrganization(request.organization());
        item.setLocation(request.location());
        item.setQuantity(request.quantity());
        item.setLastUsed(request.lastUsed());
        item.setShared(request.shared());

        return inventoryRepository.save(item);
    }

    public InventoryItem checkoutItem(Long id, CheckOutInventoryItemRequest request) {
        InventoryItem item = getItemById(id);

        if (item.isCheckedOut()) {
            throw new InventoryItemAlreadyCheckedOutException(id);
        }

        item.setCheckedOut(true);
        item.setCheckoutPurpose(request.purpose());
        item.setCheckoutDueDate(request.dueDate());

        int currentBorrowCount = item.getBorrowCount() == null ? 0 : item.getBorrowCount();

        item.setBorrowCount(currentBorrowCount + 1 );

        return inventoryRepository.save(item);
    }

    public InventoryItem checkinItem(Long id ) {
        InventoryItem item = getItemById(id);

        if (!item.isCheckedOut()) {
            throw new InventoryItemNotCheckedOutException(id);
        }

        item.setCheckedOut(false);
        item.setCheckoutPurpose(null);
        item.setCheckoutDueDate(null);

        return inventoryRepository.save(item);
    }
}
