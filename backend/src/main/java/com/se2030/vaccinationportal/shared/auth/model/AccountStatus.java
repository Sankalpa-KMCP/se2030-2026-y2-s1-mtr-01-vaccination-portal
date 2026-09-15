package com.se2030.vaccinationportal.shared.auth.model;

/**
 * Canonical Account Status Lifecycle
 * Canonical Section 12.2 (UserAccount.account_status)
 */
public enum AccountStatus {
    ACTIVE,
    PENDING_VERIFICATION,
    PENDING_APPROVAL,
    REJECTED,
    LOCKED,
    SUSPENDED
}
