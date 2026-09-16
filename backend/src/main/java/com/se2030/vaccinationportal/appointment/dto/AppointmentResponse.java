package com.se2030.vaccinationportal.appointment.dto;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import java.time.Instant;

/**
 * REST response DTO representing an Appointment entity.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Returned upon booking, cancellation, rescheduling, and citizen history queries.</li>
 *   <li>US-26: Returned upon arrival check-in, check-in reversal, and vaccinator queue queries.</li>
 * </ul>
 */
public record AppointmentResponse(
        Long id,
        Long citizenId,
        Long campaignId,
        Long slotId,
        AppointmentStatus status,
        Instant bookedAt,
        Instant cancelledAt,
        Long lockVersion
) {
    public static AppointmentResponse fromEntity(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCitizenId(),
                appointment.getCampaignId(),
                appointment.getSlotId(),
                appointment.getStatus(),
                appointment.getBookedAt(),
                appointment.getCancelledAt(),
                appointment.getLockVersion()
        );
    }
}
