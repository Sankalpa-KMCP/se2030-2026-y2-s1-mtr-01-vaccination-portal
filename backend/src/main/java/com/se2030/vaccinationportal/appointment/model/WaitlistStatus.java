package com.se2030.vaccinationportal.appointment.model;

/**
 * Status enumeration for waitlist entries (MF-03 Canonical Section 7.4).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Citizen waitlist lifecycle tracking and automated promotion.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-06: WAITING represents active presence in the FIFO queue.</li>
 *   <li>MF03-BR-07: SKIPPED_INELIGIBLE and SKIPPED_DUPLICATE_APPOINTMENT record promotion bypass reasons.</li>
 *   <li>MF03-BR-08: PROMOTED marks successful conversion into a confirmed appointment.</li>
 *   <li>MF03-BR-14: WAITING is mutually exclusive with active appointment holdings.</li>
 * </ul>
 *
 * <p>
 * Separate status model from {@link AppointmentStatus}.
 * States:
 * <ul>
 *   <li>{@link #WAITING}: Citizen is actively queued in the session waitlist.</li>
 *   <li>{@link #PROMOTED}: Citizen was promoted from waitlist into a confirmed appointment.</li>
 *   <li>{@link #SKIPPED_INELIGIBLE}: Promotion skipped citizen because screening assessment was no longer ELIGIBLE.</li>
 *   <li>{@link #SKIPPED_DUPLICATE_APPOINTMENT}: Promotion skipped citizen because an active appointment already exists.</li>
 *   <li>{@link #CANCELLED}: Waitlist entry was cancelled by citizen or staff request.</li>
 *   <li>{@link #SESSION_CANCELLED}: Waitlist entry was terminated due to session cancellation.</li>
 *   <li>{@link #EXPIRED}: Waitlist entry expired after session elapsed without open capacity.</li>
 * </ul>
 * </p>
 */
public enum WaitlistStatus {
    WAITING,
    PROMOTED,
    SKIPPED_INELIGIBLE,
    SKIPPED_DUPLICATE_APPOINTMENT,
    SKIPPED_MALFORMED,
    CANCELLED,
    SESSION_CANCELLED,
    EXPIRED
}
