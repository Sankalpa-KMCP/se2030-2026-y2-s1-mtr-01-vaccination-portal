package com.se2030.vaccinationportal.appointment.api.vaccination;

import java.util.Optional;

/**
 * Appointment-owned validation and completion boundary for resolving appointment state required by
 * vaccination administration workflows.
 *
 * <p>The provider must not expose its persistence entity through this contract.</p>
 */
public interface AppointmentValidationPort {

    /**
     * Resolves the minimal appointment state required by MF-05.
     *
     * @param appointmentId identifier of the appointment to resolve
     * @return the appointment validation snapshot, or {@link Optional#empty()} when it does not exist
     */
    Optional<AppointmentValidationSnapshot> findByAppointmentId(Long appointmentId);

    /**
     * Transitions the appointment to COMPLETED state upon vaccination finalization.
     *
     * @param appointmentId identifier of the appointment to complete
     */
    default void completeAppointment(Long appointmentId) {
    }
}
