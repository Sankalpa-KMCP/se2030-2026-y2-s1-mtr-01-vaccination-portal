package com.se2030.vaccinationportal.vaccination.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for revoking the current certificate and issuing its replacement. */
public record ReissueDigitalCertificateRequest(
        @NotBlank(message = "Reissue reason is required")
        @Size(max = 1000, message = "Reissue reason must not exceed 1000 characters")
        String reason,

        Long reissuedBy
) {
    public ReissueDigitalCertificateRequest(String reason) {
        this(reason, null);
    }
}
