package com.se2030.vaccinationportal.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "stock_transactions", uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_transaction_vaccination_record", columnNames = "vaccination_record_id"))
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false, updatable = false)
    private Long inventoryId;

    @Column(name = "stock_transfer_id", updatable = false)
    private Long stockTransferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32, updatable = false)
    private StockTransactionType type;

    @Column(name = "delta", nullable = false, updatable = false)
    private int delta;

    @Column(name = "before_qty", nullable = false, updatable = false)
    private int beforeQty;

    @Column(name = "after_qty", nullable = false, updatable = false)
    private int afterQty;

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "reason", length = 500, updatable = false)
    private String reason;

    @Column(name = "vaccination_record_id", unique = true, updatable = false)
    private Long vaccinationRecordId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockTransaction() {}

    public static StockTransaction vaccinationDeduction(Long inventoryId, Long vaccinationRecordId, int beforeQty) {
        StockTransaction transaction = new StockTransaction();
        transaction.inventoryId = inventoryId;
        transaction.vaccinationRecordId = vaccinationRecordId;
        transaction.type = StockTransactionType.VACCINATION_DEDUCTION;
        transaction.delta = -1;
        transaction.beforeQty = beforeQty;
        transaction.afterQty = beforeQty - 1;
        transaction.createdAt = Instant.now();
        return transaction;
    }

    public static StockTransaction stockChange(Long inventoryId, StockTransactionType type, int delta,
            int beforeQty, Long actorId, String reason) {
        StockTransaction transaction = new StockTransaction();
        transaction.inventoryId = inventoryId;
        transaction.type = type;
        transaction.delta = delta;
        transaction.beforeQty = beforeQty;
        transaction.afterQty = beforeQty + delta;
        transaction.actorId = actorId;
        transaction.reason = reason;
        transaction.createdAt = Instant.now();
        return transaction;
    }

    public static StockTransaction transferChange(Long inventoryId, Long transferId,
            StockTransactionType type, int delta, int beforeQty, Long actorId, String reason) {
        StockTransaction transaction = stockChange(inventoryId, type, delta, beforeQty, actorId, reason);
        transaction.stockTransferId = transferId;
        return transaction;
    }

    public Long getId() { return id; }
    public Long getInventoryId() { return inventoryId; }
    public Long getStockTransferId() { return stockTransferId; }
    public Long getVaccinationRecordId() { return vaccinationRecordId; }
    public StockTransactionType getType() { return type; }
    public int getDelta() { return delta; }
    public int getBeforeQty() { return beforeQty; }
    public int getAfterQty() { return afterQty; }
    public Long getActorId() { return actorId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
