package com.se2030.vaccinationportal.campaign.session.recurrence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for confirming and applying full series cancellation.
 */
public record ConfirmSeriesCancellationRequest(
        @NotBlank(message = "Preview token is required")
        String previewToken,

        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason,

        @Size(max = 64, message = "Actor ID cannot exceed 64 characters")
        String actorId
) {
}
