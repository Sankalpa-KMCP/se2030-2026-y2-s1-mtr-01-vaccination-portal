package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;


import com.se2030.vaccinationportal.inventory.dependency.BatchAvailability;
import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationPort;
import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.certificate.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.administration.dto.FinalizeVaccinationResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationBatchVaccineTypeMismatchException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.certificate.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationDraftValidator;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional orchestration engine for finalizing vaccination drafts into permanent clinical records.
 */
@Service
public class VaccinationFinalizationService {

    private final VaccinationDraftRepository draftRepository;
    private final VaccinationRecordRepository recordRepository;
    private final AppointmentValidationPort appointmentPort;
    private final InventoryBatchPort inventoryPort;
    private final DigitalCertificateService certificateService;
    private final VaccinationDraftValidator draftValidator;
    private final Clock clock;
    private final VaccinationDraftService draftService;

    public VaccinationFinalizationService(
            VaccinationDraftRepository draftRepository,
            VaccinationRecordRepository recordRepository,
            AppointmentValidationPort appointmentPort,
            InventoryBatchPort inventoryPort,
            DigitalCertificateService certificateService,
            VaccinationDraftValidator draftValidator,
            Clock clock
    ) {
        this(draftRepository, recordRepository, appointmentPort, inventoryPort, certificateService, draftValidator, clock, null);
    }

    @Autowired
    public VaccinationFinalizationService(
            VaccinationDraftRepository draftRepository,
            VaccinationRecordRepository recordRepository,
            AppointmentValidationPort appointmentPort,
            InventoryBatchPort inventoryPort,
            DigitalCertificateService certificateService,
            VaccinationDraftValidator draftValidator,
            Clock clock,
            @Autowired(required = false) VaccinationDraftService draftService
    ) {
        this.draftRepository = draftRepository;
        this.recordRepository = recordRepository;
        this.appointmentPort = appointmentPort;
        this.inventoryPort = inventoryPort;
        this.certificateService = certificateService;
        this.draftValidator = draftValidator;
        this.clock = clock;
        this.draftService = draftService;
    }

    @Transactional
    public FinalizeVaccinationResponse finalizeDraft(Long draftId, Long vaccinatorId) {
        if (draftId == null || draftId <= 0) {
            throw new InvalidVaccinationDraftException("Vaccination draft ID must be positive");
        }

        VaccinationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new VaccinationDraftNotFoundException(
                        "Vaccination draft not found with ID: " + draftId
                ));

        if (draft.getStatus() == VaccinationDraftStatus.FINALIZED) {
            throw new InvalidVaccinationDraftException("Vaccination draft is already finalized: " + draftId);
        }

        if (!draft.isPhysicalAdministrationConfirmed()) {
            throw new InvalidVaccinationDraftException(
                    "Physical administration must be confirmed before finalization"
            );
        }

        if (draft.getStatus() != VaccinationDraftStatus.READY_FOR_FINALIZATION) {
            throw new InvalidVaccinationDraftException(
                    "Vaccination draft must be in READY_FOR_FINALIZATION status to finalize"
            );
        }

        if (draft.getVaccineBatchId() == null || draft.getVaccineBatchId() <= 0) {
            throw new InvalidVaccinationDraftException("Vaccine batch ID is required for finalization");
        }
        if (draft.getDoseNumber() == null || draft.getDoseNumber() <= 0) {
            throw new InvalidVaccinationDraftException("Dose number is required for finalization");
        }
        if (draft.getAdministrationTime() == null) {
            throw new InvalidVaccinationDraftException("Administration time is required for finalization");
        }

        draftValidator.validateDraft(draft);

        if (recordRepository.existsByAppointmentId(draft.getAppointmentId())) {
            throw new VaccinationRecordAlreadyExistsException(
                    "A finalized vaccination record already exists for appointment: " + draft.getAppointmentId()
            );
        }

        AppointmentValidationSnapshot appointment = appointmentPort.findByAppointmentId(draft.getAppointmentId())
                .orElseThrow(() -> new VaccinationAppointmentNotFoundException(
                        "Vaccination appointment not found with ID: " + draft.getAppointmentId()
                ));

        if (!appointment.checkedIn()) {
            throw new AppointmentNotCheckedInException(
                    "Appointment must be checked in to finalize vaccination: " + draft.getAppointmentId()
            );
        }

        Long citizenId = appointment.citizenId();
        if (citizenId == null || citizenId <= 0) {
            throw new InvalidVaccinationDraftException("Appointment citizen ID is required for finalization");
        }

        Long campaignId = appointment.campaignId();
        if (campaignId == null || campaignId <= 0) {
            throw new InvalidVaccinationDraftException("Appointment campaign ID is required for finalization");
        }

