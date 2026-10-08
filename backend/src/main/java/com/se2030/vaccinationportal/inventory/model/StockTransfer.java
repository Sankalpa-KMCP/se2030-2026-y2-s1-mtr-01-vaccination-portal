package com.se2030.vaccinationportal.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stock_transfers")
public class StockTransfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;
    @Column(name = "batch_id", nullable = false)
    private Long batchId;
    @Column(name = "source_center_id", nullable = false)
    private Long sourceCenterId;
    @Column(name = "destination_center_id", nullable = false)
    private Long destinationCenterId;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockTransfer() {}

    public StockTransfer(String correlationId, Long batchId, Long sourceCenterId,
            Long destinationCenterId, int quantity, Long requestedBy, String reason) {
        this.correlationId = correlationId;
        this.batchId = batchId;
        this.sourceCenterId = sourceCenterId;
        this.destinationCenterId = destinationCenterId;
        this.quantity = quantity;
        this.status = "COMPLETED";
        this.requestedBy = requestedBy;
        this.completedAt = Instant.now();
        this.createdAt = this.completedAt;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public Long getBatchId() { return batchId; }
    public Long getSourceCenterId() { return sourceCenterId; }
    public Long getDestinationCenterId() { return destinationCenterId; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public Long getRequestedBy() { return requestedBy; }
    public String getReason() { return reason; }
    public Instant getCompletedAt() { return completedAt; }
}
