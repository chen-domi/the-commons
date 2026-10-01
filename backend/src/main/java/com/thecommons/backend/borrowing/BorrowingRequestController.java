package com.thecommons.backend.borrowing;

import com.thecommons.backend.borrowing.dto.BorrowingNotificationSummary;
import com.thecommons.backend.borrowing.dto.BorrowingRequestResponse;
import com.thecommons.backend.borrowing.dto.CreateBorrowingRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrowing-requests")
public class BorrowingRequestController {

    private final BorrowingRequestService borrowingRequestService;

    public BorrowingRequestController(
            BorrowingRequestService borrowingRequestService) {
        this.borrowingRequestService = borrowingRequestService;
    }

    @GetMapping
    public List<BorrowingRequestResponse> getRequests(Principal principal) {
        return borrowingRequestService.getVisibleRequests(principal.getName());
    }

    @GetMapping("/notifications")
    public BorrowingNotificationSummary getNotificationSummary(
            Principal principal) {
        return borrowingRequestService.getNotificationSummary(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowingRequestResponse createRequest(
            Principal principal,
            @Valid @RequestBody CreateBorrowingRequest request) {
        return borrowingRequestService.create(principal.getName(), request);
    }

    @PatchMapping("/{id}/approve")
    public BorrowingRequestResponse approve(
            Principal principal,
            @PathVariable Long id) {
        return borrowingRequestService.approve(principal.getName(), id);
    }

    @PatchMapping("/{id}/deny")
    public BorrowingRequestResponse deny(
            Principal principal,
            @PathVariable Long id) {
        return borrowingRequestService.deny(principal.getName(), id);
    }

    @PatchMapping("/{id}/cancel")
    public BorrowingRequestResponse cancel(
            Principal principal,
            @PathVariable Long id) {
        return borrowingRequestService.cancel(principal.getName(), id);
    }

    @PatchMapping("/{id}/return")
    public BorrowingRequestResponse markReturned(
            Principal principal,
            @PathVariable Long id) {
        return borrowingRequestService.markReturned(principal.getName(), id);
    }
}