        Long centerId = appointment.centerId();
        if (centerId == null || centerId <= 0) {
            throw new InvalidVaccinationDraftException("Appointment center ID could not be resolved for finalization");
        }

        BatchAvailability availability = inventoryPort.getBatchAvailability(centerId, draft.getVaccineBatchId());
        if (availability == null || !availability.isAvailable()) {
            throw new BatchUnavailableException(
                    availability != null ? availability.reason() : BatchAvailabilityReason.BATCH_NOT_FOUND
            );
        }

        // Batch/campaign vaccine type compatibility must be enforced before any irreversible
        // finalization step (record creation, stock deduction, appointment completion, certificate
        // issuance). The expected type is resolved from the authoritative campaign record via the
        // appointment integration port — never from client-supplied draft data.
        Long expectedVaccineTypeId = appointment.campaignVaccineTypeId();
        if (expectedVaccineTypeId == null) {
            throw new InvalidVaccinationDraftException(
                    "Campaign vaccine type could not be resolved for finalization of appointment: "
                            + draft.getAppointmentId()
            );
        }
        if (!expectedVaccineTypeId.equals(availability.vaccineTypeId())) {
            throw new VaccinationBatchVaccineTypeMismatchException(expectedVaccineTypeId, availability.vaccineTypeId());
        }

        Long effectiveVaccinator = draft.getVaccinatorId() != null
                ? draft.getVaccinatorId()
                : vaccinatorId;
        if (effectiveVaccinator == null || effectiveVaccinator <= 0) {
            throw new InvalidVaccinationDraftException("A valid vaccinator ID is required for finalization");
        }

        VaccinationRecord record = new VaccinationRecord(
                draft.getAppointmentId(),
                citizenId,
                campaignId,
                centerId,
                effectiveVaccinator,
                draft.getVaccineBatchId(),
                draft.getDoseNumber(),
                draft.getAdministrationTime(),
                draft.getNextDoseDate(),
                draft.getAuthorizedNotes(),
                clock.instant()
        );
        record = recordRepository.saveAndFlush(record);

        inventoryPort.deductVaccinationDose(centerId, draft.getVaccineBatchId(), record.getId());

        appointmentPort.completeAppointment(draft.getAppointmentId());

        DigitalCertificate certificate = certificateService.issueCertificateForVaccinationRecord(record.getId());

        draft.setStatus(VaccinationDraftStatus.FINALIZED);
        draftRepository.saveAndFlush(draft);

        DigitalCertificateResponse certResponse = DigitalCertificateMapper.toResponse(certificate);

        return new FinalizeVaccinationResponse(
                draft.getId(),
                record.getId(),
                draft.getAppointmentId(),
                citizenId,
                draft.getVaccineBatchId(),
                draft.getDoseNumber(),
                draft.getAdministrationTime(),
                certificate.getCertificateReference(),
                certificate.getVerificationToken(),
                certResponse
        );
    }

    @Transactional
    public FinalizeVaccinationResponse confirmAndFinalizeDraft(Long draftId, Long vaccinatorId) {
        if (draftId == null || draftId <= 0) {
            throw new InvalidVaccinationDraftException("Vaccination draft ID must be positive");
        }

        VaccinationDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new VaccinationDraftNotFoundException(
                        "Vaccination draft not found with ID: " + draftId
                ));

        if (draft.getStatus() == VaccinationDraftStatus.FINALIZED) {
            throw new InvalidVaccinationDraftException("Vaccination draft is already finalized: " + draftId);
        }

        if (!draft.isPhysicalAdministrationConfirmed() || draft.getStatus() != VaccinationDraftStatus.READY_FOR_FINALIZATION) {
            if (draftService != null) {
                draftService.confirmPhysicalAdministration(draftId);
            } else {
                if (draft.getVaccineBatchId() == null || draft.getVaccineBatchId() <= 0) {
                    throw new InvalidVaccinationDraftException(
                            "Vaccine batch ID is required before confirming physical administration"
                    );
                }
                if (draft.getDoseNumber() == null || draft.getDoseNumber() <= 0) {
                    throw new InvalidVaccinationDraftException(
                            "Dose number is required before confirming physical administration"
                    );
                }
                draft.setPhysicalAdministrationConfirmed(true);
                if (draft.getAdministrationTime() == null) {
                    draft.setAdministrationTime(clock != null ? clock.instant() : java.time.Instant.now());
                }
                draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
                draftRepository.saveAndFlush(draft);
            }
        }

        return finalizeDraft(draftId, vaccinatorId);
    }
}
