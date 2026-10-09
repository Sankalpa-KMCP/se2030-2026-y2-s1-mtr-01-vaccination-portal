package com.se2030.vaccinationportal.inventory.dependency;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** MF-04 boundary for other modules. Draft work may call only the read method. */
public interface InventoryBatchPort {
    /** Includes batches in every status, including historical inventory. */
    boolean hasBatchesForVaccineType(Long vaccineTypeId);

    BatchAvailability getBatchAvailability(Long centerId, Long batchId);

    List<AvailableBatchSnapshot> findAvailableBatches(Long centerId, Long expectedVaccineTypeId, LocalDate asOfDate);

    Optional<BatchDisplaySnapshot> findBatchDisplay(Long batchId);

    /** Joins the caller's transaction when present, so finalization can roll back atomically. */
    void deductVaccinationDose(Long centerId, Long batchId, Long vaccinationRecordId);
}
