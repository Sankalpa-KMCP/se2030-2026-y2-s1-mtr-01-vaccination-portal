package com.se2030.vaccinationportal.appointment.dependency;

/**
 * Result enum returned when attempting to reserve capacity on an MF-02 TimeSlot (MF03-BR-03).
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Booking and rescheduling appointments.</li>
 *   <li>US-05: Promoting waitlist entries into confirmed appointments.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-03: Atomic capacity allocation preventing overbooking of the final slot capacity.</li>
 * </ul>
 *
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
