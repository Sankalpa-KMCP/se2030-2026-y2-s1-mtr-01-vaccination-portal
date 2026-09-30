package com.se2030.vaccinationportal.vaccination.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VaccinationDraftValidatorTests {
    private static final Instant NOW = Instant.parse("2026-09-16T10:00:00Z");
    private final VaccinationDraftValidator validator = new VaccinationDraftValidator(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void rejectsMissingDraftAndInvalidAppointmentOrBatch() {
        assertThrows(InvalidVaccinationDraftException.class, () -> validator.validateDraft(null));
        VaccinationDraft invalidAppointment = new VaccinationDraft(0L);
        assertThrows(InvalidVaccinationDraftException.class, () -> validator.validateDraft(invalidAppointment));
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(0L);
        assertThrows(InvalidVaccinationDraftException.class, () -> validator.validateDraft(draft));
    }

    @Test
    void rejectsFutureAdministrationTime() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setAdministrationTime(NOW.plusSeconds(1));
        assertThrows(InvalidVaccinationDraftException.class, () -> validator.validateDraft(draft));
    }

    @Test
    void rejectsPastNextDoseDateEvenWithoutAdministrationTime() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setNextDoseDate(LocalDate.of(2026, 9, 15));
        assertThrows(InvalidVaccinationDraftException.class, () -> validator.validateDraft(draft));
    }

    @Test
    void allowsIncompleteDraftAndPresentDates() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        assertDoesNotThrow(() -> validator.validateDraft(draft));
        draft.setAdministrationTime(NOW);
        draft.setNextDoseDate(LocalDate.of(2026, 9, 16));
        assertDoesNotThrow(() -> validator.validateDraft(draft));
    }

    @ParameterizedTest
    @EnumSource(value = VaccinationDraftStatus.class, names = {"IN_PROGRESS", "READY_FOR_FINALIZATION"})
    void existingUnfinalizedStatesRemainEditable(VaccinationDraftStatus status) {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setStatus(status);
        assertDoesNotThrow(() -> validator.validateEditable(draft));
    }

    @Test
    void finalizedStateIsNotEditable() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setStatus(VaccinationDraftStatus.FINALIZED);
        org.junit.jupiter.api.Assertions.assertThrows(
                com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException.class,
                () -> validator.validateEditable(draft)
        );
    }
}
