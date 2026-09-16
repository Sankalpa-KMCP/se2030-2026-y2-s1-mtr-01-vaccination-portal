package com.se2030.vaccinationportal.appointment.exception;

/**
 * Unchecked exception thrown when a time slot has no available bookable capacity (MF03-BR-03).
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Initial appointment booking and atomic rescheduling.</li>
 *   <li>US-05: Prompts citizen to join waitlist when all slots are full.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-03: Capacity allocation is atomic and prevents two concurrent users taking the last place.</li>
 * </ul>
 */
public class SlotFullException extends RuntimeException {

    public SlotFullException(String message) {
        super(message);
    }

    public SlotFullException(String message, Throwable cause) {
        super(message, cause);
    }
}
