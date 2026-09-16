package com.se2030.vaccinationportal.inventory.exception;

public class DuplicateVaccinationDeductionException extends IllegalStateException {
    public DuplicateVaccinationDeductionException(Long vaccinationRecordId) {
        super("Vaccination record already deducted stock: " + vaccinationRecordId);
    }
}
