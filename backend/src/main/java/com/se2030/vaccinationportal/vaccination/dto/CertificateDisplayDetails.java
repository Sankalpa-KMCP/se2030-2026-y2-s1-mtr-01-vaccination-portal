package com.se2030.vaccinationportal.vaccination.dto;

/**
 * Resolved meaningful human-readable values for digital certificate PDF rendering.
 */
public record CertificateDisplayDetails(
        String citizenName,
        String vaccineName,
        String batchNumber,
        String centerName
) {
}
