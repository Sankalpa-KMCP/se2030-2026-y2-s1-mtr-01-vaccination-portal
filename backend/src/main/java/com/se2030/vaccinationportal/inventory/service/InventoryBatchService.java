package com.se2030.vaccinationportal.inventory.service;

import com.se2030.vaccinationportal.inventory.dependency.BatchAvailability;
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
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryBatchService implements InventoryBatchPort {
    private final VaccineBatchRepository batches;
    private final CenterInventoryRepository inventories;
    private final StockTransactionRepository transactions;
    private final Clock clock;

    public InventoryBatchService(VaccineBatchRepository batches,
                                 CenterInventoryRepository inventories,
                                 StockTransactionRepository transactions, Clock clock) {
        this.batches = batches;
        this.inventories = inventories;
        this.transactions = transactions;
        this.clock = clock;
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
