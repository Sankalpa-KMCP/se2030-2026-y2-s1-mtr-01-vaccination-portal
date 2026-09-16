package com.se2030.vaccinationportal.appointment.model;

/**
 * Canonical appointment status enumeration defining the lifecycle stages of an appointment.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Search, book, cancel, and reschedule appointments (transitions CONFIRMED, CANCELLED).</li>
 *   <li>US-26: View and process checked-in appointments (transitions CHECKED_IN, COMPLETED, NO_SHOW).</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-02: Only {@code CONFIRMED} and {@code CHECKED_IN} statuses represent active bookings
 *       guarded by the single active appointment constraint.</li>
 *   <li>MF03-BR-10: Only {@code CONFIRMED} appointments can transition to {@code CHECKED_IN}.</li>
 *   <li>MF03-BR-11: {@code COMPLETED} is immutable and terminal; cannot be cancelled, rescheduled, or re-checked in.</li>
 *   <li>MF03-BR-15: {@code CHECKED_IN} can be reversed back to {@code CONFIRMED} prior to completion.</li>
 * </ul>
 *
 * <p>Canonical section 7.4. Kept strictly separate from {@code WaitlistStatus} and {@code CheckInEventType}.</p>
 */
public enum AppointmentStatus {
    CONFIRMED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
