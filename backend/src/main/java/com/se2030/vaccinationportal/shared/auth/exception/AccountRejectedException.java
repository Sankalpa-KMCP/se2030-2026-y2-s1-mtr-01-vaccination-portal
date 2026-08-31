package com.se2030.vaccinationportal.shared.auth.exception;

public class AccountRejectedException extends RuntimeException {
    public AccountRejectedException() {
        super("Staff account request was rejected");
    }
}
