package com.se2030.vaccinationportal.appointment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Independent audit subscriber; records committed changes in the application log. */
@Component
public class AppointmentAuditObserver implements AppointmentObserver {
    private static final Logger log = LoggerFactory.getLogger(AppointmentAuditObserver.class);

    @Override
    public void update(AppointmentEvent event) {
        log.info("[MF03 AUDIT] action={} appointment={} citizen={} campaign={} slot={} occurredAt={}",
                event.type(), event.appointmentId(), event.citizenId(), event.campaignId(),
                event.slotId(), event.occurredAt());
    }
}
