package com.thecommons.backend.borrowing.dto;

import com.thecommons.backend.borrowing.BorrowingRequest;
import com.thecommons.backend.borrowing.BorrowingRequestStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BorrowingRequestResponse(
        Long id,
        Long inventoryItemId,
        String itemName,
        String qrCode,
        String lendingOrganization,
        String borrowingOrganization,
        Integer quantity,
        String purpose,
        LocalDate startDate,
        LocalDate dueDate,
        BorrowingRequestStatus status,
        String requestedByName,
        String reviewedByName,
        Instant createdAt,
        Instant reviewedAt,
        Instant cancelledAt,
        Instant returnedAt) {

    public static BorrowingRequestResponse from(BorrowingRequest request) {
        return new BorrowingRequestResponse(
                request.getId(),
                request.getInventoryItem().getId(),
                request.getInventoryItem().getName(),
                request.getInventoryItem().getQrCode(),
                request.getLendingOrganization().getName(),
                request.getBorrowingOrganization().getName(),
                request.getQuantity(),
                request.getPurpose(),
                request.getStartDate(),
                request.getDueDate(),
                request.getStatus(),
                request.getRequestedBy().getName(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getName(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getCancelledAt(),
                request.getReturnedAt());
    }
}
