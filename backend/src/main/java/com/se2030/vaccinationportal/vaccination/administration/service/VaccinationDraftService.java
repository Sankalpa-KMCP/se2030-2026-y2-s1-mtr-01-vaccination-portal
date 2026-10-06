package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.mapper.VaccinationDraftMapper;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftDeletionAudit;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftDeletionAuditRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationDraftIntegrationValidator;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationDraftValidator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for temporary vaccination draft CRUD only.
 */
@Service
@Transactional
public class VaccinationDraftService {

    private final VaccinationDraftRepository draftRepository;
    private final VaccinationDraftDeletionAuditRepository draftDeletionAuditRepository;
    private final VaccinationDraftValidator validator;
    private final VaccinationDraftIntegrationValidator integrationValidator;
    private final Clock clock;

    public VaccinationDraftService(
            VaccinationDraftRepository draftRepository,
            VaccinationDraftValidator validator,
            VaccinationDraftIntegrationValidator integrationValidator
    ) {
        this(draftRepository, null, validator, integrationValidator, Clock.systemUTC());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public VaccinationDraftService(
            VaccinationDraftRepository draftRepository,
            VaccinationDraftDeletionAuditRepository draftDeletionAuditRepository,
            VaccinationDraftValidator validator,
            VaccinationDraftIntegrationValidator integrationValidator,
            Clock clock
    ) {
        this.draftRepository = draftRepository;
        this.draftDeletionAuditRepository = draftDeletionAuditRepository;
        this.validator = validator;
        this.integrationValidator = integrationValidator;
        this.clock = clock;
    }

    public VaccinationDraft createDraft(CreateVaccinationDraftRequest request, Long vaccinatorId) {
        if (request == null) {
            throw new InvalidVaccinationDraftException("Create vaccination draft request is required");
        }
        if (request.appointmentId() == null || request.appointmentId() <= 0) {
            throw new InvalidVaccinationDraftException("Appointment ID must be positive");
        }
        if (vaccinatorId == null || vaccinatorId <= 0) {
            throw new InvalidVaccinationDraftException("Authenticated vaccinator ID must be positive");
        }
        if (draftRepository.existsByAppointmentId(request.appointmentId())) {
            throw duplicateForAppointment(request.appointmentId(), null);
        }

        VaccinationDraft draft = VaccinationDraftMapper.fromCreateRequest(request);
        draft.setVaccinatorId(vaccinatorId);
        validator.validateDraft(draft);
        integrationValidator.validateAppointment(draft.getAppointmentId());
        if (draft.getVaccineBatchId() != null) {
            integrationValidator.validateBatchCompatibility(draft.getAppointmentId(), draft.getVaccineBatchId());
        }

        try {
            return draftRepository.saveAndFlush(draft);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateForAppointment(request.appointmentId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public VaccinationDraft getDraftById(Long id) {
        requirePositiveId(id);
        return draftRepository.findById(id)
                .orElseThrow(() -> new VaccinationDraftNotFoundException(
                        "Vaccination draft not found with ID: " + id
                ));
    }

    @Transactional(readOnly = true)
    public List<VaccinationDraft> getAllDrafts() {
        return draftRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<VaccinationDraft> getAllDraftsByVaccinatorId(Long vaccinatorId) {
        if (vaccinatorId == null || vaccinatorId <= 0) {
            throw new InvalidVaccinationDraftException("Vaccinator ID must be positive");
        }
        return draftRepository.findAllByVaccinatorIdOrderByCreatedAtDesc(vaccinatorId);
    }

    public VaccinationDraft updateDraft(Long id, UpdateVaccinationDraftRequest request) {
        validator.validateUpdateRequest(request);
        VaccinationDraft draft = getDraftById(id);
        validator.validateEditable(draft);
        if (draft.isPhysicalAdministrationConfirmed()) {
            throw new InvalidVaccinationDraftException(
                    "Vaccination draft cannot be edited after physical administration has been confirmed"
            );
        }

        VaccinationDraftMapper.applyUpdate(request, draft);
        validator.validateDraft(draft);
        integrationValidator.validateAppointment(draft.getAppointmentId());
        if (draft.getVaccineBatchId() != null) {
            integrationValidator.validateBatchCompatibility(draft.getAppointmentId(), draft.getVaccineBatchId());
        }
        return draftRepository.save(draft);
    }

    public VaccinationDraft confirmPhysicalAdministration(Long id) {
        requirePositiveId(id);
        VaccinationDraft draft = getDraftById(id);
        validator.validateEditable(draft);

        if (draft.isPhysicalAdministrationConfirmed()) {
            return draft;
        }

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

        integrationValidator.validateAppointment(draft.getAppointmentId());
        integrationValidator.validateBatchCompatibility(draft.getAppointmentId(), draft.getVaccineBatchId());

        draft.setPhysicalAdministrationConfirmed(true);
        if (draft.getAdministrationTime() == null) {
            draft.setAdministrationTime(clock != null ? clock.instant() : Instant.now());
        }
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        validator.validateDraft(draft);
        return draftRepository.save(draft);
    }

    public void deleteDraft(Long id, String reason, Long deletedBy) {
        requirePositiveId(id);
        if (deletedBy == null || deletedBy <= 0) {
            throw new InvalidVaccinationDraftException("Deleted-by ID must be positive");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidVaccinationDraftException("Draft deletion reason is required and cannot be blank");
        }
        if (reason.trim().length() > 1000) {
            throw new InvalidVaccinationDraftException("Draft deletion reason must not exceed 1000 characters");
        }
        VaccinationDraft draft = getDraftById(id);
        validator.validateEditable(draft);
        if (draft.isPhysicalAdministrationConfirmed()) {
            throw new InvalidVaccinationDraftException(
                    "Vaccination draft cannot be deleted after physical administration has been confirmed"
            );
        }

        if (draftDeletionAuditRepository != null) {
            VaccinationDraftDeletionAudit audit = new VaccinationDraftDeletionAudit(
                    draft.getId(),
                    draft.getAppointmentId(),
                    draft.getVaccinatorId() != null ? draft.getVaccinatorId() : deletedBy,
                    deletedBy,
                    reason.trim(),
                    clock != null ? clock.instant() : Instant.now()
            );
            draftDeletionAuditRepository.save(audit);
        }
        draftRepository.delete(draft);
    }

    public void deleteDraft(Long id) {
        deleteDraft(id, "Administrative removal", 1L);
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidVaccinationDraftException("Vaccination draft ID must be positive");
        }
    }

    private DuplicateVaccinationDraftException duplicateForAppointment(Long appointmentId, Throwable cause) {
        String message = "An active vaccination draft already exists for appointment: " + appointmentId;
        return cause == null
                ? new DuplicateVaccinationDraftException(message)
                : new DuplicateVaccinationDraftException(message, cause);
    }
}
