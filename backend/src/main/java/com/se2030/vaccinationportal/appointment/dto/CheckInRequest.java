package com.se2030.vaccinationportal.appointment.dto;

/**
 * Request DTO for appointment arrival check-in.
 *
 * <p>
 * <b>User Story:</b>
 * <ul>
 * <li>US-26: Citizen arrival check-in and queue management.</li>
 * </ul>
 *
 * <p>
 * <b>Business Rules:</b>
 * <ul>
 * <li>MF03-BR-09: Standard check-in window is 30 minutes before to 30 minutes
 * after slot start;
 * overriding outside this window requires {@code isStaffOverride = true} and a
 * non-empty {@code staffReason}.</li>
 * </ul>
 */
public record CheckInRequest(
                Boolean isStaffOverride,
                String staffReason) {
}
