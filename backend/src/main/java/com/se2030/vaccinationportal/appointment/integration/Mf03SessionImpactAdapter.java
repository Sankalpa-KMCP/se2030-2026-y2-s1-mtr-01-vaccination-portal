package com.se2030.vaccinationportal.appointment.integration;

import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import java.util.List;
import org.springframework.stereotype.Component;

/** Supplies MF-03's active appointment and waitlist counts to campaign session change control. */
@Component
public class Mf03SessionImpactAdapter implements SessionImpactReader {

    private static final List<AppointmentStatus> AFFECTED_APPOINTMENT_STATUSES =
            List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN);

    private final AppointmentRepository appointmentRepository;
    private final WaitlistRepository waitlistRepository;

    public Mf03SessionImpactAdapter(
            AppointmentRepository appointmentRepository,
            WaitlistRepository waitlistRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.waitlistRepository = waitlistRepository;
    }

    @Override
    public SessionImpactSummary getSessionImpact(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }

        long appointments = appointmentRepository.countBySessionIdAndStatusIn(
                sessionId, AFFECTED_APPOINTMENT_STATUSES);
        long waitlist = waitlistRepository.countBySessionIdAndStatus(sessionId, WaitlistStatus.WAITING);
        return new SessionImpactSummary(Math.toIntExact(appointments), Math.toIntExact(waitlist));
    }
}
