package com.thecommons.backend.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import com.thecommons.backend.inventory.exception.InventoryItemNotFoundException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecommons.backend.inventory.dto.CheckOutInventoryItemRequest;
import com.thecommons.backend.inventory.dto.CreateInventoryItemRequest;
import com.thecommons.backend.inventory.dto.UpdateInventoryItemRequest;
import com.thecommons.backend.inventory.exception.DuplicateQrCodeException;
import com.thecommons.backend.inventory.exception.InventoryItemAlreadyCheckedOutException;
import com.thecommons.backend.inventory.exception.InventoryItemNotCheckedOutException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = InventoryController.class,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration,"
                + "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration")
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void getAllItemsReturnsOkAndItems() throws Exception {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        when(inventoryService.getAllItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].qrCode").value("TEST-QR-001"))
                .andExpect(jsonPath("$[0].name").value("Test Table"));

        verify(inventoryService).getAllItems();
    }

    @Test
    void getItemByIdReturnsOkAndItem() throws Exception {
        InventoryItem item = new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);

        when(inventoryService.getItemById(1L)).thenReturn(item);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").value("TEST-QR-001"))
                .andExpect(jsonPath("$.name").value("Test Table"));

        verify(inventoryService).getItemById(1L);
    }

    @Test
    void getItemByIdReturnsNotFoundWhenItemIsMissing() throws Exception {
        when(inventoryService.getItemById(99L))
                .thenThrow(new InventoryItemNotFoundException(99L));

        mockMvc.perform(get("/api/inventory/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("INVENTORY_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Inventory item with ID 99 was not found"));

        verify(inventoryService).getItemById(99L);
    }

    @Test
    void createItemReturnsCreatedAndItem() throws Exception {
        InventoryItem item = testItem();
        when(inventoryService.createItem(
                eq("google-subject-123"),
                any(CreateInventoryItemRequest.class)))
                .thenReturn(item);

        mockMvc.perform(post("/api/inventory")
                        .principal(() -> "google-subject-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCode").value("TEST-QR-001"))
                .andExpect(jsonPath("$.name").value("Test Table"));

        verify(inventoryService).createItem(
                eq("google-subject-123"),
                any(CreateInventoryItemRequest.class));
    }

    @Test
    void createItemReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .principal(() -> "google-subject-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qrCode": "",
                                  "name": "",
                                  "category": "Furniture",
                                  "organization": "UGBC",
                                  "location": "Test Storage",
                                  "quantity": -1,
                                  "shared": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(inventoryService, never())
                .createItem(any(), any(CreateInventoryItemRequest.class));
    }

    @Test
    void createItemReturnsConflictWhenQrCodeIsDuplicate() throws Exception {
        when(inventoryService.createItem(
                eq("google-subject-123"),
                any(CreateInventoryItemRequest.class)))
                .thenThrow(new DuplicateQrCodeException("TEST-QR-001"));

        mockMvc.perform(post("/api/inventory")
                        .principal(() -> "google-subject-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_QR_CODE"));

        verify(inventoryService).createItem(
                eq("google-subject-123"),
                any(CreateInventoryItemRequest.class));
    }

    @Test
    void updateItemReturnsOkAndUpdatedItem() throws Exception {
        InventoryItem item = testItem();
        item.setName("Updated Table");
        item.setQuantity(5);
        when(inventoryService.updateItem(
                eq("google-subject-123"),
                eq(1L),
                any(UpdateInventoryItemRequest.class)))
                .thenReturn(item);

        mockMvc.perform(put("/api/inventory/1")
                        .principal(() -> "google-subject-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Table",
                                  "category": "Furniture",
                                  "organization": "UGBC",
                                  "location": "Test Storage",
                                  "quantity": 5,
                                  "lastUsed": "Test event",
                                  "shared": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Table"))
                .andExpect(jsonPath("$.quantity").value(5));

        verify(inventoryService).updateItem(
                eq("google-subject-123"),
                eq(1L),
                any(UpdateInventoryItemRequest.class));
    }

    @Test
    void deleteItemReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/inventory/1"))
                .andExpect(status().isNoContent());

        verify(inventoryService).deleteItem(1L);
    }

    @Test
    void checkoutItemReturnsOkAndCheckedOutItem() throws Exception {
        InventoryItem item = testItem();
        item.setCheckedOut(true);
        item.setCheckoutPurpose("Test event");
        when(inventoryService.checkoutItem(
                eq(1L),
                any(CheckOutInventoryItemRequest.class)))
                .thenReturn(item);

        mockMvc.perform(post("/api/inventory/1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "Test event",
                                  "dueDate": "2099-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedOut").value(true))
                .andExpect(jsonPath("$.checkoutPurpose").value("Test event"));

        verify(inventoryService).checkoutItem(
                eq(1L),
                any(CheckOutInventoryItemRequest.class));
    }

    @Test
    void checkoutItemReturnsConflictWhenAlreadyCheckedOut() throws Exception {
        when(inventoryService.checkoutItem(
                eq(1L),
                any(CheckOutInventoryItemRequest.class)))
                .thenThrow(new InventoryItemAlreadyCheckedOutException(1L));

        mockMvc.perform(post("/api/inventory/1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "Test event",
                                  "dueDate": "2099-01-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code")
                        .value("INVENTORY_ITEM_ALREADY_CHECKED_OUT"));

        verify(inventoryService).checkoutItem(
                eq(1L),
                any(CheckOutInventoryItemRequest.class));
    }

    @Test
    void checkinItemReturnsOkAndAvailableItem() throws Exception {
        InventoryItem item = testItem();
        when(inventoryService.checkinItem(1L)).thenReturn(item);

        mockMvc.perform(post("/api/inventory/1/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedOut").value(false));

        verify(inventoryService).checkinItem(1L);
    }

    @Test
    void checkinItemReturnsConflictWhenItemIsNotCheckedOut() throws Exception {
        when(inventoryService.checkinItem(1L))
                .thenThrow(new InventoryItemNotCheckedOutException(1L));

        mockMvc.perform(post("/api/inventory/1/checkin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code")
                        .value("INVENTORY_ITEM_NOT_CHECKED_OUT"));

        verify(inventoryService).checkinItem(1L);
    }

    private InventoryItem testItem() {
        return new InventoryItem(
                "TEST-QR-001",
                "Test Table",
                "Furniture",
                "UGBC",
                "Test Storage",
                1);
    }

    private String validCreateRequestJson() {
        return """
                {
                  "qrCode": "TEST-QR-001",
                  "name": "Test Table",
                  "category": "Furniture",
                  "organization": "UGBC",
                  "location": "Test Storage",
                  "quantity": 1,
                  "lastUsed": "Test event",
                  "shared": true
                }
                """;
    }
}
