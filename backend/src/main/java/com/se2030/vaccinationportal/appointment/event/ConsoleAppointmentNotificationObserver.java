package com.se2030.vaccinationportal.appointment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Demonstration notification delivery to the application console, not email or SMS. */
@Component
public class ConsoleAppointmentNotificationObserver implements AppointmentObserver {
    private static final Logger log = LoggerFactory.getLogger(ConsoleAppointmentNotificationObserver.class);

    @Override
    public void update(AppointmentEvent event) {
        String message = switch (event.type()) {
            case BOOKED -> "Your vaccination appointment is confirmed.";
            case CANCELLED -> "Your vaccination appointment has been cancelled.";
            case WAITLIST_PROMOTED -> "You have been promoted from the waitlist to a confirmed appointment.";
        };
        log.info("[MF03 NOTIFICATION] citizen={} appointment={} message={}",
                event.citizenId(), event.appointmentId(), message);
    }
}
