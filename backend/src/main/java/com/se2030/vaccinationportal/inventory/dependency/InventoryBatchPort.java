package com.se2030.vaccinationportal.inventory.dependency;

/** MF-04 boundary for other modules. Draft work may call only the read method. */
public interface InventoryBatchPort {
    BatchAvailability getBatchAvailability(Long centerId, Long batchId);

    /** Joins the caller's transaction when present, so finalization can roll back atomically. */
    void deductVaccinationDose(Long centerId, Long batchId, Long vaccinationRecordId);
}
