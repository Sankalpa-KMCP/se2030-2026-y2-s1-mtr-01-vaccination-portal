package com.se2030.vaccinationportal.vaccination.exception;

/**
 * Thrown when the selected vaccine batch type does not match the expected vaccine type of the
 * appointment's campaign during finalization. Finalization must abort before any irreversible
 * step (record creation, stock deduction, appointment completion, certificate issuance).
 */
public class VaccinationBatchVaccineTypeMismatchException extends RuntimeException {

    public VaccinationBatchVaccineTypeMismatchException(String message) {
        super(message);
    }

    public VaccinationBatchVaccineTypeMismatchException(Long expectedVaccineTypeId, Long actualVaccineTypeId) {
        super("Selected vaccine batch type " + actualVaccineTypeId
                + " does not match the campaign vaccine type " + expectedVaccineTypeId
                + "; finalization rejected");
    }
}
