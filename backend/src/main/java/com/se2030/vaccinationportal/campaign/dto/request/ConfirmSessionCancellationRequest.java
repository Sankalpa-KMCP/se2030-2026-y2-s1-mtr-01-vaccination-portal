package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for confirming a Vaccination Session cancellation.
 */
public record ConfirmSessionCancellationRequest(
        @NotBlank(message = "Preview token is required")
        String previewToken,

        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason,

        @NotBlank(message = "Actor ID is required")
        @Size(max = 64, message = "Actor ID cannot exceed 64 characters")
        String actorId
) {
}
