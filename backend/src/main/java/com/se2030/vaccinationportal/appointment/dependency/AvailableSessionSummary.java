package com.se2030.vaccinationportal.appointment.dependency;

import java.time.Instant;

/**
 * Read-only projection of a bookable MF-02 vaccination session for the MF-03 availability search port.
 *
 * <p>Fields are sourced exclusively from fields that are verified to exist on
 * {@link com.se2030.vaccinationportal.campaign.model.VaccinationSession} and
 * {@link com.se2030.vaccinationportal.campaign.model.VaccinationCenter}.
 * Do not add fields that are not backed by those entities.
 *
 * <p>This record is the value type crossing the MF-02 → MF-03 port boundary.
 * MF-02 populates it; MF-03 maps it to {@code AvailableSessionResponse} for the REST layer.
 *
 * @param sessionId         Primary key of the {@code VaccinationSession} (this is the "slotId"
 *                          used when booking via {@code AppointmentBookingService})
 * @param campaignId        Foreign key to the owning {@code VaccinationCampaign}
 * @param centerId          Foreign key to the hosting {@code VaccinationCenter}
 * @param centerName        Display name of the hosting center
 *                          ({@code VaccinationCenter.name})
 * @param centerAddress     Physical address of the hosting center
 *                          ({@code VaccinationCenter.address})
 * @param startAt           Session start timestamp ({@code VaccinationSession.startAt})
 * @param endAt             Session end timestamp ({@code VaccinationSession.endAt})
 * @param bookingCloseAt    Booking acceptance deadline ({@code VaccinationSession.bookingCloseAt})
 * @param remainingCapacity Number of currently bookable places remaining on this session;
 *                          derived from capacity tracking inside MF-02's domain
 */
public record AvailableSessionSummary(
        Long sessionId,
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
