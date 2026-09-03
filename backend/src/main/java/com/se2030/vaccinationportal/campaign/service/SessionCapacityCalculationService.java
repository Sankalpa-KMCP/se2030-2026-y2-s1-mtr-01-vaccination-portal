package com.se2030.vaccinationportal.campaign.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

/**
 * Pure calculation engine for session throughput, effective station bundles, and concurrent occupancy.
 * Stateless with zero database or repository dependencies.
 * Canonical Section 6.4, MF02-BR-10, MF02-BR-21, MF02-BR-22.
 */
@Service
public class SessionCapacityCalculationService {

    /**
     * Calculates the effective staffed station count for a session service window.
     * Canonical: {@code effective_station_count = min(assigned_vaccinator_count, available_vaccination_station_count, fully_equipped_station_count)}.
     */
    public int calculateEffectiveStationCount(int assignedVaccinators,
                                             int availableStations,
                                             int fullyEquippedStations) {
        if (assignedVaccinators < 0) {
            throw new IllegalArgumentException("Assigned vaccinators must be non-negative");
        }
        if (availableStations < 0) {
            throw new IllegalArgumentException("Available stations must be non-negative");
        }
        if (fullyEquippedStations < 0) {
            throw new IllegalArgumentException("Fully equipped stations must be non-negative");
        }
        if (fullyEquippedStations > availableStations) {
            throw new IllegalArgumentException("Fully equipped stations (" + fullyEquippedStations
                    + ") cannot exceed available stations (" + availableStations + ")");
        }
        return Math.min(assignedVaccinators, Math.min(availableStations, fullyEquippedStations));
    }

    /**
     * Calculates the recommended total session throughput.
     * Canonical: {@code recommended_throughput = effective_station_count * floor(duration_minutes / expected_service_minutes)}.
     */
    public int calculateRecommendedThroughput(int effectiveStations,
                                             long durationMinutes,
                                             int expectedServiceMinutes) {
        if (effectiveStations < 0) {
            throw new IllegalArgumentException("Effective stations must be non-negative");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Session duration minutes must be positive");
        }
        if (expectedServiceMinutes <= 0) {
            throw new IllegalArgumentException("Expected service minutes per citizen must be positive");
        }
        long throughput = effectiveStations * (durationMinutes / expectedServiceMinutes);
        if (throughput > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) throughput;
    }

