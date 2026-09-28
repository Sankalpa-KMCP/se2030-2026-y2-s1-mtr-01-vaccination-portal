package com.se2030.vaccinationportal.vaccination.validation;

import com.se2030.vaccinationportal.inventory.dependency.BatchAvailability;
import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationBatchVaccineTypeMismatchException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates cross-module appointment state required by vaccination draft workflows.
 */
@Component
public class VaccinationDraftIntegrationValidator {

    private final AppointmentValidationPort appointmentValidationPort;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final InventoryBatchPort inventoryBatchPort;

    public VaccinationDraftIntegrationValidator(
            AppointmentValidationPort appointmentValidationPort,
            VaccinationRecordRepository vaccinationRecordRepository
    ) {
        this(appointmentValidationPort, vaccinationRecordRepository, null);
    }

    @Autowired
    public VaccinationDraftIntegrationValidator(
            AppointmentValidationPort appointmentValidationPort,
            VaccinationRecordRepository vaccinationRecordRepository,
            @Autowired(required = false) InventoryBatchPort inventoryBatchPort
    ) {
        this.appointmentValidationPort = appointmentValidationPort;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.inventoryBatchPort = inventoryBatchPort;
    }

    public void validateAppointment(Long appointmentId) {
        AppointmentValidationSnapshot appointment = appointmentValidationPort.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new VaccinationAppointmentNotFoundException(
                        "Vaccination appointment not found with ID: " + appointmentId
                ));

        if (!appointment.checkedIn()) {
            throw new AppointmentNotCheckedInException(
                    "Appointment must be checked in before vaccination draft processing: " + appointmentId
            );
        }

        if (vaccinationRecordRepository.existsByAppointmentId(appointmentId)) {
            throw new VaccinationRecordAlreadyExistsException(
                    "A finalized vaccination record already exists for appointment: " + appointmentId
            );
        }
    }

    /**
     * Validates that the selected vaccine batch's vaccine type matches the appointment campaign's expected
     * vaccine type before physical administration is confirmed (F4). Fails closed if any authoritative
     * entity or vaccine type cannot be resolved.
     */
    public void validateBatchCompatibility(Long appointmentId, Long vaccineBatchId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID is required for batch compatibility validation");
        }
        if (vaccineBatchId == null) {
            throw new InvalidVaccinationDraftException("Vaccine batch ID is required before confirming physical administration");
        }

        AppointmentValidationSnapshot appointment = appointmentValidationPort.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new VaccinationAppointmentNotFoundException(
                        "Vaccination appointment not found with ID: " + appointmentId
                ));

        Long expectedVaccineTypeId = appointment.campaignVaccineTypeId();
        if (expectedVaccineTypeId == null) {
            throw new InvalidVaccinationDraftException(
                    "Campaign expected vaccine type could not be resolved for appointment: " + appointmentId
            );
        }

        Long centerId = appointment.centerId();
        if (centerId == null || centerId <= 0) {
            throw new InvalidVaccinationDraftException(
                    "Appointment center ID could not be resolved for appointment: " + appointmentId
            );
        }

        if (inventoryBatchPort == null) {
            throw new IllegalStateException("InventoryBatchPort is unavailable for batch compatibility validation");
        }

        BatchAvailability availability = inventoryBatchPort.getBatchAvailability(centerId, vaccineBatchId);
        if (availability == null || !availability.isAvailable()) {
            throw new BatchUnavailableException(
                    availability != null ? availability.reason() : BatchAvailabilityReason.BATCH_NOT_FOUND
            );
        }

        Long batchVaccineTypeId = availability.vaccineTypeId();
        if (batchVaccineTypeId == null) {
            throw new InvalidVaccinationDraftException(
                    "Inventory batch vaccine type could not be resolved for batch: " + vaccineBatchId
            );
        }

        if (!expectedVaccineTypeId.equals(batchVaccineTypeId)) {
            throw new VaccinationBatchVaccineTypeMismatchException(expectedVaccineTypeId, batchVaccineTypeId);
        }
    }
}
