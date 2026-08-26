package com.se2030.vaccinationportal.appointment.model;

/**
 * Status enumeration for waitlist entries (MF-03 Canonical Section 7.4).
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
    CANCELLED,
    SESSION_CANCELLED,
    EXPIRED
}
