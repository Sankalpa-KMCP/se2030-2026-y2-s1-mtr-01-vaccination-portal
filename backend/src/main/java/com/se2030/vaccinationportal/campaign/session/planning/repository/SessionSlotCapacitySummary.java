package com.se2030.vaccinationportal.campaign.session.planning.repository;

/**
 * Lightweight aggregate projection representing the total slot count, total capacity,
 * and total reserved count for a vaccination session.
 */
public record SessionSlotCapacitySummary(
        Long sessionId,
        long slotCount,
        long totalCapacity,
        long totalReservedCount
) {
    public boolean hasAvailableCapacity() {
        return (totalCapacity - totalReservedCount) > 0;
    }
}
