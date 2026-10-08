package com.se2030.vaccinationportal.shared.auth.exception;

public class InvalidApprovalRoleException extends RuntimeException {
    public InvalidApprovalRoleException() {
        super("Approved staff role is not permitted");
    }
}
