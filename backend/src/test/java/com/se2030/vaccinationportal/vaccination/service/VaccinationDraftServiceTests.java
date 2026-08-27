package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationDraftRepository;
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

    private VaccinationDraftService service;

    @BeforeEach
    void setUp() {
        VaccinationDraftValidator validator = new VaccinationDraftValidator(
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        service = new VaccinationDraftService(draftRepository, validator);
    }

    @Test
    void createDraft_validRequest_savesUnfinalizedDraft() {
        CreateVaccinationDraftRequest request = createRequest(101L);
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(false);
        when(draftRepository.saveAndFlush(any(VaccinationDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationDraft created = service.createDraft(request);

        assertEquals(101L, created.getAppointmentId());
        assertEquals(2, created.getDoseNumber());
        assertFalse(created.isPhysicalAdministrationConfirmed());
        verify(draftRepository).saveAndFlush(created);
    }

    @Test
    void createDraft_duplicateAppointment_isRejectedWithoutSaving() {
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(true);

        assertThrows(DuplicateVaccinationDraftException.class, () -> service.createDraft(createRequest(101L)));

        verify(draftRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_futureAdministrationTime_isRejected() {
        CreateVaccinationDraftRequest request = new CreateVaccinationDraftRequest(
                101L, 201L, 301L, 1, NOW.plusSeconds(60), null, null, false
        );
        when(draftRepository.existsByAppointmentId(101L)).thenReturn(false);

        assertThrows(InvalidVaccinationDraftException.class, () -> service.createDraft(request));

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
        when(draftRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(draftRepository.save(draft)).thenReturn(draft);
        UpdateVaccinationDraftRequest request = new UpdateVaccinationDraftRequest(
                202L,
                302L,
                3,
                NOW.minusSeconds(60),
                LocalDate.of(2026, 9, 20),
                "Updated notes",
                true
        );

        VaccinationDraft updated = service.updateDraft(1L, request);

        assertEquals(202L, updated.getVaccinatorId());
        assertEquals(302L, updated.getVaccineBatchId());
        assertEquals(3, updated.getDoseNumber());
        assertEquals("Updated notes", updated.getAuthorizedNotes());
        assertEquals(true, updated.isPhysicalAdministrationConfirmed());
    }

    @Test
    void updateDraft_emptyPatch_isRejected() {
        UpdateVaccinationDraftRequest empty = new UpdateVaccinationDraftRequest(
                null, null, null, null, null, null, null
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

    private CreateVaccinationDraftRequest createRequest(Long appointmentId) {
        return new CreateVaccinationDraftRequest(
                appointmentId,
                201L,
                301L,
                2,
                NOW.minusSeconds(60),
                LocalDate.of(2026, 9, 20),
                "Initial notes",
                false
        );
    }
}
