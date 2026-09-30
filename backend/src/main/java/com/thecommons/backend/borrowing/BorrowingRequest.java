package com.thecommons.backend.borrowing;

import com.thecommons.backend.auth.AppUser;
import com.thecommons.backend.inventory.InventoryItem;
import com.thecommons.backend.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "borrowing_requests")
public class BorrowingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lending_organization_id", nullable = false)
    private Organization lendingOrganization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrowing_organization_id", nullable = false)
    private Organization borrowingOrganization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private AppUser requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private AppUser reviewedBy;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BorrowingRequestStatus status = BorrowingRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    protected BorrowingRequest() {
    }

    public BorrowingRequest(
            InventoryItem inventoryItem,
            Organization lendingOrganization,
            Organization borrowingOrganization,
            AppUser requestedBy,
            Integer quantity,
            String purpose,
            LocalDate startDate,
            LocalDate dueDate) {
        this.inventoryItem = inventoryItem;
        this.lendingOrganization = lendingOrganization;
        this.borrowingOrganization = borrowingOrganization;
        this.requestedBy = requestedBy;
        this.quantity = quantity;
        this.purpose = purpose;
        this.startDate = startDate;
        this.dueDate = dueDate;
    }

    public Long getId() { return id; }
    public InventoryItem getInventoryItem() { return inventoryItem; }
    public Organization getLendingOrganization() { return lendingOrganization; }
    public Organization getBorrowingOrganization() { return borrowingOrganization; }
    public AppUser getRequestedBy() { return requestedBy; }
    public AppUser getReviewedBy() { return reviewedBy; }
    public Integer getQuantity() { return quantity; }
    public String getPurpose() { return purpose; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BorrowingRequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getReturnedAt() { return returnedAt; }

    public void approve(AppUser reviewer) {
        requireStatus(BorrowingRequestStatus.PENDING);
        status = BorrowingRequestStatus.APPROVED;
        reviewedBy = reviewer;
        reviewedAt = Instant.now();
    }

    public void deny(AppUser reviewer) {
        requireStatus(BorrowingRequestStatus.PENDING);
        status = BorrowingRequestStatus.DENIED;
        reviewedBy = reviewer;
        reviewedAt = Instant.now();
    }

    public void cancel() {
        requireStatus(BorrowingRequestStatus.PENDING);
        status = BorrowingRequestStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public void markReturned(AppUser reviewer) {
        requireStatus(BorrowingRequestStatus.APPROVED);
        status = BorrowingRequestStatus.RETURNED;
        reviewedBy = reviewer;
        returnedAt = Instant.now();
    }

    private void requireStatus(BorrowingRequestStatus requiredStatus) {
        if (status != requiredStatus) {
            throw new IllegalStateException(
                    "Borrowing request must be " + requiredStatus + " but is " + status);
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
