package com.se2030.vaccinationportal.vaccination.mapper;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationDraftResponse;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;

/**
 * Pure transformations between vaccination draft REST DTOs and the MF-05 entity.
 */
public final class VaccinationDraftMapper {

    private VaccinationDraftMapper() {
    }

    public static VaccinationDraft fromCreateRequest(CreateVaccinationDraftRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create vaccination draft request is required");
        }

        VaccinationDraft draft = new VaccinationDraft(request.appointmentId());
        draft.setVaccinatorId(request.vaccinatorId());
        draft.setVaccineBatchId(request.vaccineBatchId());
        draft.setDoseNumber(request.doseNumber());
        draft.setAdministrationTime(request.administrationTime());
        draft.setNextDoseDate(request.nextDoseDate());
        draft.setAuthorizedNotes(request.authorizedNotes());
        draft.setPhysicalAdministrationConfirmed(Boolean.TRUE.equals(request.physicalAdministrationConfirmed()));
        return draft;
    }

    public static void applyUpdate(UpdateVaccinationDraftRequest request, VaccinationDraft draft) {
        if (request == null) {
            throw new IllegalArgumentException("Update vaccination draft request is required");
        }
        if (draft == null) {
            throw new IllegalArgumentException("Vaccination draft is required");
        }

        if (request.vaccinatorId() != null) {
            draft.setVaccinatorId(request.vaccinatorId());
        }
        if (request.vaccineBatchId() != null) {
            draft.setVaccineBatchId(request.vaccineBatchId());
        }
        if (request.doseNumber() != null) {
            draft.setDoseNumber(request.doseNumber());
        }
        if (request.administrationTime() != null) {
            draft.setAdministrationTime(request.administrationTime());
        }
        if (request.nextDoseDate() != null) {
            draft.setNextDoseDate(request.nextDoseDate());
        }
        if (request.authorizedNotes() != null) {
            draft.setAuthorizedNotes(request.authorizedNotes());
        }
        if (request.physicalAdministrationConfirmed() != null) {
            draft.setPhysicalAdministrationConfirmed(request.physicalAdministrationConfirmed());
        }
    }

    public static VaccinationDraftResponse toResponse(VaccinationDraft draft) {
        if (draft == null) {
            return null;
        }
        return new VaccinationDraftResponse(
                draft.getId(),
                draft.getAppointmentId(),
                draft.getVaccinatorId(),
                draft.getVaccineBatchId(),
                draft.getDoseNumber(),
                draft.getAdministrationTime(),
                draft.getNextDoseDate(),
                draft.getAuthorizedNotes(),
                draft.isPhysicalAdministrationConfirmed(),
                draft.getStatus(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
