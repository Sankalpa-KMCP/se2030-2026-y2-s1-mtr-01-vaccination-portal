package com.se2030.vaccinationportal.appointment.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "module_event_consumption", uniqueConstraints =
        @UniqueConstraint(name = "uk_module_event_consumer_source", columnNames = {"consumer_code", "source_event_id"}))
public class ModuleEventConsumption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "consumer_code", nullable = false, length = 64, updatable = false) private String consumerCode;
    @Column(name = "source_event_id", nullable = false, updatable = false) private Long sourceEventId;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 24) private ModuleEventConsumptionStatus status;
    @Column(name = "attempts", nullable = false) private int attempts;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "error_message", length = 500) private String errorMessage;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ModuleEventConsumption() {}
    public ModuleEventConsumption(String consumerCode, Long sourceEventId) {
        if (consumerCode == null || consumerCode.isBlank()) throw new IllegalArgumentException("Consumer code is required");
        if (sourceEventId == null || sourceEventId <= 0) throw new IllegalArgumentException("Source event ID is required");
        this.consumerCode = consumerCode.trim();
        this.sourceEventId = sourceEventId;
        this.status = ModuleEventConsumptionStatus.PROCESSING;
        this.attempts = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    public void retry() { status = ModuleEventConsumptionStatus.PROCESSING; attempts++; errorMessage = null; updatedAt = Instant.now(); }
    public void complete() { status = ModuleEventConsumptionStatus.COMPLETED; completedAt = Instant.now(); errorMessage = null; updatedAt = completedAt; }
    public void fail(String error) { status = ModuleEventConsumptionStatus.FAILED; errorMessage = truncate(error); updatedAt = Instant.now(); }
    private static String truncate(String value) { return value == null ? null : value.substring(0, Math.min(500, value.length())); }
    public Long getId() { return id; }
    public String getConsumerCode() { return consumerCode; }
    public Long getSourceEventId() { return sourceEventId; }
    public ModuleEventConsumptionStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
