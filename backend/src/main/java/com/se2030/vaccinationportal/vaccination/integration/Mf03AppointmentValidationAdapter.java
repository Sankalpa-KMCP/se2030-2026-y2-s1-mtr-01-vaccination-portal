package com.se2030.vaccinationportal.vaccination.integration;

import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the minimal MF-03 appointment state required by MF-05 validation.
 */
@Component
public class Mf03AppointmentValidationAdapter implements AppointmentValidationPort {

    private final AppointmentRepository appointmentRepository;

    public Mf03AppointmentValidationAdapter(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public Optional<AppointmentValidationSnapshot> findByAppointmentId(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> new AppointmentValidationSnapshot(
                        appointment.getId(),
                        appointment.getStatus() == AppointmentStatus.CHECKED_IN
                ));
    }
}
