package com.se2030.vaccinationportal.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "center_inventories", uniqueConstraints = @UniqueConstraint(
        name = "uk_center_inventory_center_batch", columnNames = {"center_id", "batch_id"}))
public class CenterInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CenterInventory() {}

    public CenterInventory(Long centerId, Long batchId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Inventory cannot be negative");
        this.centerId = centerId;
        this.batchId = batchId;
        this.quantity = quantity;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void deductOneDose() {
        if (quantity < 1) throw new IllegalStateException("Insufficient center stock");
        quantity--;
    }

    public void applyChange(int delta) {
        long next = (long) quantity + delta;
        if (next < 0) throw new IllegalStateException("Inventory cannot become negative");
        if (next > Integer.MAX_VALUE) throw new IllegalArgumentException("Inventory quantity is too large");
        quantity = (int) next;
    }

    public Long getId() { return id; }
    public Long getCenterId() { return centerId; }
    public Long getBatchId() { return batchId; }
    public int getQuantity() { return quantity; }
    public Long getLockVersion() { return lockVersion; }
    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Inventory cannot be negative");
        this.quantity = quantity;
    }
}
