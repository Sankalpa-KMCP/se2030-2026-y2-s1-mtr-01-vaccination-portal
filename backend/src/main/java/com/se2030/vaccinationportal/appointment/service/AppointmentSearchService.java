package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.AvailableSessionSummary;
import com.se2030.vaccinationportal.appointment.dependency.SlotAvailabilityPort;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only service implementing the availability search half of US-03
 * ("As a Citizen, I want to search for and book an available appointment slot at a nearby
 * vaccination center").
 *
 * <p>Queries bookable sessions from MF-02 via {@link SlotAvailabilityPort}, then applies
 * citizen-specific filtering:
 * <ul>
 *   <li>Excludes sessions where the citizen already has a CONFIRMED or CHECKED_IN appointment
 *       for the same campaign (MF03-BR-02 — one active appointment per citizen per campaign).
 *       Although this would be enforced at booking time, surfacing it here prevents the citizen
 *       from seeing sessions they cannot actually book.</li>
 *   <li>Excludes sessions where the citizen already has a WAITING waitlist entry for the same
 *       campaign (MF03-BR-14). Again, prevented at join time, but filtered here for UX clarity.</li>
 * </ul>
 *
 * <p>Eligibility filtering is intentionally <em>not</em> applied here. MF03-BR-01 is enforced
 * at booking/promotion time by the eligibility port (MF-01 concern). Search is deliberately
 * eligibility-agnostic so a citizen can discover sessions even if their current eligibility
 * status is pending review — the booking step will gate them correctly.
 *
 * <p>This service is read-only ({@code readOnly = true}) and holds no write locks.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentSearchService {

    private final SlotAvailabilityPort slotAvailabilityPort;
    private final AppointmentRepository appointmentRepository;
    private final WaitlistRepository waitlistRepository;

    @Autowired
    public AppointmentSearchService(
            @Autowired(required = false) SlotAvailabilityPort slotAvailabilityPort,
            AppointmentRepository appointmentRepository,
            WaitlistRepository waitlistRepository
    ) {
        this.slotAvailabilityPort = slotAvailabilityPort;
        this.appointmentRepository = appointmentRepository;
        this.waitlistRepository = waitlistRepository;
    }

    /**
     * Searches for bookable vaccination sessions visible to the given citizen.
     *
     * <p>Sessions are sourced from MF-02 via {@link SlotAvailabilityPort} and filtered to exclude
     * sessions that the citizen cannot book due to an existing active appointment or waitlist entry
     * for the same campaign (MF03-BR-02, MF03-BR-14).
     *
     * @param citizenId  Identifier of the searching citizen; used for personalised exclusion filter
     * @param campaignId Identifier of the target campaign
     * @param centerId   Optional center filter; {@code null} means all centers
     * @param fromDate   Inclusive start of the search date window
     * @param toDate     Inclusive end of the search date window
     * @return Ordered list of {@link AvailableSessionSummary} visible and bookable by this citizen;
     *         empty list if no sessions are available or if the availability port is not yet wired
     */
    public List<AvailableSessionSummary> searchAvailableSessions(
            Long citizenId,
            Long campaignId,
            Long centerId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID is required");
        }
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("From date and to date are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date must not be after to date");
        }

        // If MF-02 has not yet wired the port, return an empty list rather than throwing.
        // This allows MF-03 tests and controller to function independently of MF-02's deployment state.
        if (slotAvailabilityPort == null) {
            return Collections.emptyList();
        }

        List<AvailableSessionSummary> candidates =
                slotAvailabilityPort.findAvailableSessions(campaignId, centerId, fromDate, toDate);

        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Build exclusion sets — sessions this citizen cannot book for this campaign.
        // MF03-BR-02: citizen already has an active (CONFIRMED/CHECKED_IN) appointment for this campaign.
        // MF03-BR-14: citizen already has a WAITING waitlist entry for this campaign.
        Set<Long> blockedSessionIds = buildBlockedSessionIds(citizenId, campaignId);

        if (blockedSessionIds.isEmpty()) {
            return candidates;
        }

        return candidates.stream()
                .filter(s -> !blockedSessionIds.contains(s.sessionId()))
                .collect(Collectors.toList());
    }

    /**
     * Builds the set of session IDs that this citizen cannot book for the given campaign.
     *
     * <p>A session is blocked when the citizen already has:
     * <ul>
     *   <li>a CONFIRMED or CHECKED_IN appointment targeting that session (MF03-BR-02 —
     *       note: appointment.slotId maps to session.id in the current MF-02 data model); or</li>
     *   <li>a WAITING waitlist entry targeting that session (MF03-BR-14).</li>
     * </ul>
     *
     * <p>MF03-BR-14 enforces at most one WAITING entry per citizen per campaign, so
     * {@code findByCitizenIdAndCampaignIdAndStatus} returning {@code Optional} is sufficient.
     */
    private Set<Long> buildBlockedSessionIds(Long citizenId, Long campaignId) {
        Set<Long> blockedSlots = new HashSet<>(
                appointmentRepository
                        .findByCitizenIdAndCampaignIdAndStatusIn(
                                citizenId,
                                campaignId,
                                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
                        )
                        .stream()
                        .map(appt -> appt.getSlotId())
                        .collect(Collectors.toSet())
        );

        waitlistRepository
                .findByCitizenIdAndCampaignIdAndStatus(citizenId, campaignId, WaitlistStatus.WAITING)
                .ifPresent(entry -> blockedSlots.add(entry.getSessionId()));

        return blockedSlots;
    }
}
