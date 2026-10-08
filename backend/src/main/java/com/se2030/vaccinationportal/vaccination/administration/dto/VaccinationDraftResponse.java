package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * REST representation of a vaccination draft without JPA internals.
 */
public record VaccinationDraftResponse(
        Long id,
        Long appointmentId,
        Long vaccinatorId,
        Long vaccineBatchId,
        Integer doseNumber,
        Instant administrationTime,
        LocalDate nextDoseDate,
        String authorizedNotes,
        boolean physicalAdministrationConfirmed,
        VaccinationDraftStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
