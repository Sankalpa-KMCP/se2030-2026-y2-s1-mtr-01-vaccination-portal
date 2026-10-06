package com.se2030.vaccinationportal.shared.auth.dto;

import java.time.Instant;

public record EmailVerificationStatusResponse(
        boolean emailVerified,
        Instant emailVerifiedAt
) {
}
