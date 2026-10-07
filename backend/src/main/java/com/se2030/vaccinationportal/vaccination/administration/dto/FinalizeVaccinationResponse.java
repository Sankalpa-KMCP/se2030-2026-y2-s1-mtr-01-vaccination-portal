package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.se2030.vaccinationportal.vaccination.certificate.dto.DigitalCertificateResponse;


import java.time.Instant;

/**
 * Result payload returned upon successful transactional vaccination finalization.
 */
public record FinalizeVaccinationResponse(
        Long draftId,
        Long vaccinationRecordId,
        Long appointmentId,
        Long citizenId,
        Long vaccineBatchId,
        Integer doseNumber,
        Instant administrationTime,
        String certificateReference,
        String verificationToken,
        DigitalCertificateResponse certificate
) {
}
