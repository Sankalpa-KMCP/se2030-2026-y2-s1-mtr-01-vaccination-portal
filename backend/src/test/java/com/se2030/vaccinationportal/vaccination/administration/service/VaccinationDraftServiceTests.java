package com.se2030.vaccinationportal.vaccination.administration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.administration.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationDraftIntegrationValidator;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationDraftValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationDraftServiceTests {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");

    @Mock
    private VaccinationDraftRepository draftRepository;

    @Mock
    private VaccinationDraftIntegrationValidator integrationValidator;

    private VaccinationDraftService service;

    @BeforeEach
    void setUp() {
        VaccinationDraftValidator validator = new VaccinationDraftValidator(
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        service = new VaccinationDraftService(draftRepository, validator, integrationValidator);
    }

    @Test
    void createDraft_validRequest_savesUnfinalizedDraft() {
        CreateVaccinationDraftRequest request = createRequest(101L);
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(false);
        when(draftRepository.saveAndFlush(any(VaccinationDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationDraft created = service.createDraft(request, 201L);

        assertEquals(101L, created.getAppointmentId());
        assertEquals(201L, created.getVaccinatorId());
        assertEquals(2, created.getDoseNumber());
        assertFalse(created.isPhysicalAdministrationConfirmed());
        verify(integrationValidator).validateAppointment(101L);
        verify(draftRepository).saveAndFlush(created);
    }

    @Test
    void createDraft_duplicateAppointment_isRejectedWithoutSaving() {
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(true);

        assertThrows(DuplicateVaccinationDraftException.class, () -> service.createDraft(createRequest(101L), 201L));

        verify(draftRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_futureAdministrationTime_isRejected() {
        CreateVaccinationDraftRequest request = new CreateVaccinationDraftRequest(
                101L, 301L, 1, NOW.plusSeconds(60), null, null
        );
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(false);

        assertThrows(InvalidVaccinationDraftException.class, () -> service.createDraft(request, 201L));

        verify(draftRepository, never()).saveAndFlush(any());
    }

    @Test
    void getDraftById_existingDraft_isReturned() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertSame(draft, service.getDraftById(1L));
    }

    @Test
    void getDraftById_unknownDraft_throwsNotFound() {
        when(draftRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VaccinationDraftNotFoundException.class, () -> service.getDraftById(999L));
    }

    @Test
    void getAllDrafts_returnsRepositoryResults() {
        VaccinationDraft first = new VaccinationDraft(101L);
        VaccinationDraft second = new VaccinationDraft(102L);
        when(draftRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(first, second));

        assertEquals(List.of(first, second), service.getAllDrafts());
    }

    @Test
    void updateDraft_permittedFields_areApplied() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccinatorId(201L);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(draftRepository.save(draft)).thenReturn(draft);
        UpdateVaccinationDraftRequest request = new UpdateVaccinationDraftRequest(
                302L,
                3,
                NOW.minusSeconds(60),
                LocalDate.of(2026, 9, 20),
                "Updated notes"
        );

        VaccinationDraft updated = service.updateDraft(1L, request);

        assertEquals(201L, updated.getVaccinatorId());
        assertEquals(302L, updated.getVaccineBatchId());
        assertEquals(3, updated.getDoseNumber());
        assertEquals("Updated notes", updated.getAuthorizedNotes());
        assertFalse(updated.isPhysicalAdministrationConfirmed());
        verify(integrationValidator).validateAppointment(101L);
    }

    @Test
    void createDraft_failedIntegrationValidationDoesNotPersist() {
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(false);
        doThrow(new AppointmentNotCheckedInException("Appointment is not checked in"))
                .when(integrationValidator).validateAppointment(101L);

        assertThrows(
                AppointmentNotCheckedInException.class,
                () -> service.createDraft(createRequest(101L), 201L)
        );

        verify(draftRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_whenBatchUnavailable_throwsExceptionAndDoesNotPersist() {
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(false);
        doThrow(new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.EXPIRED_BATCH))
                .when(integrationValidator).validateBatchCompatibility(101L, 301L);

        assertThrows(
                com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException.class,
                () -> service.createDraft(createRequest(101L), 201L)
        );

        verify(draftRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateDraft_whenBatchUnavailable_throwsExceptionAndDoesNotSave() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);
        draft.setPhysicalAdministrationConfirmed(false);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        UpdateVaccinationDraftRequest request = new UpdateVaccinationDraftRequest(
                301L, null, null, null, null
        );
        doThrow(new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.INSUFFICIENT_STOCK))
                .when(integrationValidator).validateBatchCompatibility(101L, 301L);

        assertThrows(
                com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException.class,
                () -> service.updateDraft(1L, request)
        );

        verify(draftRepository, never()).save(any());
    }

    @Test
    void updateDraft_emptyPatch_isRejected() {
        UpdateVaccinationDraftRequest empty = new UpdateVaccinationDraftRequest(
                null, null, null, null, null
        );

        assertThrows(InvalidVaccinationDraftException.class, () -> service.updateDraft(1L, empty));

        verify(draftRepository, never()).findById(any());
    }

    @Test
    void deleteDraft_existingDraft_isPermanentlyDeleted() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        service.deleteDraft(1L);

        verify(draftRepository).delete(draft);
    }

    @Test
    void deleteDraft_unknownDraft_throwsNotFound() {
        when(draftRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VaccinationDraftNotFoundException.class, () -> service.deleteDraft(999L));

        verify(draftRepository, never()).delete(any());
    }

    @Test
    void createDraft_nullRequestOrInvalidIdentityIsRejected() {
        assertThrows(InvalidVaccinationDraftException.class, () -> service.createDraft(null, 201L));
        assertThrows(InvalidVaccinationDraftException.class, () -> service.createDraft(createRequest(0L), 201L));
        assertThrows(InvalidVaccinationDraftException.class, () -> service.createDraft(createRequest(101L), null));
        assertThrows(InvalidVaccinationDraftException.class, () -> service.createDraft(createRequest(101L), 0L));
        verify(draftRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidDraftIdsAreRejected() {
        assertThrows(InvalidVaccinationDraftException.class, () -> service.getDraftById(0L));
        assertThrows(InvalidVaccinationDraftException.class, () -> service.deleteDraft(-1L));
        verify(draftRepository, never()).findById(any());
    }

    @Test
    void updateAndDeleteRejectNonEditableDraft() {
        VaccinationDraft draft = org.mockito.Mockito.mock(VaccinationDraft.class);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        assertThrows(InvalidVaccinationDraftException.class,
                () -> service.updateDraft(1L, new UpdateVaccinationDraftRequest(null, 2, null, null, null)));
        assertThrows(InvalidVaccinationDraftException.class, () -> service.deleteDraft(1L));
        verify(draftRepository, never()).save(any());
        verify(draftRepository, never()).delete(any());
    }

    @Test
    void updateDraft_whenPhysicalAdministrationConfirmed_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccinatorId(201L);
        draft.setAdministrationTime(NOW.minusSeconds(60));
        draft.setPhysicalAdministrationConfirmed(true);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThrows(InvalidVaccinationDraftException.class,
                () -> service.updateDraft(1L, new UpdateVaccinationDraftRequest(null, null, null, null, "Notes")));
        verify(draftRepository, never()).save(any());
    }

    @Test
    void deleteDraft_whenPhysicalAdministrationConfirmed_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setPhysicalAdministrationConfirmed(true);
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThrows(InvalidVaccinationDraftException.class,
                () -> service.deleteDraft(1L, "Test reason", 201L));
        verify(draftRepository, never()).delete(any());
    }

    @Test
    void confirmPhysicalAdministration_validDraft_setsConfirmedAndReadyForFinalization() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(301L);
        draft.setDoseNumber(1);
        draft.setAdministrationTime(NOW.minusSeconds(60));
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);
        draft.setPhysicalAdministrationConfirmed(false);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(draftRepository.save(draft)).thenReturn(draft);

        VaccinationDraft confirmed = service.confirmPhysicalAdministration(1L);

        assertTrue(confirmed.isPhysicalAdministrationConfirmed());
        assertEquals(VaccinationDraftStatus.READY_FOR_FINALIZATION, confirmed.getStatus());
        verify(draftRepository).save(draft);
    }

    @Test
    void confirmPhysicalAdministration_alreadyConfirmed_returnsDraftDeterministically() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(301L);
        draft.setDoseNumber(1);
        Instant originalTime = NOW.minusSeconds(120);
        draft.setAdministrationTime(originalTime);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        draft.setPhysicalAdministrationConfirmed(true);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        VaccinationDraft result = service.confirmPhysicalAdministration(1L);

        assertTrue(result.isPhysicalAdministrationConfirmed());
        assertEquals(originalTime, result.getAdministrationTime());
        verify(draftRepository, never()).save(any());
    }

    @Test
    void confirmPhysicalAdministration_missingBatchId_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(null);
        draft.setDoseNumber(1);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThrows(InvalidVaccinationDraftException.class,
                () -> service.confirmPhysicalAdministration(1L));
        verify(draftRepository, never()).save(any());
    }

    @Test
    void confirmPhysicalAdministration_missingDoseNumber_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(301L);
        draft.setDoseNumber(null);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThrows(InvalidVaccinationDraftException.class,
                () -> service.confirmPhysicalAdministration(1L));
        verify(draftRepository, never()).save(any());
    }

    @Test
    void confirmPhysicalAdministration_finalizedDraft_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setStatus(VaccinationDraftStatus.FINALIZED);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThrows(InvalidVaccinationDraftException.class,
                () -> service.confirmPhysicalAdministration(1L));
        verify(draftRepository, never()).save(any());
    }

    @Test
    void confirmPhysicalAdministration_unknownDraft_throwsNotFound() {
        when(draftRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VaccinationDraftNotFoundException.class,
                () -> service.confirmPhysicalAdministration(999L));
    }

    @Test
    void confirmPhysicalAdministration_whenBatchTypeMismatched_throwsExceptionAndDraftRemainsUnconfirmedAndEditable() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(301L);
        draft.setDoseNumber(1);
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);
        draft.setPhysicalAdministrationConfirmed(false);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        doThrow(new com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationBatchVaccineTypeMismatchException(10L, 20L))
                .when(integrationValidator).validateBatchCompatibility(101L, 301L);

        assertThrows(
                com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationBatchVaccineTypeMismatchException.class,
                () -> service.confirmPhysicalAdministration(1L)
        );

        assertFalse(draft.isPhysicalAdministrationConfirmed(), "Draft must remain unconfirmed on type mismatch");
        assertEquals(VaccinationDraftStatus.IN_PROGRESS, draft.getStatus(), "Draft status must not transition");
        verify(draftRepository, never()).save(any());
    }

    @Test
    void confirmPhysicalAdministration_whenBatchUnavailable_throwsExceptionAndDraftRemainsUnconfirmedAndEditable() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(301L);
        draft.setDoseNumber(1);
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);
        draft.setPhysicalAdministrationConfirmed(false);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        doThrow(new com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException(
                com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.BATCH_NOT_FOUND))
                .when(integrationValidator).validateBatchCompatibility(101L, 301L);

        assertThrows(
                com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException.class,
                () -> service.confirmPhysicalAdministration(1L)
        );

        assertFalse(draft.isPhysicalAdministrationConfirmed(), "Draft must remain unconfirmed when batch is unavailable");
        assertEquals(VaccinationDraftStatus.IN_PROGRESS, draft.getStatus(), "Draft status must not transition");
        verify(draftRepository, never()).save(any());
    }

    @Test
    void confirmPhysicalAdministration_whenExpectedCampaignTypeNull_throwsExceptionAndDraftRemainsUnconfirmedAndEditable() {
        VaccinationDraft draft = new VaccinationDraft(101L);
        draft.setVaccineBatchId(301L);
        draft.setDoseNumber(1);
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);
        draft.setPhysicalAdministrationConfirmed(false);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        doThrow(new InvalidVaccinationDraftException("Campaign expected vaccine type could not be resolved"))
                .when(integrationValidator).validateBatchCompatibility(101L, 301L);

        assertThrows(
                InvalidVaccinationDraftException.class,
                () -> service.confirmPhysicalAdministration(1L)
        );

        assertFalse(draft.isPhysicalAdministrationConfirmed(), "Draft must remain unconfirmed when campaign type is null");
        assertEquals(VaccinationDraftStatus.IN_PROGRESS, draft.getStatus(), "Draft status must not transition");
        verify(draftRepository, never()).save(any());
    }

    private CreateVaccinationDraftRequest createRequest(Long appointmentId) {
        return new CreateVaccinationDraftRequest(
                appointmentId,
                301L,
                2,
                NOW.minusSeconds(60),
                LocalDate.of(2026, 9, 20),
                "Initial notes"
        );
    }
}