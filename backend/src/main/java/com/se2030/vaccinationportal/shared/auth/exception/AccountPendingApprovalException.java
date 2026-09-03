package com.se2030.vaccinationportal.shared.auth.exception;

public class AccountPendingApprovalException extends RuntimeException {
    public AccountPendingApprovalException() {
        super("Staff account is pending administrator approval");
    }
}
