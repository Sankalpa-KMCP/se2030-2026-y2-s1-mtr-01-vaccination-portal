package com.se2030.vaccinationportal.appointment.dependency;

import java.time.Instant;

/**
 * Cross-module integration port to check, reserve, release capacity, and inspect schedule details on an MF-02 TimeSlot (MF03-BR-03, MF03-BR-05).
 * <p>
 * Implemented by MF-02 (Campaign, Center &amp; Session). Must be concurrency-safe on MF-02's side;
 * MF-03 must not write its own capacity decrement or increment against MF-02's tables.
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

    /**
     * Atomically releases one place of capacity back to the specified time slot (MF03-BR-05).
     * Must execute concurrency-safe capacity increment inside MF-02's transactional domain.
     *
     * @param slotId Identifier of the time slot
     * @return {@code true} if capacity was successfully released; {@code false} otherwise
     */
    boolean releaseCapacity(Long slotId);

    /**
     * Retrieves the scheduled start timestamp of the specified time slot for 24-hour window validation (MF03-BR-04).
     *
     * @param slotId Identifier of the time slot
     * @return Scheduled start {@link Instant} of the time slot, or {@code null} if not found
     */
    Instant getSlotStartTime(Long slotId);

    /**
     * Checks whether the specified session is currently full across all its time slots (MF03-BR-06).
     *
     * @param sessionId Identifier of the vaccination session
     * @return {@code true} if the session has no remaining capacity across any time slot; {@code false} otherwise
     */
    boolean isSessionFull(Long sessionId);
}
