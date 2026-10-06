package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.inventory.dependency.BatchAvailability;
import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationPort;
import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.administration.dto.FinalizeVaccinationResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationBatchVaccineTypeMismatchException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationDraftValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VaccinationFinalizationServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-19T10:00:00Z");

    @Mock
    private VaccinationDraftRepository draftRepository;

    @Mock
    private VaccinationRecordRepository recordRepository;

    @Mock
    private AppointmentValidationPort appointmentPort;

    @Mock
    private InventoryBatchPort inventoryPort;

    @Mock
    private DigitalCertificateService certificateService;

    private VaccinationDraftValidator draftValidator;
    private VaccinationFinalizationService finalizationService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        draftValidator = new VaccinationDraftValidator(clock);
        finalizationService = new VaccinationFinalizationService(
                draftRepository,
                recordRepository,
                appointmentPort,
                inventoryPort,
                certificateService,
                draftValidator,
                clock
        );
    }

    @Test
    void successfulFinalization_createsRecordDeductsInventoryCompletesAppointmentIssuesCertificate() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationRecord savedRecord = new VaccinationRecord(
                42L, 501L, 601L, 10L, 201L, 12L, 1, NOW, null, "Authorized notes", NOW
        );
        ReflectionTestUtils.setField(savedRecord, "id", 777L);
        when(recordRepository.saveAndFlush(any(VaccinationRecord.class))).thenReturn(savedRecord);

        DigitalCertificate certificate = new DigitalCertificate(
                savedRecord.getId(), "CERT-2026-XYZ", "tok_verify_123", NOW
        );
        when(certificateService.issueCertificateForVaccinationRecord(savedRecord.getId())).thenReturn(certificate);

        FinalizeVaccinationResponse response = finalizationService.finalizeDraft(101L, 201L);

        assertNotNull(response);
        assertEquals(101L, response.draftId());
        assertEquals(777L, response.vaccinationRecordId());
        assertEquals("CERT-2026-XYZ", response.certificateReference());
        assertEquals(VaccinationDraftStatus.FINALIZED, draft.getStatus());

        verify(recordRepository).saveAndFlush(any(VaccinationRecord.class));
        verify(inventoryPort).deductVaccinationDose(10L, 12L, savedRecord.getId());
        verify(appointmentPort).completeAppointment(42L);
        verify(certificateService).issueCertificateForVaccinationRecord(savedRecord.getId());
        verify(draftRepository).saveAndFlush(draft);
    }

    @Test
    void finalize_draftWithoutPhysicalConfirmation_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(42L);
        draft.setPhysicalAdministrationConfirmed(false);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        draft.setVaccineBatchId(12L);
        draft.setDoseNumber(1);
        draft.setAdministrationTime(NOW);

        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals("Physical administration must be confirmed before finalization", ex.getMessage());
        verify(recordRepository, never()).saveAndFlush(any());
    }

    @Test
    void finalize_draftNotInReadyStatus_throwsException() {
        VaccinationDraft draft = new VaccinationDraft(42L);
        draft.setPhysicalAdministrationConfirmed(true);
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);
        draft.setVaccineBatchId(12L);
        draft.setDoseNumber(1);
        draft.setAdministrationTime(NOW);

        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals("Vaccination draft must be in READY_FOR_FINALIZATION status to finalize", ex.getMessage());
    }

    @Test
    void finalize_alreadyFinalizedDraft_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        draft.setStatus(VaccinationDraftStatus.FINALIZED);

        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals("Vaccination draft is already finalized: 101", ex.getMessage());
    }

    @Test
    void finalize_appointmentNotCheckedIn_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot notCheckedIn = new AppointmentValidationSnapshot(
                42L, false, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(notCheckedIn));

        assertThrows(
                AppointmentNotCheckedInException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        verify(recordRepository, never()).saveAndFlush(any());
    }

    @Test
    void finalize_recordAlreadyExists_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(true);

        assertThrows(
                VaccinationRecordAlreadyExistsException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
    }

    @Test
    void finalize_missingCitizenId_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, null, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals("Appointment citizen ID is required for finalization", ex.getMessage());
    }

    @Test
    void finalize_missingCampaignId_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, null, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals("Appointment campaign ID is required for finalization", ex.getMessage());
    }

    @Test
    void finalize_missingCenterId_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, null, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals("Appointment center ID could not be resolved for finalization", ex.getMessage());
    }

    @Test
    void finalize_missingVaccinatorId_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        draft.setVaccinatorId(null);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));
        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, null)
        );
        assertEquals("A valid vaccinator ID is required for finalization", ex.getMessage());
    }

    @Test
    void finalize_batchUnavailable_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability unavailable = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 0, BatchAvailabilityReason.INSUFFICIENT_STOCK
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(unavailable);

        assertThrows(
                BatchUnavailableException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        verify(recordRepository, never()).saveAndFlush(any());
    }

    @Test
    void finalize_inventoryDeductionFailure_propagatesException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationRecord savedRecord = new VaccinationRecord(
                42L, 501L, 601L, 10L, 201L, 12L, 1, NOW, null, "Authorized notes", NOW
        );
        ReflectionTestUtils.setField(savedRecord, "id", 777L);
        when(recordRepository.saveAndFlush(any(VaccinationRecord.class))).thenReturn(savedRecord);

        doThrow(new BatchUnavailableException(BatchAvailabilityReason.EXPIRED_BATCH))
                .when(inventoryPort).deductVaccinationDose(10L, 12L, 777L);

        assertThrows(
                BatchUnavailableException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        verify(draftRepository, never()).saveAndFlush(argThat(d -> d.getStatus() == VaccinationDraftStatus.FINALIZED));
    }

    @Test
    void finalize_certificateGenerationFailure_propagatesException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationRecord savedRecord = new VaccinationRecord(
                42L, 501L, 601L, 10L, 201L, 12L, 1, NOW, null, "Authorized notes", NOW
        );
        ReflectionTestUtils.setField(savedRecord, "id", 777L);
        when(recordRepository.saveAndFlush(any(VaccinationRecord.class))).thenReturn(savedRecord);

        doThrow(new RuntimeException("Certificate signature failure"))
                .when(certificateService).issueCertificateForVaccinationRecord(777L);

        assertThrows(
                RuntimeException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        verify(draftRepository, never()).saveAndFlush(argThat(d -> d.getStatus() == VaccinationDraftStatus.FINALIZED));
    }

    private VaccinationDraft readyDraft(Long draftId, Long appointmentId) {
        VaccinationDraft draft = new VaccinationDraft(appointmentId);
        ReflectionTestUtils.setField(draft, "id", draftId);
        draft.setVaccinatorId(201L);
        draft.setVaccineBatchId(12L);
        draft.setDoseNumber(1);
        draft.setAdministrationTime(NOW);
        draft.setAuthorizedNotes("Authorized notes");
        draft.setPhysicalAdministrationConfirmed(true);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        return draft;
    }

    @Test
    void finalize_batchVaccineTypeMatchesCampaign_completesAllFinalizationSteps() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 7L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 7L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationRecord savedRecord = new VaccinationRecord(
                42L, 501L, 601L, 10L, 201L, 12L, 1, NOW, null, "Authorized notes", NOW
        );
        ReflectionTestUtils.setField(savedRecord, "id", 777L);
        when(recordRepository.saveAndFlush(any(VaccinationRecord.class))).thenReturn(savedRecord);

        DigitalCertificate certificate = new DigitalCertificate(
                savedRecord.getId(), "CERT-2026-MATCH", "tok_match_123", NOW
        );
        when(certificateService.issueCertificateForVaccinationRecord(savedRecord.getId())).thenReturn(certificate);

        FinalizeVaccinationResponse response = finalizationService.finalizeDraft(101L, 201L);

        assertNotNull(response);
        verify(inventoryPort).deductVaccinationDose(10L, 12L, 777L);
        verify(appointmentPort).completeAppointment(42L);
        verify(certificateService).issueCertificateForVaccinationRecord(777L);
        verify(draftRepository).saveAndFlush(draft);
    }

    @Test
    void finalize_batchVaccineTypeMismatch_abortsBeforeAnyIrreversibleStep() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        // Campaign expects vaccine type 7; the selected batch holds vaccine type 1 (an unrelated type)
        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 7L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationBatchVaccineTypeMismatchException ex = assertThrows(
                VaccinationBatchVaccineTypeMismatchException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertTrue(ex.getMessage().contains("7"));

        verify(recordRepository, never()).saveAndFlush(any(VaccinationRecord.class));
        verify(inventoryPort, never()).deductVaccinationDose(any(), any(), any());
        verify(appointmentPort, never()).completeAppointment(any());
        verify(certificateService, never()).issueCertificateForVaccinationRecord(any());
        verify(draftRepository, never()).saveAndFlush(argThat(d -> d.getStatus() == VaccinationDraftStatus.FINALIZED));
    }

    @Test
    void finalize_batchVaccineTypeMismatch_doesNotMutateStockOrAppointment() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 2L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 9L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        assertThrows(
                VaccinationBatchVaccineTypeMismatchException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );

        // Zero stock mutation and zero appointment mutation on mismatch
        verify(inventoryPort, never()).deductVaccinationDose(any(), any(), any());
        verify(appointmentPort, never()).completeAppointment(any());
        assertEquals(VaccinationDraftStatus.READY_FOR_FINALIZATION, draft.getStatus());
    }

    @Test
    void finalize_missingCampaignVaccineType_throwsException() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, null
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        InvalidVaccinationDraftException ex = assertThrows(
                InvalidVaccinationDraftException.class,
                () -> finalizationService.finalizeDraft(101L, 201L)
        );
        assertEquals(
                "Campaign vaccine type could not be resolved for finalization of appointment: 42",
                ex.getMessage()
        );
        verify(recordRepository, never()).saveAndFlush(any());
        verify(inventoryPort, never()).deductVaccinationDose(any(), any(), any());
    }

    @Test
    void confirmAndFinalizeDraft_whenAlreadyReady_finalizesDirectly() {
        VaccinationDraft draft = readyDraft(101L, 42L);
        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationRecord savedRecord = new VaccinationRecord(
                42L, 501L, 601L, 10L, 201L, 12L, 1, NOW, null, "Authorized notes", NOW
        );
        ReflectionTestUtils.setField(savedRecord, "id", 777L);
        when(recordRepository.saveAndFlush(any(VaccinationRecord.class))).thenReturn(savedRecord);

        DigitalCertificate certificate = new DigitalCertificate(
                savedRecord.getId(), "CERT-2026-XYZ", "tok_verify_123", NOW
        );
        when(certificateService.issueCertificateForVaccinationRecord(savedRecord.getId())).thenReturn(certificate);

        FinalizeVaccinationResponse response = finalizationService.confirmAndFinalizeDraft(101L, 201L);

        assertNotNull(response);
        assertEquals(101L, response.draftId());
        assertEquals(VaccinationDraftStatus.FINALIZED, draft.getStatus());
        verify(recordRepository).saveAndFlush(any(VaccinationRecord.class));
    }

    @Test
    void confirmAndFinalizeDraft_whenInProgress_confirmsPhysicalThenFinalizes() {
        VaccinationDraft draft = new VaccinationDraft(42L);
        ReflectionTestUtils.setField(draft, "id", 101L);
        draft.setVaccinatorId(201L);
        draft.setVaccineBatchId(12L);
        draft.setDoseNumber(1);
        draft.setAdministrationTime(NOW);
        draft.setAuthorizedNotes("Authorized notes");
        draft.setPhysicalAdministrationConfirmed(false);
        draft.setStatus(VaccinationDraftStatus.IN_PROGRESS);

        when(draftRepository.findById(101L)).thenReturn(Optional.of(draft));
        when(recordRepository.existsByAppointmentId(42L)).thenReturn(false);

        AppointmentValidationSnapshot appointment = new AppointmentValidationSnapshot(
                42L, true, 501L, 601L, 701L, 10L, 1L
        );
        when(appointmentPort.findByAppointmentId(42L)).thenReturn(Optional.of(appointment));

        BatchAvailability available = new BatchAvailability(
                12L, 1L, BatchStatus.ACTIVE, LocalDate.of(2027, 1, 1), 50, BatchAvailabilityReason.AVAILABLE
        );
        when(inventoryPort.getBatchAvailability(10L, 12L)).thenReturn(available);

        VaccinationRecord savedRecord = new VaccinationRecord(
                42L, 501L, 601L, 10L, 201L, 12L, 1, NOW, null, "Authorized notes", NOW
        );
        ReflectionTestUtils.setField(savedRecord, "id", 777L);
        when(recordRepository.saveAndFlush(any(VaccinationRecord.class))).thenReturn(savedRecord);

        DigitalCertificate certificate = new DigitalCertificate(
                savedRecord.getId(), "CERT-2026-XYZ", "tok_verify_123", NOW
        );
        when(certificateService.issueCertificateForVaccinationRecord(savedRecord.getId())).thenReturn(certificate);

        FinalizeVaccinationResponse response = finalizationService.confirmAndFinalizeDraft(101L, 201L);

        assertNotNull(response);
        assertEquals(101L, response.draftId());
        assertTrue(draft.isPhysicalAdministrationConfirmed());
        assertEquals(VaccinationDraftStatus.FINALIZED, draft.getStatus());
        verify(recordRepository).saveAndFlush(any(VaccinationRecord.class));
    }
}