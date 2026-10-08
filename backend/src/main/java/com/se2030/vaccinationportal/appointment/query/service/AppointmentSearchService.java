package com.se2030.vaccinationportal.appointment.query.service;

import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;
import com.se2030.vaccinationportal.campaign.api.availability.SlotAvailabilityPort;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only service implementing the availability search half of US-03
 * ("As a Citizen, I want to search for and book an available appointment slot at a nearby
 * vaccination center").
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Citizen searches available vaccination sessions and slots.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-01: Eligibility is deliberately not filtered during search so citizens can discover slots;
 *       it is strictly enforced at booking time.</li>
 *   <li>MF03-BR-02: Excludes slots where the citizen already holds a CONFIRMED or CHECKED_IN appointment
 *       for the same campaign, though alternative slots remain visible for rescheduling.</li>
 *   <li>MF03-BR-14: Excludes sessions where the citizen already holds a WAITING waitlist entry for the same campaign.</li>
 * </ul>
 *
 * <p>Queries bookable sessions from MF-02 via {@link SlotAvailabilityPort}, then applies
 * citizen-specific filtering.
 * This service is read-only ({@code readOnly = true}) and holds no write locks.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentSearchService {

    private final SlotAvailabilityPort slotAvailabilityPort;
    private final AppointmentRepository appointmentRepository;
    private final WaitlistRepository waitlistRepository;

    @Autowired
    public AppointmentSearchService(
            SlotAvailabilityPort slotAvailabilityPort,
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
     *         empty list if no sessions are available
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

        List<AvailableSessionSummary> candidates =
                slotAvailabilityPort.findAvailableSessions(campaignId, centerId, fromDate, toDate);

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // Hide already-held time slots, but keep alternatives available for rescheduling.
        // The booking service enforces the campaign-wide active-appointment invariant.
        var bookedSlotIds = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                        citizenId, campaignId,
                        List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)).stream()
                .map(appointment -> appointment.getSlotId())
                .collect(Collectors.toSet());
        // Waitlists exclude an entire session; appointments exclude only their held slot.
        var waitingEntry = waitlistRepository.findFirstByCitizenIdAndCampaignIdAndStatusOrderByJoinedAtDesc(
                citizenId, campaignId, WaitlistStatus.WAITING);
        return candidates.stream()
                .filter(candidate -> !bookedSlotIds.contains(candidate.slotId()))
                .filter(candidate -> waitingEntry
                        .map(entry -> !entry.getSessionId().equals(candidate.sessionId()))
                        .orElse(true))
                .toList();
    }
}
