package com.se2030.vaccinationportal.screening.dto;

import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EligibilityDecisionRequest(
        @NotNull(message = "Decision status is required")
        EligibilityAssessmentStatus status,

        @NotBlank(message = "Decision reason is required")
        String reason,

        String clinicalNotes,

        Instant validUntil
) {}
