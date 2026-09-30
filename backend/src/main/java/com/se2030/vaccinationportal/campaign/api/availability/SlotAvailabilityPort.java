package com.se2030.vaccinationportal.campaign.api.availability;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;

import java.time.LocalDate;
import java.util.List;

/**
 * Cross-module read-only integration port for querying bookable MF-02 vaccination sessions (US-03).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search for available appointment slots.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-01: Eligibility is intentionally deferred to booking time, keeping search open for discovery.</li>
 * </ul>
 *
 * <p>MF-03 defines this contract; an MF-03 read adapter consumes existing MF-02 services to implement it.
 * This port is intentionally separate from {@link SlotCapacityPort}, which handles write operations
 * (reserve/release). Keeping read and write port boundaries distinct avoids coupling MF-03's search
 * path to the mutation path and makes each contract independently testable via a test double.
 *
 * <p>The adapter only reads MF-02 state; capacity calculation and reservation remain owned by MF-02.
 */
public interface SlotAvailabilityPort {

    /**
     * Returns all bookable vaccination sessions for a campaign within the given date window,
     * optionally restricted to a specific vaccination center.
     *
     * <p>A session is considered bookable when:
     * <ul>
     *   <li>its {@code lifecycleStatus} is {@code SCHEDULED}
     *       (the state accepted by the current MF-02 reservation service);</li>
     *   <li>its campaign permits intake and booking is not suspended;</li>
 *   <li>its {@code bookingCloseAt} has not yet passed at query time;</li>
     *   <li>its remaining capacity is greater than zero.</li>
     * </ul>
     *
     * <p>MF-03 does <em>not</em> apply eligibility filtering here. Eligibility is enforced at
     * booking time by {@link com.se2030.vaccinationportal.appointment.service.AppointmentBookingService}
     * via its eligibility boundary (MF03-BR-01). Search intentionally shows all available
     * sessions regardless of the citizen's current eligibility status.
     *
     * @param campaignId Identifier of the target campaign (required)
     * @param centerId   Identifier of the vaccination center to restrict results to;
     *                   {@code null} means no center restriction (return all centers)
     * @param fromDate   Inclusive lower bound on session start date (required)
     * @param toDate     Inclusive upper bound on session start date (required)
     * @return Ordered list of {@link AvailableSessionSummary} records representing bookable sessions;
     *         empty if no sessions match the criteria
     */
    List<AvailableSessionSummary> findAvailableSessions(
            Long campaignId,
            Long centerId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
