package com.se2030.vaccinationportal.appointment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Independent audit subscriber; records committed changes in the application log. */
// Spring discovers this component and includes it in the subject's observer list.
@Component
public class AppointmentAuditObserver implements AppointmentObserver {
    private static final Logger log = LoggerFactory.getLogger(AppointmentAuditObserver.class);

    // Concrete observer: implements the shared update method with an audit action.
    @Override
    public void update(AppointmentEvent event) {
        // Record what happened, which records were involved, and the event time.
        // This writes to the application log; it does not insert a database audit row.
        log.info("[MF03 AUDIT] action={} appointment={} citizen={} campaign={} slot={} occurredAt={}",
                event.type(), event.appointmentId(), event.citizenId(), event.campaignId(),
                event.slotId(), event.occurredAt());
    }
}
