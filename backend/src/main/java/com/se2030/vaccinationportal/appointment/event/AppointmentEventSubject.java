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
    // All registered observers share the interface, so the subject does not need
    // to know whether an observer handles auditing or notification messages.
    private final List<AppointmentObserver> observers;

    public AppointmentEventSubject(List<AppointmentObserver> observers) {
        // Spring supplies all AppointmentObserver beans when creating this subject.
        // Adding another @Component implementing the interface registers a new observer.
        // The copy keeps this application's subscriber list fixed after construction.
        this.observers = List.copyOf(observers);
    }

    // Business services publish AppointmentEvent through Spring's event publisher.
    // Receive it only after the transaction succeeds: rolled-back changes must not
    // produce a success audit entry or notification. Delivery is synchronous here.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyObservers(AppointmentEvent event) {
        for (AppointmentObserver observer : observers) {
            try {
                // Observer Pattern's notification step: let each subscriber react
                // independently to the appointment change.
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
