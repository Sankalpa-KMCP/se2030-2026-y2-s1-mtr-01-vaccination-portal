package com.se2030.vaccinationportal.vaccination.validation;

import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraftStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * MF-05 business validation that is independent of appointment and inventory modules.
 */
@Component
public class VaccinationDraftValidator {

    private static final int MAX_NOTES_LENGTH = 2000;
    private static final Set<VaccinationDraftStatus> EDITABLE_STATUSES = EnumSet.of(
            VaccinationDraftStatus.IN_PROGRESS,
            VaccinationDraftStatus.READY_FOR_FINALIZATION
    );

    private final Clock clock;

    public VaccinationDraftValidator(Clock clock) {
        this.clock = clock;
    }

    public void validateDraft(VaccinationDraft draft) {
        if (draft == null) {
            throw new InvalidVaccinationDraftException("Vaccination draft is required");
        }
        requirePositive(draft.getAppointmentId(), "Appointment ID");
        requirePositiveIfPresent(draft.getVaccinatorId(), "Vaccinator ID");
        requirePositiveIfPresent(draft.getVaccineBatchId(), "Vaccine batch ID");
        if (draft.getDoseNumber() != null && draft.getDoseNumber() <= 0) {
            throw new InvalidVaccinationDraftException("Dose number must be positive");
        }
        if (draft.getAdministrationTime() != null && draft.getAdministrationTime().isAfter(clock.instant())) {
            throw new InvalidVaccinationDraftException("Administration time cannot be in the future");
        }
        if (draft.getAuthorizedNotes() != null && draft.getAuthorizedNotes().length() > MAX_NOTES_LENGTH) {
            throw new InvalidVaccinationDraftException(
                    "Authorized notes must not exceed " + MAX_NOTES_LENGTH + " characters"
            );
        }
        if (draft.isPhysicalAdministrationConfirmed() && draft.getAdministrationTime() == null) {
            throw new InvalidVaccinationDraftException(
                    "Administration time is required when physical administration is confirmed"
            );
        }
        validateNextDoseDate(draft);
    }

    public void validateEditable(VaccinationDraft draft) {
        if (draft == null || draft.getStatus() == null || !EDITABLE_STATUSES.contains(draft.getStatus())) {
            throw new InvalidVaccinationDraftException("Only an unfinalized vaccination draft may be changed");
        }
    }

    public void validateUpdateRequest(UpdateVaccinationDraftRequest request) {
        if (request == null || !request.hasChanges()) {
            throw new InvalidVaccinationDraftException("At least one editable draft field must be provided");
        }
    }

    private void validateNextDoseDate(VaccinationDraft draft) {
        if (draft.getNextDoseDate() == null || draft.getAdministrationTime() == null) {
            return;
        }
        LocalDate administrationDate = LocalDate.ofInstant(draft.getAdministrationTime(), ZoneOffset.UTC);
        if (draft.getNextDoseDate().isBefore(administrationDate)) {
            throw new InvalidVaccinationDraftException(
                    "Next dose date cannot be before the administration date"
            );
        }
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidVaccinationDraftException(fieldName + " must be positive");
        }
    }

    private void requirePositiveIfPresent(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new InvalidVaccinationDraftException(fieldName + " must be positive");
        }
    }
}
