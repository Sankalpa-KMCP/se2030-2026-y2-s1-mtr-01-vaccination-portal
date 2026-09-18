package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationDraftNotFoundException;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationDraftMapper;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraftDeletionAudit;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationDraftDeletionAuditRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationDraftRepository;
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

        VaccinationDraftMapper.applyUpdate(request, draft);
        validator.validateDraft(draft);
        integrationValidator.validateAppointment(draft.getAppointmentId());
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
