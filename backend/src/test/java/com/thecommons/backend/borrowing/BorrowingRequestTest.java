package com.thecommons.backend.borrowing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.inventory.InventoryItem;
import com.thecommons.backend.organization.Organization;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BorrowingRequestTest {

    @Test
    void approveChangesPendingRequestToApproved() {
        BorrowingRequest request = newRequest();
        AppUser reviewer = new AppUser("reviewer", "reviewer@bc.edu", "Reviewer");

        request.approve(reviewer);

        assertEquals(BorrowingRequestStatus.APPROVED, request.getStatus());
        assertEquals(reviewer, request.getReviewedBy());
        assertNotNull(request.getReviewedAt());
    }

    @Test
    void denyChangesPendingRequestToDenied() {
        BorrowingRequest request = newRequest();

        request.deny(new AppUser("reviewer", "reviewer@bc.edu", "Reviewer"));

        assertEquals(BorrowingRequestStatus.DENIED, request.getStatus());
        assertNotNull(request.getReviewedAt());
    }

    @Test
    void cancelChangesPendingRequestToCancelled() {
        BorrowingRequest request = newRequest();

        request.cancel();

        assertEquals(BorrowingRequestStatus.CANCELLED, request.getStatus());
        assertNotNull(request.getCancelledAt());
    }

    @Test
    void returnedRequestMustFirstBeApproved() {
        BorrowingRequest request = newRequest();
        AppUser reviewer = new AppUser("reviewer", "reviewer@bc.edu", "Reviewer");

        assertThrows(IllegalStateException.class, () -> request.markReturned(reviewer));

        request.approve(reviewer);
        request.markReturned(reviewer);

        assertEquals(BorrowingRequestStatus.RETURNED, request.getStatus());
        assertNotNull(request.getReturnedAt());
    }

    private BorrowingRequest newRequest() {
        Organization lender = new Organization("Lender", "hash");
        Organization borrower = new Organization("Borrower", "hash");
        AppUser requester = new AppUser("requester", "requester@bc.edu", "Requester");
        InventoryItem item = new InventoryItem(
                "QR-1", "Table", "Furniture", "Lender", "Storage", 5);
        item.setOwningOrganization(lender);

        return new BorrowingRequest(
                item,
                lender,
                borrower,
                requester,
                2,
                "Student event",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3));
    }
}
