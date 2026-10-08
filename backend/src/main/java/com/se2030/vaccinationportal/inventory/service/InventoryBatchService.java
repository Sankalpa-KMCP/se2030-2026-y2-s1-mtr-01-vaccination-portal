package com.se2030.vaccinationportal.inventory.service;

import com.se2030.vaccinationportal.inventory.dependency.BatchAvailability;
import com.se2030.vaccinationportal.inventory.dependency.AvailableBatchSnapshot;
import com.se2030.vaccinationportal.inventory.dependency.BatchDisplaySnapshot;
import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.inventory.exception.DuplicateVaccinationDeductionException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.StockTransaction;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryBatchService implements InventoryBatchPort {
    private final VaccineBatchRepository batches;
    private final CenterInventoryRepository inventories;
    private final StockTransactionRepository transactions;
    private final Clock clock;
    private final EntityManager entityManager;

    public InventoryBatchService(VaccineBatchRepository batches,
                                 CenterInventoryRepository inventories,
                                 StockTransactionRepository transactions, Clock clock) {
        this(batches, inventories, transactions, clock, null);
    }

    @Autowired
    public InventoryBatchService(VaccineBatchRepository batches,
                                 CenterInventoryRepository inventories,
                                 StockTransactionRepository transactions, Clock clock,
                                 EntityManager entityManager) {
        this.batches = batches;
        this.inventories = inventories;
        this.transactions = transactions;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchAvailability getBatchAvailability(Long centerId, Long batchId) {
        requireIds(centerId, batchId);
        VaccineBatch batch = batches.findById(batchId).orElse(null);
        if (batch == null) {
            return new BatchAvailability(batchId, null, null, null, 0,
                    BatchAvailabilityReason.BATCH_NOT_FOUND);
        }
        int quantity = inventories.findByCenterIdAndBatchId(centerId, batchId)
                .map(CenterInventory::getQuantity).orElse(0);
        return availability(batch, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableBatchSnapshot> findAvailableBatches(Long centerId, Long expectedVaccineTypeId,
                                                              LocalDate asOfDate) {
        return inventories.findByCenterId(centerId).stream()
                .filter(inventory -> inventory.getQuantity() > 0)
                .map(inventory -> batches.findById(inventory.getBatchId())
                        .filter(batch -> batch.getBatchStatus() == BatchStatus.ACTIVE)
                        .filter(batch -> batch.getExpiryDate() == null || !batch.getExpiryDate().isBefore(asOfDate))
                        .filter(batch -> expectedVaccineTypeId == null
                                || expectedVaccineTypeId.equals(batch.getVaccineTypeId()))
                        .map(batch -> new AvailableBatchSnapshot(batch.getId(), batch.getBatchNumber(),
                                batch.getVaccineTypeId(), batch.getExpiryDate(), inventory.getQuantity())))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchDisplaySnapshot> findBatchDisplay(Long batchId) {
        return batches.findById(batchId)
                .map(batch -> new BatchDisplaySnapshot(batch.getBatchNumber(), batch.getVaccineTypeId()));
    }

    @Override
    @Transactional
    public void deductVaccinationDose(Long centerId, Long batchId, Long vaccinationRecordId) {
        requireIds(centerId, batchId);
        if (vaccinationRecordId == null || vaccinationRecordId <= 0) {
            throw new IllegalArgumentException("Vaccination record ID must be positive");
        }
        if (transactions.existsByVaccinationRecordId(vaccinationRecordId)) {
            throw new DuplicateVaccinationDeductionException(vaccinationRecordId);
        }

        // Always lock the batch before the center-batch inventory row. This serializes
        // status changes and competing finalizations in a consistent lock order.
        if (entityManager != null) {
            VaccineBatch cached = entityManager.find(VaccineBatch.class, batchId);
            if (cached != null) {
                entityManager.detach(cached);
            }
        }
        VaccineBatch batch = batches.findByIdForUpdate(batchId)
                .orElseThrow(() -> new BatchUnavailableException(BatchAvailabilityReason.BATCH_NOT_FOUND));
        CenterInventory inventory = inventories.findByCenterAndBatchForUpdate(centerId, batchId)
                .orElse(null);
        int quantity = inventory == null ? 0 : inventory.getQuantity();
        BatchAvailability snapshot = availability(batch, quantity);
        if (!snapshot.isAvailable()) {
            throw new BatchUnavailableException(snapshot.reason());
        }
        // A second transaction may have completed while this one waited for the lock.
        if (transactions.existsByVaccinationRecordId(vaccinationRecordId)) {
            throw new DuplicateVaccinationDeductionException(vaccinationRecordId);
        }

        int before = inventory.getQuantity();
        inventory.deductOneDose();
        inventories.saveAndFlush(inventory);
        transactions.saveAndFlush(StockTransaction.vaccinationDeduction(
                inventory.getId(), vaccinationRecordId, before));
    }

    private BatchAvailability availability(VaccineBatch batch, int quantity) {
        BatchAvailabilityReason reason;
        if (batch.getBatchStatus() == BatchStatus.RECALLED) {
            reason = BatchAvailabilityReason.RECALLED_BATCH;
        } else if (batch.getBatchStatus() != BatchStatus.ACTIVE) {
            reason = BatchAvailabilityReason.INACTIVE_BATCH;
        } else if (batch.getExpiryDate().isBefore(LocalDate.now(clock))) {
            reason = BatchAvailabilityReason.EXPIRED_BATCH;
        } else if (quantity < 1) {
            reason = BatchAvailabilityReason.INSUFFICIENT_STOCK;
        } else {
            reason = BatchAvailabilityReason.AVAILABLE;
        }
        return new BatchAvailability(batch.getId(), batch.getVaccineTypeId(),
                batch.getBatchStatus(), batch.getExpiryDate(), quantity, reason);
    }

    private static void requireIds(Long centerId, Long batchId) {
        if (centerId == null || centerId <= 0 || batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("Center and batch IDs must be positive");
        }
    }
}
