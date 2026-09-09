package com.se2030.vaccinationportal.vaccination.mapper;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;

/**
 * Pure transformations from the certificate entity to its safe API representation.
 */
public final class DigitalCertificateMapper {

    private DigitalCertificateMapper() {
    }

    public static DigitalCertificateResponse toResponse(DigitalCertificate certificate) {
        if (certificate == null) {
            return null;
        }
        return new DigitalCertificateResponse(
                certificate.getId(),
                certificate.getVaccinationRecordId(),
                certificate.getCertificateReference(),
                certificate.getIssuedAt(),
                certificate.getStatus(),
                certificate.getRevokedAt(),
                certificate.getRevokedBy(),
                certificate.getRevocationReason(),
                certificate.getReissuedFromCertificateId(),
                certificate.getSupersededByCertificateId()
        );
    }

    public static CertificateVerificationResponse toVerificationResponse(DigitalCertificate certificate) {
        if (certificate == null) {
            return null;
        }
        return new CertificateVerificationResponse(
                certificate.getCertificateReference(),
                certificate.getVaccinationRecordId(),
                certificate.getIssuedAt(),
                certificate.getStatus(),
                certificate.getStatus() == CertificateStatus.VALID
        );
    }
}
