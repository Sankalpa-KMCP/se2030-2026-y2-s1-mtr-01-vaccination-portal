package com.se2030.vaccinationportal.inventory.dependency;

import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import java.time.LocalDate;

/** Read-only MF-04 snapshot; callers must revalidate during finalization. */
public record BatchAvailability(
        Long batchId,
        Long vaccineTypeId,
        BatchStatus batchStatus,
        LocalDate expiryDate,
        int availableQuantity,
        BatchAvailabilityReason reason) {
    public boolean isAvailable() { return reason == BatchAvailabilityReason.AVAILABLE; }
    public boolean isRecalled() { return batchStatus == BatchStatus.RECALLED; }
}
