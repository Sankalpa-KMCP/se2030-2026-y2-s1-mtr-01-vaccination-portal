package com.se2030.vaccinationportal.appointment.event;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Subject: notifies registered observers only after the business transaction commits. */
@Component
public class AppointmentEventSubject {
    private static final Logger log = LoggerFactory.getLogger(AppointmentEventSubject.class);
    private final List<AppointmentObserver> observers;

    public AppointmentEventSubject(List<AppointmentObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyObservers(AppointmentEvent event) {
        for (AppointmentObserver observer : observers) {
            try {
                observer.update(event);
            } catch (RuntimeException ex) {
                // One subscriber must not prevent delivery to the others or mask a committed booking.
                log.error("Appointment observer {} failed for appointment {}: {}",
                        observer.getClass().getSimpleName(), event.appointmentId(),
                        ex.getClass().getSimpleName());
            }
        }
    }
}
