package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.SlotCapacityPort;
import com.se2030.vaccinationportal.appointment.dependency.SlotReservationResult;
import java.time.Instant;
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
}
