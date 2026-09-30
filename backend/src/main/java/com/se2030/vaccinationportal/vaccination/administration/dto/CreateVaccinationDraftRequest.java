package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Request payload for creating an editable vaccination draft.
 */
public record CreateVaccinationDraftRequest(
        @NotNull(message = "Appointment ID is required")
        @Positive(message = "Appointment ID must be positive")
        Long appointmentId,

        @Positive(message = "Vaccine batch ID must be positive")
        Long vaccineBatchId,

        @Positive(message = "Dose number must be positive")
        Integer doseNumber,

        @PastOrPresent(message = "Administration time cannot be in the future")
        Instant administrationTime,

        @FutureOrPresent(message = "Next dose date cannot be in the past")
        LocalDate nextDoseDate,

        @Size(max = 2000, message = "Authorized notes must not exceed 2000 characters")
        String authorizedNotes
) {
    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Unsupported draft field: " + name);
    }
}
