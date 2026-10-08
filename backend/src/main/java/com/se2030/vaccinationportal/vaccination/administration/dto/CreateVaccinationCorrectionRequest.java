package com.se2030.vaccinationportal.vaccination.administration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for appending a correction or citizen correction request to finalized vaccination history. */
public record CreateVaccinationCorrectionRequest(
        @Size(max = 64, message = "Correction field must not exceed 64 characters")
        @JsonAlias({"field", "field_name"})
        String fieldName,

        @Size(max = 2000, message = "New value must not exceed 2000 characters")
        @JsonAlias({"value", "new_value", "proposedValue"})
        String newValue,

        @NotBlank(message = "Correction reason is required")
        @Size(max = 1000, message = "Correction reason must not exceed 1000 characters")
        @JsonAlias({"message", "issueDescription", "description"})
        String reason,

        Long correctedBy,

        @Size(max = 64, message = "Category must not exceed 64 characters")
        String category,

        String requestType
) {
    public CreateVaccinationCorrectionRequest(String fieldName, String newValue, String reason, Long correctedBy) {
        this(fieldName, newValue, reason, correctedBy, null, null);
    }

    public CreateVaccinationCorrectionRequest(String fieldName, String newValue, String reason) {
        this(fieldName, newValue, reason, null, null, null);
    }

    public CreateVaccinationCorrectionRequest(String reason) {
        this(null, null, reason, null, null, null);
    }
}
