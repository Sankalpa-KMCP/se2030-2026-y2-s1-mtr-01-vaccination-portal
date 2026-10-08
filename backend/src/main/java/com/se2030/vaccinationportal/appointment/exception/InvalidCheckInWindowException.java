package com.se2030.vaccinationportal.appointment.exception;

/**
 * Exception thrown when a check-in or no-show attempt violates the allowable check-in window (MF03-BR-09).
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-26: Check-in and no-show arrival processing.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-09: Standard check-in window is 30 minutes before to 30 minutes after slot start;
 *       earlier or later check-in requires authorized staff override with reason.
 *       Similarly, NO_SHOW cannot be marked until the 30-minute window has fully elapsed.</li>
 * </ul>
 */
public class InvalidCheckInWindowException extends RuntimeException {

    public InvalidCheckInWindowException(String message) {
        super(message);
    }

    public InvalidCheckInWindowException(String message, Throwable cause) {
        super(message, cause);
    }
}
