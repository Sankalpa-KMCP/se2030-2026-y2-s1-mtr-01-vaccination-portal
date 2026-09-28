package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotBookability;
import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for MF-01/MF-02 until their real port implementations exist.
 * Contains no real business logic — do not reuse in production code.
 */
public class TestSlotCapacityPortDouble implements SlotCapacityPort {

    private final ConcurrentMap<Long, AtomicInteger> slotCapacityMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Instant> slotStartTimeMap = new ConcurrentHashMap<>();
    private boolean simulateReleaseFailure = false;

    public void setAvailableCapacity(Long slotId, int capacity) {
        slotCapacityMap.put(slotId, new AtomicInteger(capacity));
    }

    public void setSlotStartTime(Long slotId, Instant startTime) {
        slotStartTimeMap.put(slotId, startTime);
    }

    public void setSimulateReleaseFailure(boolean simulateReleaseFailure) {
        this.simulateReleaseFailure = simulateReleaseFailure;
    }

    @Override
    public boolean hasAvailableCapacity(Long slotId) {
        AtomicInteger remaining = slotCapacityMap.get(slotId);
        return remaining != null && remaining.get() > 0;
    }

    @Override
    public SlotReservationResult reserveCapacity(Long slotId) {
        AtomicInteger remaining = slotCapacityMap.get(slotId);
        if (remaining == null) {
            return SlotReservationResult.SLOT_NOT_FOUND;
        }

        while (true) {
            int current = remaining.get();
            if (current <= 0) {
                return SlotReservationResult.SLOT_FULL;
            }
            if (remaining.compareAndSet(current, current - 1)) {
                return SlotReservationResult.SUCCESS;
            }
        }
    }

    @Override
    public boolean releaseCapacity(Long slotId) {
        if (simulateReleaseFailure) {
            return false;
        }
        AtomicInteger remaining = slotCapacityMap.get(slotId);
        if (remaining == null) {
            return false;
        }
        remaining.incrementAndGet();
        return true;
    }

    @Override
    public Instant getSlotStartTime(Long slotId) {
        return slotStartTimeMap.get(slotId);
    }

    private final ConcurrentMap<Long, Boolean> sessionFullMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Long> slotCampaignMap = new ConcurrentHashMap<>();
    private boolean defaultSlotCampaignMatch = true;

    public void setSessionFull(Long sessionId, boolean full) {
        sessionFullMap.put(sessionId, full);
    }

    public void setSlotCampaign(Long slotId, Long campaignId) {
        if (slotId != null && campaignId != null) {
            slotCampaignMap.put(slotId, campaignId);
        }
    }

    public void setDefaultSlotCampaignMatch(boolean defaultSlotCampaignMatch) {
        this.defaultSlotCampaignMatch = defaultSlotCampaignMatch;
    }

    public void clearSlotCampaignMap() {
        slotCampaignMap.clear();
    }

    public int getAvailableCapacity(Long slotId) {
        AtomicInteger remaining = slotCapacityMap.get(slotId);
        return remaining != null ? remaining.get() : 0;
    }

    @Override
    public boolean isSessionFull(Long sessionId) {
        return sessionFullMap.getOrDefault(sessionId, true);
    }

    @Override
    public boolean isSlotInCampaign(Long slotId, Long campaignId) {
        if (slotId == null || campaignId == null) {
            return false;
        }
        Long mappedCampaign = slotCampaignMap.get(slotId);
        if (mappedCampaign != null) {
            return mappedCampaign.equals(campaignId);
        }
        return defaultSlotCampaignMatch;
    }

    private final ConcurrentMap<Long, Long> slotSessionMap = new ConcurrentHashMap<>();
    private Long defaultSessionId = null;

    public void setSlotSession(Long slotId, Long sessionId) {
        if (slotId != null && sessionId != null) {
            slotSessionMap.put(slotId, sessionId);
        }
    }

    public void setDefaultSessionId(Long defaultSessionId) {
        this.defaultSessionId = defaultSessionId;
    }

    public void clearSlotSessionMap() {
        slotSessionMap.clear();
    }

    @Override
    public Optional<Long> getSessionIdForSlot(Long slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        Long mappedSession = slotSessionMap.get(slotId);
        if (mappedSession != null) {
            return Optional.of(mappedSession);
        }
        if (defaultSessionId != null) {
            return Optional.of(defaultSessionId);
        }
        return Optional.empty();
    }

    private final ConcurrentMap<Long, Long> sessionCampaignMap = new ConcurrentHashMap<>();
    private Long defaultSessionCampaignId = null;
    private final ConcurrentMap<Long, SlotBookability> newBookingBookabilityOverrides = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SlotBookability> promotionBookabilityOverrides = new ConcurrentHashMap<>();

    /** Maps a session to its authoritative campaign (R01 admission/promotion validation). */
    public void setSessionCampaign(Long sessionId, Long campaignId) {
        if (sessionId != null && campaignId != null) {
            sessionCampaignMap.put(sessionId, campaignId);
        }
    }

    public void setDefaultSessionCampaignId(Long defaultSessionCampaignId) {
        this.defaultSessionCampaignId = defaultSessionCampaignId;
    }

    public void clearSessionCampaignMap() {
        sessionCampaignMap.clear();
    }

    /** Forces the new-booking bookability verdict for a slot (R03 rejection scenarios). */
    public void setNewBookingBookability(Long slotId, SlotBookability bookability) {
        if (slotId != null) {
            newBookingBookabilityOverrides.put(slotId, bookability);
        }
    }

    /** Forces the waitlist-promotion bookability verdict for a slot (R03 rejection scenarios). */
    public void setPromotionBookability(Long slotId, SlotBookability bookability) {
        if (slotId != null) {
            promotionBookabilityOverrides.put(slotId, bookability);
        }
    }

    public void clearBookabilityOverrides() {
        newBookingBookabilityOverrides.clear();
        promotionBookabilityOverrides.clear();
    }

    @Override
    public Optional<Long> getCampaignIdForSession(Long sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        Long mappedCampaign = sessionCampaignMap.get(sessionId);
        if (mappedCampaign != null) {
            return Optional.of(mappedCampaign);
        }
        if (defaultSessionCampaignId != null) {
            return Optional.of(defaultSessionCampaignId);
        }
        return Optional.empty();
    }

    @Override
    public SlotBookability evaluateBookabilityForNewBooking(Long slotId, Instant now) {
        SlotBookability override = newBookingBookabilityOverrides.get(slotId);
        return override != null ? override : SlotBookability.BOOKABLE;
    }

    @Override
    public SlotBookability evaluateBookabilityForWaitlistPromotion(Long slotId, Instant now) {
        SlotBookability override = promotionBookabilityOverrides.get(slotId);
        return override != null ? override : SlotBookability.BOOKABLE;
    }

    @Override
    public SlotReservationResult reserveCapacityForNewBooking(Long slotId, Instant now) {
        SlotBookability bookability = evaluateBookabilityForNewBooking(slotId, now);
        if (bookability != SlotBookability.BOOKABLE) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        return reserveCapacity(slotId);
    }

    @Override
    public SlotReservationResult reserveCapacityForWaitlistPromotion(Long slotId, Instant now) {
        SlotBookability bookability = evaluateBookabilityForWaitlistPromotion(slotId, now);
        if (bookability != SlotBookability.BOOKABLE) {
            return SlotReservationResult.NOT_BOOKABLE;
        }
        return reserveCapacity(slotId);
    }
}
