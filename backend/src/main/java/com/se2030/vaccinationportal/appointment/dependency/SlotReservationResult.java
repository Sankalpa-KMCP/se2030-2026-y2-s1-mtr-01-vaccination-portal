package com.se2030.vaccinationportal.appointment.dependency;

/**
 * Result enum returned when attempting to reserve capacity on an MF-02 TimeSlot (MF03-BR-03).
 * <p>
 * Result values:
 * <ul>
 *   <li>{@link #SUCCESS}: Capacity was successfully reserved for the time slot.</li>
 *   <li>{@link #SLOT_FULL}: Time slot has no remaining capacity available.</li>
 *   <li>{@link #SLOT_NOT_FOUND}: Specified time slot does not exist.</li>
 * </ul>
 * </p>
 */
public enum SlotReservationResult {
    SUCCESS,
    SLOT_FULL,
    SLOT_NOT_FOUND
}
