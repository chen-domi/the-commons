package com.thecommons.backend.borrowing.dto;

public record BorrowingNotificationSummary(
        long pendingIncomingRequests,
        long approvedOutgoingBorrows) {
}
