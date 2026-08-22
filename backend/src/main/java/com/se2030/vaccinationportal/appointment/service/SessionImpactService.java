package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link SessionImpactReader} provided by MF-03 (Appointment Management)
 * for consumption by MF-02 (Campaign, Center &amp; Session Management).
 *
 * <p>Canonical Architecture Boundaries (WBVP-SE2030 Section 2, 5, 7, 12):</p>
 * <ul>
 *   <li>MF-03 owns citizen bookings, appointments, and waitlists.</li>
 *   <li>Evaluates authoritative active appointment counts (CONFIRMED, CHECKED_IN) for time slots of a session.</li>
 *   <li>Excludes historical/non-active statuses (CANCELLED, COMPLETED, NO_SHOW).</li>
 *   <li>Waitlist impact is currently 0 until the waitlist subsystem is introduced.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SessionImpactService implements SessionImpactReader {

    private final AppointmentRepository appointmentRepository;

    public SessionImpactService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Retrieves the citizen appointment and waitlist impact for the specified vaccination session.
     *
     * @param sessionId the unique identifier of the vaccination session
     * @return {@link SessionImpactSummary} containing active appointment count and waitlist count
     * @throws IllegalArgumentException if sessionId is null
     */
    @Override
    public SessionImpactSummary getSessionImpact(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }

        long activeAppointmentsCount = appointmentRepository.countActiveAppointmentsBySessionId(sessionId);
        int affectedAppointments = Math.toIntExact(activeAppointmentsCount);

        // Waitlist subsystem is not yet implemented (0 waitlist entries)
        int affectedWaitlist = 0;

        return new SessionImpactSummary(affectedAppointments, affectedWaitlist);
    }
}
