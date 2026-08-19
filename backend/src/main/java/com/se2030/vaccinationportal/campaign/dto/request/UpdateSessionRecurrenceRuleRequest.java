package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Request payload for replacing a mutable session recurrence rule schedule before occurrences are generated.
 */
public record UpdateSessionRecurrenceRuleRequest(
        @NotEmpty(message = "Recurrence weekdays are required")
        Set<DayOfWeek> weekdays,

        @NotNull(message = "Recurrence start date is required")
        LocalDate startDate,

        @NotNull(message = "Recurrence end date is required")
        LocalDate endDate,

        @NotNull(message = "Recurrence start time is required")
        LocalTime startTime,

        @NotNull(message = "Recurrence end time is required")
        LocalTime endTime,

        @NotBlank(message = "Recurrence timezone is required")
        @Size(max = 64, message = "Timezone cannot exceed 64 characters")
        String timezone
) {
}
