package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a citizen attempts to cancel or reschedule an appointment
 * within 24 hours of session start without authorized staff override and reason (MF03-BR-04).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-03: Self-service modification window enforcement.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-04: Citizen self-cancellation/rescheduling is allowed until 24 hours before slot start;
 *       later changes require staff override with reason.</li>
 * </ul>
 */
public class RequiresStaffOverrideException extends RuntimeException {

    public RequiresStaffOverrideException(String message) {
        super(message);
    }

    public RequiresStaffOverrideException(String message, Throwable cause) {
        super(message, cause);
    }
}
