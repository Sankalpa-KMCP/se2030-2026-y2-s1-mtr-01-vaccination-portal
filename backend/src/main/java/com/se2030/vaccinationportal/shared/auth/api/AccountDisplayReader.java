package com.se2030.vaccinationportal.shared.auth.api;

import java.util.Optional;

/**
 * Minimal account display facts for an already authorized application workflow.
 * The calling feature owns access to the record containing the account identity;
 * this internal reader does not grant access to accounts or expose account state.
 */
public interface AccountDisplayReader {
    Optional<String> findFullName(Long accountId);

    /** Full name when present, otherwise the account email for staff display. */
    Optional<String> findDisplayName(Long accountId);
}
