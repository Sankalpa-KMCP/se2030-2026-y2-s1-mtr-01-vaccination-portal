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
import java.time.LocalDate;

@Entity
@Table(name = "vaccine_batches", uniqueConstraints = @UniqueConstraint(
        name = "uk_vaccine_batch_identity", columnNames = {"manufacturer_id", "vaccine_type_id", "batch_number"}))
public class VaccineBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manufacturer_id", nullable = false)
    private Long manufacturerId;

    @Column(name = "vaccine_type_id", nullable = false)
    private Long vaccineTypeId;

    @Column(name = "batch_number", nullable = false, length = 128)
    private String batchNumber;

    @Column(name = "manufacture_date", nullable = false)
    private LocalDate manufactureDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_status", nullable = false, length = 32)
    private BatchStatus batchStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VaccineBatch() {}

    public VaccineBatch(Long manufacturerId, Long vaccineTypeId, String batchNumber,
                        LocalDate manufactureDate, LocalDate expiryDate, BatchStatus batchStatus) {
        this.manufacturerId = manufacturerId;
        this.vaccineTypeId = vaccineTypeId;
        this.batchNumber = batchNumber;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.batchStatus = batchStatus;
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

    public Long getId() { return id; }
    public Long getManufacturerId() { return manufacturerId; }
    public Long getVaccineTypeId() { return vaccineTypeId; }
    public String getBatchNumber() { return batchNumber; }
    public LocalDate getManufactureDate() { return manufactureDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public BatchStatus getBatchStatus() { return batchStatus; }
    public boolean isRecalled() { return batchStatus == BatchStatus.RECALLED; }
    public void setBatchStatus(BatchStatus batchStatus) { this.batchStatus = batchStatus; }
    public void update(String batchNumber, LocalDate manufactureDate, LocalDate expiryDate, BatchStatus status) {
        this.batchNumber = batchNumber;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.batchStatus = status;
    }
}
