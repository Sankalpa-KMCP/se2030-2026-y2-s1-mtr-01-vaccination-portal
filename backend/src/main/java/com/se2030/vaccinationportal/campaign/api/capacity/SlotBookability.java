package com.se2030.vaccinationportal.campaign.api.capacity;

/**
 * Authoritative bookability verdict computed by MF-02 for a time slot at a given instant.
 * Mirrors the search/availability policy (MF03-BR-03, MF03-BR-04, MF02-BR-03) so that
 * reservation mutations enforce the same restrictions the availability endpoint advertises.
 */
public enum SlotBookability {
    /** The slot may accept a new reservation under the authoritative booking policy. */
    BOOKABLE,

    /** The slot or its session/campaign chain could not be resolved. */
    SLOT_NOT_FOUND,

    /** The owning session is not in SCHEDULED lifecycle status. */
    SESSION_NOT_SCHEDULED,

    /** The campaign does not currently accept bookings (status or booking window). */
    CAMPAIGN_BOOKING_NOT_OPEN,

    /** Booking is operationally suspended for the owning session. */
    SESSION_BOOKING_SUSPENDED,

    /** The session's booking cutoff timestamp has passed. */
    SESSION_BOOKING_CLOSED,

    /** The slot start time is not in the future relative to the evaluation instant. */
    SLOT_IN_PAST,

    /** The slot has no remaining bookable capacity. */
    NO_CAPACITY
}
