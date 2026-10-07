package com.se2030.vaccinationportal.campaign.api.capacity;

import java.time.Instant;
import java.util.Optional;

/**
 * Cross-module integration port to check, reserve, release capacity, and inspect schedule details on an MF-02 TimeSlot.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Booking, cancellation, and atomic rescheduling of appointment slots.</li>
 *   <li>US-05: Verifying session full status before waitlist join, and reserving slot during promotion.</li>
 * </ul>
 *
 * <p><b>Business Rules:</b>
 * <ul>
 *   <li>MF03-BR-03: Atomic capacity allocation preventing two concurrent users from taking the last place.</li>
 *   <li>MF03-BR-04: 24-hour notice window checking via slot start timestamp.</li>
 *   <li>MF03-BR-05: Atomic release and reservation during rescheduling.</li>
 *   <li>MF03-BR-06: Verifies all session slots are full before permitting waitlist join.</li>
 * </ul>
 *
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
     * Must execute concurrency-safe capacity inspection within the caller's transaction by acquiring
     * pessimistic locks on the session's time slots.
     *
     * @param sessionId Identifier of the vaccination session
     * @return {@code true} if the session is scheduled and has zero remaining capacity across all time slots;
     *         {@code false} if at least one time slot has available booking capacity
     * @throws IllegalArgumentException if {@code sessionId} is {@code null} or does not exist
     * @throws IllegalStateException if the session is not in {@code SCHEDULED} status or has no configured time slots
     */
    boolean isSessionFull(Long sessionId);

    /**
     * Checks whether the specified time slot belongs to the given campaign and is in an operational scheduled state.
     *
     * @param slotId     Identifier of the time slot
     * @param campaignId Identifier of the vaccination campaign
     * @return {@code true} if the slot exists and belongs to a scheduled session for the specified campaign;
     *         {@code false} otherwise
     */
    boolean isSlotInCampaign(Long slotId, Long campaignId);

    /**
     * Resolves the authoritative session ID for the specified time slot (MF03-BR-07 / F04).
     *
     * @param slotId Identifier of the time slot
     * @return {@link Optional} containing the session ID if the slot exists and belongs to a scheduled session;
     *         empty otherwise
     */
    Optional<Long> getSessionIdForSlot(Long slotId);

    /**
     * Resolves the authoritative campaign ID for the specified session (R01).
     * Used to validate that a waitlist admission's campaign/session pair is authoritative
     * instead of trusting two independently supplied IDs.
     *
     * @param sessionId Identifier of the vaccination session
     * @return {@link Optional} containing the campaign ID if the session exists and belongs to a
     *         scheduled campaign relationship; empty otherwise
     */
    Optional<Long> getCampaignIdForSession(Long sessionId);

    /**
     * Authoritatively evaluates whether a NEW direct booking may currently be made into the slot
     * (R03). Enforces the full booking policy: session lifecycle, campaign booking openness,
     * session booking suspension, session booking cutoff, future slot time, and remaining capacity.
     * Must be enforced at every reservation mutation boundary — the availability/search path alone
     * cannot guarantee mutation safety.
     *
     * @param slotId Identifier of the time slot
     * @param now    Current evaluation instant
     * @return {@link SlotBookability#BOOKABLE} if a new booking is permitted; the specific failure
     *         reason otherwise
     */
    SlotBookability evaluateBookabilityForNewBooking(Long slotId, Instant now);

    /**
     * Authoritatively evaluates whether a WAITLIST PROMOTION may currently reserve the slot (R03).
     * Promotion fulfils an already-permitted queue join, so it deliberately does NOT re-apply the
     * direct-booking restrictions that would strand legitimate waiters: the session booking cutoff,
     * session booking suspension, and future-slot-time rules are not enforced here. Freed capacity
     * from cancellations and no-shows (which occur at or after slot start, past the cutoff) must
     * remain reachable by the FIFO queue. What IS enforced: slot existence, session lifecycle, and
     * campaign intake status (operationally closed campaigns reject promotion).
     *
     * @param slotId Identifier of the time slot
     * @param now    Current evaluation instant
     * @return {@link SlotBookability#BOOKABLE} if promotion may reserve the slot; the specific
     *         failure reason otherwise
     */
    SlotBookability evaluateBookabilityForWaitlistPromotion(Long slotId, Instant now);

    /**
     * Atomically validates bookability under lock and reserves one place of capacity for a new booking (F1).
     * Enforces session lock, authoritative bookability, and capacity reservation inside the transaction to
     * prevent TOCTOU races.
     *
     * @param slotId Identifier of the time slot
     * @param now    Current evaluation instant
     * @return {@link SlotReservationResult#SUCCESS} if reserved;
     *         {@link SlotReservationResult#NOT_BOOKABLE} if not currently bookable;
     *         {@link SlotReservationResult#SLOT_FULL} if capacity exhausted;
     *         {@link SlotReservationResult#SLOT_NOT_FOUND} if slot/session does not exist
     */
    SlotReservationResult reserveCapacityForNewBooking(Long slotId, Instant now);

    /**
     * Atomically validates bookability under lock and reserves one place of capacity for waitlist promotion (F1).
     *
     * @param slotId Identifier of the time slot
     * @param now    Current evaluation instant
     * @return {@link SlotReservationResult#SUCCESS} if reserved;
     *         {@link SlotReservationResult#NOT_BOOKABLE} if not bookable for promotion;
     *         {@link SlotReservationResult#SLOT_FULL} if capacity exhausted;
     *         {@link SlotReservationResult#SLOT_NOT_FOUND} if slot/session does not exist
     */
    SlotReservationResult reserveCapacityForWaitlistPromotion(Long slotId, Instant now);
}
