package com.se2030.vaccinationportal.vaccination.mapper;

import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
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
                certificate.getStatus()
        );
    }
}
