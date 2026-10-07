package com.se2030.vaccinationportal.vaccination.certificate.mapper;

import com.se2030.vaccinationportal.vaccination.certificate.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.certificate.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;

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
                certificate.getIssuedDoseNumber(),
                certificate.getIssuedAdministrationTime(),
                certificate.getRevokedAt(),
                certificate.getRevokedBy(),
                certificate.getRevocationReason(),
                certificate.getReissuedFromCertificateId(),
                certificate.getSupersededByCertificateId(),
                certificate.getSupersededAt(),
                certificate.getSupersededBy(),
                certificate.getSupersessionReason()
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
                certificate.getIssuedDoseNumber(),
                certificate.getIssuedAdministrationTime(),
                certificate.getStatus() == CertificateStatus.VALID
        );
    }
}
