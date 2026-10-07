package com.se2030.vaccinationportal.vaccination.certificate.dto;

import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import java.time.Instant;

/**
 * Representation of a digital certificate for the citizen portal view.
 */
public record CitizenCertificateResponse(
        Long id,
        Long vaccinationRecordId,
        String certificateReference,
        Instant issuedAt,
        CertificateStatus status,
        Integer doseNumber,
        Instant administrationTime,
        Long vaccineBatchId,
        String verificationToken,
        Instant revokedAt,
        String revocationReason,
        Long reissuedFromCertificateId,
        Long supersededByCertificateId
) {
}
