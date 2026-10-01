package com.se2030.vaccinationportal.vaccination.dto;

import java.time.LocalDate;

/**
 * Minimal safe projection of an inventory vaccine batch available for a given appointment.
 */
public record AvailableVaccineBatchResponse(
        Long id,
        String batchNumber,
        String vaccineName,
        LocalDate expiryDate,
        int availableQuantity
) {
}
