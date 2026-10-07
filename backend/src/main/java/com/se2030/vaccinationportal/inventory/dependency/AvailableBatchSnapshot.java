package com.se2030.vaccinationportal.inventory.dependency;

import java.time.LocalDate;

/** Available center stock for appointment batch selection. */
public record AvailableBatchSnapshot(Long batchId, String batchNumber, Long vaccineTypeId,
                                     LocalDate expiryDate, int availableQuantity) {
}
