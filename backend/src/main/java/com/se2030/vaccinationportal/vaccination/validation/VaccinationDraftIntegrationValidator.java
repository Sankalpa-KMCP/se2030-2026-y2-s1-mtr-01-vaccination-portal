package com.se2030.vaccinationportal.vaccination.validation;

import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import org.springframework.stereotype.Component;

/**
 * Validates cross-module appointment state required by vaccination draft workflows.
 */
@Component
public class VaccinationDraftIntegrationValidator {

    private final AppointmentValidationPort appointmentValidationPort;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    public VaccinationDraftIntegrationValidator(
            AppointmentValidationPort appointmentValidationPort,
            VaccinationRecordRepository vaccinationRecordRepository
    ) {
        this.appointmentValidationPort = appointmentValidationPort;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
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
}
