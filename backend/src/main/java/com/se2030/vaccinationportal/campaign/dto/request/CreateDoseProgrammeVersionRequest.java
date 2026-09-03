package com.se2030.vaccinationportal.campaign.dto.request;

import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Request payload for creating a new dose programme version for a campaign.
 */
public record CreateDoseProgrammeVersionRequest(
        @NotNull(message = "Dose category is required")
        DoseCategory doseCategory,

        @NotBlank(message = "Permitted dose numbers are required")
        String permittedDoseNumbers,

        @NotNull(message = "Minimum interval days is required")
        @PositiveOrZero(message = "Minimum interval days must be non-negative")
        Integer minimumIntervalDays,

        Boolean previousEvidenceRequired,

        Integer expectedServiceMinutes,

        @NotNull(message = "Effective start date is required")
        LocalDate effectiveStartDate
) {
    public boolean isPreviousEvidenceRequired() {
        return Boolean.TRUE.equals(previousEvidenceRequired);
    }
}
