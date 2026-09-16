package com.se2030.vaccinationportal.appointment.dependency;

import java.time.Instant;

/**
 * Read-only projection of a bookable MF-02 vaccination session for the MF-03 availability search port.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Search available vaccination appointment slots.</li>
 * </ul>
 *
 * <p>Fields are sourced exclusively from fields that are verified to exist on
 * {@link com.se2030.vaccinationportal.campaign.model.VaccinationSession} and
 * {@link com.se2030.vaccinationportal.campaign.model.VaccinationCenter} and
 * {@link com.se2030.vaccinationportal.campaign.model.TimeSlot}.
 * Do not add fields that are not backed by those entities.
 *
 * <p>This record is the value type crossing the MF-02 → MF-03 port boundary.
 * The MF-03 read adapter populates it from MF-02; MF-03 maps it to {@code AvailableSessionResponse} for the REST layer.
 *
 * @param sessionId         Primary key of the owning {@code VaccinationSession}
 * @param slotId            Primary key of the concrete {@code TimeSlot} used for booking
 * @param campaignId        Foreign key to the owning {@code VaccinationCampaign}
 * @param centerId          Foreign key to the hosting {@code VaccinationCenter}
 * @param centerName        Display name of the hosting center
 *                          ({@code VaccinationCenter.name})
 * @param centerAddress     Physical address of the hosting center
 *                          ({@code VaccinationCenter.address})
 * @param startAt           Slot start timestamp ({@code TimeSlot.startTime})
 * @param endAt             Slot end timestamp ({@code TimeSlot.endTime})
 * @param bookingCloseAt    Booking acceptance deadline ({@code VaccinationSession.bookingCloseAt})
 * @param remainingCapacity Number of currently bookable places remaining on this slot;
 *                          derived from capacity tracking inside MF-02's domain
 */
public record AvailableSessionSummary(
        Long sessionId,
        Long slotId,
        Long campaignId,
        Long centerId,
        String centerName,
        String centerAddress,
        Instant startAt,
        Instant endAt,
        Instant bookingCloseAt,
        int remainingCapacity
) {
}
