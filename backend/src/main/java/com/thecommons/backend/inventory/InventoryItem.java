package com.thecommons.backend.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")

public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String qrCode;
    
    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String organization;

    @Column(nullable = false)
    private String location;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    private String lastUsed;
    private boolean shared = false;
    private boolean checkedOut = false;

    private Integer borrowCount = 0;
    private String checkoutPurpose;
    private LocalDate checkoutDueDate;

    private Instant createdAt;
    private Instant updatedAt;

    protected InventoryItem() {

    }

    public InventoryItem(
            String qrCode,
            String name,
            String category,
            String organization,
            String location, 
            Integer quantity
    ) {
        this.qrCode = qrCode;
        this.name = name;
        this.category = category;
        this.organization = organization;
        this.location = location;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(String lastUsed) {
        this.lastUsed = lastUsed;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public Integer getBorrowCount() {
        return borrowCount;
    }

    public void setBorrowCount(Integer borrowCount) {
        this.borrowCount = borrowCount;
    }

    public String getCheckoutPurpose() {
        return checkoutPurpose;
    }

    public void setCheckoutPurpose(String checkoutPurpose) {
        this.checkoutPurpose = checkoutPurpose;
    }

    public LocalDate getCheckoutDueDate() {
        return checkoutDueDate;
    }

    public void setCheckoutDueDate(LocalDate checkoutDueDate) {
        this.checkoutDueDate = checkoutDueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
