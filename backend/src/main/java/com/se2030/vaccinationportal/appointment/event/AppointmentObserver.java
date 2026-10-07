package com.se2030.vaccinationportal.appointment.event;

/** Observer contract; Spring registers implementations with the subject. */
public interface AppointmentObserver {
    void update(AppointmentEvent event);
}
