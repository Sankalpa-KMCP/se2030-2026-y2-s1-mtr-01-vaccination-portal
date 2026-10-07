package com.se2030.vaccinationportal.vaccination.certificate.dto;

import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import java.time.Instant;

/**
 * Safe result returned when a digital vaccination certificate is verified.
 */
public record CertificateVerificationResponse(
        String certificateReference,
        Long vaccinationRecordId,
        Instant issuedAt,
        CertificateStatus status,
        Integer doseNumber,
        Instant administrationTime,
        boolean valid
) {
}
