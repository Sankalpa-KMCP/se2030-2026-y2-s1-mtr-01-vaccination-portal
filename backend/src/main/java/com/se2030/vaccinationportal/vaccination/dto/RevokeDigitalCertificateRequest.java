package com.se2030.vaccinationportal.vaccination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for revoking a valid digital certificate. */
public record RevokeDigitalCertificateRequest(
        @NotBlank(message = "Revocation reason is required")
        @Size(max = 1000, message = "Revocation reason must not exceed 1000 characters")
        String reason,

        Long revokedBy
) {
    public RevokeDigitalCertificateRequest(String reason) {
        this(reason, null);
    }
}
