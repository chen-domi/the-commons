package com.thecommons.backend.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;
import com.thecommons.backend.inventory.dto.CheckOutInventoryItemRequest;
import com.thecommons.backend.inventory.dto.UpdateInventoryItemRequest;
import com.thecommons.backend.inventory.exception.DuplicateQrCodeException;
import com.thecommons.backend.inventory.exception.InventoryItemAlreadyCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotFoundException;
import com.thecommons.backend.organization.OrganizationAuthorizationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OrganizationAuthorizationService authorizationService;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void getAllItemsReturnsRepositoryItems() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        when(inventoryRepository.findAll()).thenReturn(List.of(item));

        List<InventoryItem> result = inventoryService.getAllItems();

        assertEquals(1, result.size());
        assertSame(item, result.getFirst());
        verify(inventoryRepository).findAll();
    }

    @Test
    void createItemSavesItemWhenQrCodeIsUnique() {

        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "TEST-QR-001",
                "Test table",
                "Furniture",
                "Test organization",
                "Test Location",
                1,
                "Test event",
                true);

        when(inventoryRepository.existsByQrCode("TEST-QR-001")).thenReturn(
                false);
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.createItem(
                "google-subject-123",
                request);

        assertEquals("TEST-QR-001", result.getQrCode());
        verify(authorizationService).requireCanManage(
                "google-subject-123",
                "Test organization");
        verify(inventoryRepository).existsByQrCode("TEST-QR-001");
        verify(inventoryRepository).save(any(InventoryItem.class));
    }

    @Test
    void createItemThrowsWhenQrCodeAlreadyExists() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "TEST-QR-001",
                "Test table",
                "Furniture",
                "Test organization",
                "Test location",
                1,
                "Test event",
                true);

        when(inventoryRepository.existsByQrCode("TEST-QR-001")).thenReturn(
                true);

        assertThrows(
                DuplicateQrCodeException.class,
                () -> inventoryService.createItem(
                        "google-subject-123",
                        request));
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
        verify(inventoryRepository).existsByQrCode("TEST-QR-001");
    }

    @Test
    void createItemDoesNotAccessRepositoryWhenUserIsUnauthorized() {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
                "TEST-QR-001",
                "Test table",
                "Furniture",
                "Another organization",
                "Test location",
                1,
                "Test event",
                true);

        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService)
                .requireCanManage(
                        "google-subject-123",
                        "Another organization");

        assertThrows(
                AccessDeniedException.class,
                () -> inventoryService.createItem(
                        "google-subject-123",
                        request));

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void getItemByIdReturnsItemWhenFound() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));

        InventoryItem result = inventoryService.getItemById(1L);

        assertSame(item, result);
        verify(inventoryRepository).findById(1L);
    }

    @Test
    void getItemByIdThrowsWhenItemIsMissing() {

        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InventoryItemNotFoundException.class, () -> inventoryService.getItemById(1L));
        verify(inventoryRepository).findById(1L);
    }

    @Test
    void updateItemUpdatesAndSavesExistingItem() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Old name",
                "Old category",
                "Old organization",
                "Old location",
                1);

        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest(
                "New name",
                "New category",
                "New organization",
                "New location",
                5,
                "New event",
                true);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.updateItem(
                "google-subject-123",
                1L,
                request);

        assertSame(item, result);
        assertEquals("New name", result.getName());
        assertEquals("New category", result.getCategory());
        assertEquals("New organization", result.getOrganization());
        assertEquals("New location", result.getLocation());
        assertEquals(5, result.getQuantity());
        assertEquals("New event", result.getLastUsed());
        assertEquals(true, result.isShared());

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).save(item);
        verify(authorizationService).requireCanManage(
                "google-subject-123",
                "Old organization");
        verify(authorizationService).requireCanManage(
                "google-subject-123",
                "New organization");
    }

    @Test
    void updateItemThrowsWhenItemIsMissing() {

        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest(
                "New name",
                "New category",
                "New organization",
                "New location",
                5,
                "New event",
                true);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                InventoryItemNotFoundException.class,
                () -> inventoryService.updateItem(
                        "google-subject-123",
                        1L,
                        request));
        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void updateItemDoesNotSaveWhenUserCannotManageCurrentOrganization() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Old name",
                "Furniture",
                "Another organization",
                "Old location",
                1);
        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest(
                "New name",
                "Furniture",
                "Users organization",
                "New location",
                1,
                null,
                true);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        doThrow(new AccessDeniedException("denied"))
                .when(authorizationService)
                .requireCanManage(
                        "google-subject-123",
                        "Another organization");

        assertThrows(
                AccessDeniedException.class,
                () -> inventoryService.updateItem(
                        "google-subject-123",
                        1L,
                        request));

        verify(inventoryRepository, never()).save(any(InventoryItem.class));
        verify(authorizationService, never()).requireCanManage(
                "google-subject-123",
                "Users organization");
    }

    @Test
    void deleteItemDeletesExistingItem() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));

        inventoryService.deleteItem(1L);

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).delete(item);
    }

    @Test
    void deleteItemThrowsWhenItemIsMissing() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                InventoryItemNotFoundException.class,
                () -> inventoryService.deleteItem(1L));

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository, never()).delete(any(InventoryItem.class));
    }

    @Test
    void checkoutItemUpdatesAndSavesAvailableItem() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);
        LocalDate dueDate = LocalDate.of(2099, 1, 1);
        CheckOutInventoryItemRequest request =
                new CheckOutInventoryItemRequest("Test event", dueDate);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.checkoutItem(1L, request);

        assertSame(item, result);
        assertTrue(result.isCheckedOut());
        assertEquals("Test event", result.getCheckoutPurpose());
        assertEquals(dueDate, result.getCheckoutDueDate());
        assertEquals(1, result.getBorrowCount());

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).save(item);
    }

    @Test
    void checkoutItemThrowsWhenItemIsAlreadyCheckedOut() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        item.setCheckedOut(true);

        CheckOutInventoryItemRequest request =
                new CheckOutInventoryItemRequest(
                        "Another event",
                        LocalDate.of(2099, 1, 1));

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(
                InventoryItemAlreadyCheckedOutException.class,
                () -> inventoryService.checkoutItem(1L, request));

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void checkinItemClearsCheckoutDetailsAndSavesItem() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);
        item.setCheckedOut(true);
        item.setCheckoutPurpose("Test event");
        item.setCheckoutDueDate(LocalDate.of(2099, 1, 1));

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.checkinItem(1L);

        assertSame(item, result);
        assertFalse(result.isCheckedOut());
        assertNull(result.getCheckoutPurpose());
        assertNull(result.getCheckoutDueDate());

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository).save(item);
    }

    @Test
    void checkinItemThrowsWhenItemIsNotCheckedOut() {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(
                InventoryItemNotCheckedOutException.class,
                () -> inventoryService.checkinItem(1L));

        verify(inventoryRepository).findById(1L);
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }
}
