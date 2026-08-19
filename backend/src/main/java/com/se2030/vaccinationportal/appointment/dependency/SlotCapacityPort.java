package com.se2030.vaccinationportal.appointment.dependency;

/**
 * Cross-module integration port to check and reserve capacity on an MF-02 TimeSlot (MF03-BR-03).
 * <p>
 * Implemented by MF-02 (Campaign, Center &amp; Session). Must be concurrency-safe on MF-02's side;
 * MF-03 must not write its own capacity decrement against MF-02's tables.
 * </p>
 */
public interface SlotCapacityPort {

    /**
     * Checks whether the specified time slot currently has bookable capacity available.
     *
     * @param slotId Identifier of the time slot
     * @return {@code true} if capacity remains; {@code false} otherwise
     */
    boolean hasAvailableCapacity(Long slotId);

    /**
     * Atomically reserves one place of capacity on the specified time slot.
     * Must execute concurrency-safe capacity decrement inside MF-02's transactional domain.
     *
     * @param slotId Identifier of the time slot
     * @return {@link SlotReservationResult} indicating reservation success or failure state
     */
    SlotReservationResult reserveCapacity(Long slotId);
}