    /**
     * Calculates expected concurrent occupancy units for a given booking capacity and accompanying ratio.
     * Canonical: {@code capacity * (1 + expected_accompanying_persons_per_booking)}.
     */
    public double calculateExpectedOccupancyUnits(int capacity, double accompanyingRatio) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be non-negative");
        }
        if (accompanyingRatio < 0.0 || accompanyingRatio > 1.0) {
            throw new IllegalArgumentException("Accompanying ratio must be between 0.0 and 1.0");
        }
        return capacity * (1.0 + accompanyingRatio);
    }

    /**
     * Maps time slots and capacity plan parameters to occupancy intervals.
     */
    public List<OccupancyInterval> mapSlotsToOccupancyIntervals(Collection<com.se2030.vaccinationportal.campaign.model.TimeSlot> slots,
                                                               int expectedDwellMinutes,
                                                               double accompanyingRatio) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        return slots.stream()
                .map(s -> new OccupancyInterval(
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getCapacity(),
                        expectedDwellMinutes,
                        accompanyingRatio
                ))
                .toList();
    }

    /**
     * Evaluates concurrent occupancy across a collection of intervals at every start/dwell boundary.
     * Half-open interval semantics {@code [startAt, max(endAt, startAt + expectedDwellMinutes))}.
     * Canonical Section 6.4 and MF02-BR-22.
     */
    public OccupancyEvaluationResult evaluateConcurrentOccupancy(Collection<OccupancyInterval> intervals,
                                                                int safeConcurrentOccupancy) {
        if (safeConcurrentOccupancy <= 0) {
            throw new IllegalArgumentException("Safe concurrent occupancy must be positive");
        }
        if (intervals == null || intervals.isEmpty()) {
            return OccupancyEvaluationResult.safe(0.0, safeConcurrentOccupancy, null);
        }

        // Collect all distinct discrete boundary points
        TreeSet<Instant> boundaries = new TreeSet<>();
        for (OccupancyInterval interval : intervals) {
            boundaries.add(interval.startAt());
            boundaries.add(interval.getEffectiveDwellEnd());
        }

        double peakOccupancy = 0.0;
        Instant peakStart = null;

        for (Instant boundary : boundaries) {
            double currentOccupancy = 0.0;
            for (OccupancyInterval interval : intervals) {
                // Half-open interval check: [startAt, dwellEnd)
                if (!boundary.isBefore(interval.startAt()) && boundary.isBefore(interval.getEffectiveDwellEnd())) {
                    currentOccupancy += interval.getExpectedOccupancyUnits();
                }
            }

            if (currentOccupancy > peakOccupancy) {
                peakOccupancy = currentOccupancy;
                peakStart = boundary;
            }
        }

        if (peakOccupancy > safeConcurrentOccupancy) {
            return OccupancyEvaluationResult.exceeded(peakOccupancy, safeConcurrentOccupancy, peakStart);
        }

        return OccupancyEvaluationResult.safe(peakOccupancy, safeConcurrentOccupancy, peakStart);
    }

    /**
     * Resolves the effective runtime safe concurrent occupancy as the minimum of the plan's
     * conservative snapshot (if specified) and the center's current authoritative safety limit.
     * Canonical MF02-M01 / MF02-H03 runtime safety boundary.
     */
    public int resolveEffectiveSafeConcurrentOccupancy(Integer planSafeOccupancy, int centerSafeLimit) {
        if (centerSafeLimit <= 0) {
            throw new IllegalArgumentException("Center safe concurrent occupancy limit must be positive");
        }
        if (planSafeOccupancy == null || planSafeOccupancy <= 0) {
            return centerSafeLimit;
        }
        return Math.min(planSafeOccupancy, centerSafeLimit);
    }

    /**
     * Calculates the minimum number of distinct qualifying identities concurrently available
     * across the entire session window {@code [sessionStart, sessionEnd)}.
     * Uses half-open segment evaluation {@code [boundary_i, boundary_{i+1})}.
     */
    public <T, K> int calculateMinimumConcurrentIdentities(
            Instant sessionStart,
            Instant sessionEnd,
            Collection<T> items,
            java.util.function.Function<T, K> identityExtractor,
            java.util.function.Function<T, Instant> startExtractor,
            java.util.function.Function<T, Instant> endExtractor) {

        if (sessionStart == null || sessionEnd == null || !sessionStart.isBefore(sessionEnd)) {
            return 0;
        }
        if (items == null || items.isEmpty()) {
            return 0;
        }

        record ClampedItem<K>(K identity, Instant start, Instant end) {}
        List<ClampedItem<K>> clampedItems = new java.util.ArrayList<>();
        TreeSet<Instant> boundaries = new TreeSet<>();
        boundaries.add(sessionStart);
        boundaries.add(sessionEnd);

        for (T item : items) {
            K identity = identityExtractor.apply(item);
            Instant rawStart = startExtractor.apply(item);
            Instant rawEnd = endExtractor.apply(item);

            if (identity == null || rawStart == null || rawEnd == null || !rawStart.isBefore(rawEnd)) {
                continue;
            }

            Instant clampedStart = rawStart.isBefore(sessionStart) ? sessionStart : rawStart;
            Instant clampedEnd = rawEnd.isAfter(sessionEnd) ? sessionEnd : rawEnd;

            if (clampedStart.isBefore(clampedEnd)) {
                clampedItems.add(new ClampedItem<>(identity, clampedStart, clampedEnd));
                boundaries.add(clampedStart);
                boundaries.add(clampedEnd);
            }
        }

        if (clampedItems.isEmpty()) {
            return 0;
        }

        List<Instant> sortedBoundaries = new java.util.ArrayList<>(boundaries);
        int minConcurrent = Integer.MAX_VALUE;

        for (int i = 0; i < sortedBoundaries.size() - 1; i++) {
            Instant segStart = sortedBoundaries.get(i);
            Instant segEnd = sortedBoundaries.get(i + 1);
            if (!segStart.isBefore(segEnd)) {
                continue;
            }

            java.util.Set<K> distinctInSegment = new java.util.HashSet<>();
            for (ClampedItem<K> item : clampedItems) {
                if (!item.start().isAfter(segStart) && !item.end().isBefore(segEnd)) {
                    distinctInSegment.add(item.identity());
                }
            }

            minConcurrent = Math.min(minConcurrent, distinctInSegment.size());
            if (minConcurrent == 0) {
                return 0;
            }
        }

        return minConcurrent == Integer.MAX_VALUE ? 0 : minConcurrent;
    }
}
