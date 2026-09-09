package com.se2030.vaccinationportal.vaccination.dto;

import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import java.time.Instant;

/**
 * Safe public representation of an issued digital vaccination certificate.
 */
public record DigitalCertificateResponse(
        Long id,
        Long vaccinationRecordId,
        String certificateReference,
        Instant issuedAt,
        CertificateStatus status,
        Instant revokedAt,
        Long revokedBy,
        String revocationReason,
        Long reissuedFromCertificateId,
        Long supersededByCertificateId
) {
}
