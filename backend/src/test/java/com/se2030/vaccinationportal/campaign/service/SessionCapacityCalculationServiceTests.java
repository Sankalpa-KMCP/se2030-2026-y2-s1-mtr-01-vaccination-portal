package com.se2030.vaccinationportal.campaign.service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
class SessionCapacityCalculationServiceTests {

    private SessionCapacityCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new SessionCapacityCalculationService();
    }

    @Test
    void calculateEffectiveStationCount_minOfThreeInputs() {
        // Limited by vaccinators
        assertEquals(2, calculationService.calculateEffectiveStationCount(2, 5, 5));

        // Limited by available stations
        assertEquals(3, calculationService.calculateEffectiveStationCount(5, 3, 3));

        // Limited by fully equipped stations
        assertEquals(1, calculationService.calculateEffectiveStationCount(4, 4, 1));

        // All equal
        assertEquals(4, calculationService.calculateEffectiveStationCount(4, 4, 4));

        // Zero vaccinators
        assertEquals(0, calculationService.calculateEffectiveStationCount(0, 5, 5));
    }

    @Test
    void calculateEffectiveStationCount_rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateEffectiveStationCount(-1, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateEffectiveStationCount(5, -1, 5));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateEffectiveStationCount(5, 5, -1));

        // Fully equipped stations cannot exceed available stations
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> calculationService.calculateEffectiveStationCount(5, 3, 4));
        assertTrue(ex.getMessage().contains("cannot exceed available stations"));
    }

    @Test
    void calculateRecommendedThroughput_exactAndFloorCalculations() {
        // 4 stations, 120 minutes, 10 min per citizen -> 4 * (120/10) = 4 * 12 = 48
        assertEquals(48, calculationService.calculateRecommendedThroughput(4, 120, 10));

        // Floor division: 3 stations, 45 minutes, 10 min per citizen -> 3 * 4 = 12
        assertEquals(12, calculationService.calculateRecommendedThroughput(3, 45, 10));

        // Duration shorter than service minutes -> 0 throughput
        assertEquals(0, calculationService.calculateRecommendedThroughput(5, 8, 10));

        // 0 effective stations -> 0 throughput
        assertEquals(0, calculationService.calculateRecommendedThroughput(0, 120, 10));
    }

    @Test
    void calculateRecommendedThroughput_rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateRecommendedThroughput(-1, 120, 10));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateRecommendedThroughput(4, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateRecommendedThroughput(4, -60, 10));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateRecommendedThroughput(4, 120, 0));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateRecommendedThroughput(4, 120, -5));
    }

    @Test
    void calculateExpectedOccupancyUnits_withAndWithoutAccompanyingRatio() {
        // No accompanying persons: capacity 20 -> 20.0
        assertEquals(20.0, calculationService.calculateExpectedOccupancyUnits(20, 0.0), 0.0001);

        // 50% accompanying persons: capacity 20 -> 20 * 1.5 = 30.0
        assertEquals(30.0, calculationService.calculateExpectedOccupancyUnits(20, 0.5), 0.0001);

        // 100% accompanying persons: capacity 10 -> 10 * 2.0 = 20.0
        assertEquals(20.0, calculationService.calculateExpectedOccupancyUnits(10, 1.0), 0.0001);

        // Capacity 0 -> 0.0
        assertEquals(0.0, calculationService.calculateExpectedOccupancyUnits(0, 0.5), 0.0001);
    }

    @Test
    void calculateExpectedOccupancyUnits_rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateExpectedOccupancyUnits(-1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateExpectedOccupancyUnits(10, -0.1));
        assertThrows(IllegalArgumentException.class, () -> calculationService.calculateExpectedOccupancyUnits(10, 1.1));
    }

    @Test
    void evaluateConcurrentOccupancy_emptyIntervalsIsSafe() {
        OccupancyEvaluationResult result = calculationService.evaluateConcurrentOccupancy(List.of(), 50);
        assertTrue(result.safe());
        assertEquals(0.0, result.peakOccupancyUnits());
        assertEquals(50, result.safeConcurrentOccupancy());
        assertNull(result.peakIntervalStart());
    }

    @Test
    void evaluateConcurrentOccupancy_singleIntervalWithinLimit() {
        Instant start = Instant.parse("2026-10-01T09:00:00Z");
        Instant end = Instant.parse("2026-10-01T10:00:00Z");
        OccupancyInterval interval = new OccupancyInterval(start, end, 20, 15, 0.2); // 20 * 1.2 = 24.0

        OccupancyEvaluationResult result = calculationService.evaluateConcurrentOccupancy(List.of(interval), 30);
        assertTrue(result.safe());
        assertEquals(24.0, result.peakOccupancyUnits(), 0.0001);
        assertEquals(30, result.safeConcurrentOccupancy());
        assertEquals(start, result.peakIntervalStart());
    }

    @Test
    void evaluateConcurrentOccupancy_overlappingIntervalsExceedingLimit() {
        Instant t1 = Instant.parse("2026-10-01T09:00:00Z");
        Instant t2 = Instant.parse("2026-10-01T09:30:00Z");
        Instant t3 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t4 = Instant.parse("2026-10-01T10:30:00Z");

        // Interval 1: [09:00, 10:00), dwell 15 min -> effective [09:00, 10:00), cap 20 (20 units)
        OccupancyInterval i1 = new OccupancyInterval(t1, t3, 20, 15, 0.0);
        // Interval 2: [09:30, 10:30), dwell 15 min -> effective [09:30, 10:30), cap 15 (15 units)
        OccupancyInterval i2 = new OccupancyInterval(t2, t4, 15, 0, 0.0);

        // At 09:30, both overlap: 20 + 15 = 35 units. Limit is 30.
        OccupancyEvaluationResult result = calculationService.evaluateConcurrentOccupancy(List.of(i1, i2), 30);
        assertFalse(result.safe());
        assertEquals(35.0, result.peakOccupancyUnits(), 0.0001);
        assertEquals(30, result.safeConcurrentOccupancy());
        assertEquals(t2, result.peakIntervalStart());
    }

    @Test
    void evaluateConcurrentOccupancy_halfOpenBoundaryTouchingDoesNotOverlap() {
        Instant t1 = Instant.parse("2026-10-01T09:00:00Z");
        Instant t2 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t3 = Instant.parse("2026-10-01T11:00:00Z");

        // Interval 1: [09:00, 10:00), dwell 0 -> effective [09:00, 10:00), cap 25
        OccupancyInterval i1 = new OccupancyInterval(t1, t2, 25, 0, 0.0);
        // Interval 2: [10:00, 11:00), dwell 0 -> effective [10:00, 11:00), cap 25
        OccupancyInterval i2 = new OccupancyInterval(t2, t3, 25, 0, 0.0);

        // At boundary 10:00, i1 has ended ([09:00, 10:00) is half-open, excludes 10:00), i2 starts. Peak = 25. Limit = 30.
        OccupancyEvaluationResult result = calculationService.evaluateConcurrentOccupancy(List.of(i1, i2), 30);
        assertTrue(result.safe());
        assertEquals(25.0, result.peakOccupancyUnits(), 0.0001);
    }

    @Test
    void evaluateConcurrentOccupancy_dwellExtensionCausesOverlap() {
        Instant t1 = Instant.parse("2026-10-01T09:00:00Z");
        Instant t2 = Instant.parse("2026-10-01T09:30:00Z");
        Instant t3 = Instant.parse("2026-10-01T10:00:00Z");

        // Interval 1: [09:00, 09:30), dwell 45 min -> effective [09:00, 09:45), cap 20
        OccupancyInterval i1 = new OccupancyInterval(t1, t2, 20, 45, 0.0);
        // Interval 2: [09:30, 10:00), dwell 15 min -> effective [09:30, 10:00), cap 15
        OccupancyInterval i2 = new OccupancyInterval(t2, t3, 15, 15, 0.0);

        // Due to dwell extension on i1, both active at 09:30: 20 + 15 = 35. Safe limit 30 -> exceeded.
        OccupancyEvaluationResult result = calculationService.evaluateConcurrentOccupancy(List.of(i1, i2), 30);
        assertFalse(result.safe());
        assertEquals(35.0, result.peakOccupancyUnits(), 0.0001);
    }

    @Test
    void evaluateConcurrentOccupancy_rejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> calculationService.evaluateConcurrentOccupancy(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> calculationService.evaluateConcurrentOccupancy(List.of(), -10));
    }

    @Test
    void resolveEffectiveSafeConcurrentOccupancy_rules() {
        // null or non-positive plan safe occupancy -> uses center limit
        assertEquals(10, calculationService.resolveEffectiveSafeConcurrentOccupancy(null, 10));
        assertEquals(10, calculationService.resolveEffectiveSafeConcurrentOccupancy(0, 10));
        assertEquals(10, calculationService.resolveEffectiveSafeConcurrentOccupancy(-5, 10));

        // plan safe occupancy lower than center limit -> uses plan (conservative snapshot)
        assertEquals(8, calculationService.resolveEffectiveSafeConcurrentOccupancy(8, 10));

        // plan safe occupancy higher than center limit (e.g. center limit lowered) -> clamped to center limit
        assertEquals(5, calculationService.resolveEffectiveSafeConcurrentOccupancy(10, 5));

        // both equal -> returns value
        assertEquals(10, calculationService.resolveEffectiveSafeConcurrentOccupancy(10, 10));

        // Non-positive center limit -> throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> calculationService.resolveEffectiveSafeConcurrentOccupancy(8, 0));
        assertThrows(IllegalArgumentException.class, () -> calculationService.resolveEffectiveSafeConcurrentOccupancy(8, -1));
    }

    @Test
    void calculateMinimumConcurrentIdentities_intervalScenarios() {
        Instant t0900 = Instant.parse("2026-10-01T09:00:00Z");
        Instant t0930 = Instant.parse("2026-10-01T09:30:00Z");
        Instant t1000 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1100 = Instant.parse("2026-10-01T11:00:00Z");
        Instant t1200 = Instant.parse("2026-10-01T12:00:00Z");
        Instant t1230 = Instant.parse("2026-10-01T12:30:00Z");
        Instant t1300 = Instant.parse("2026-10-01T13:00:00Z");

        record Assignment(Long id, Instant start, Instant end) {}

        // Case A: 1 identity covering full session (09:00-13:00) -> 1
        List<Assignment> caseA = List.of(new Assignment(1L, t0900, t1300));
        assertEquals(1, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseA, Assignment::id, Assignment::start, Assignment::end));

        // Case B: 1 identity covering only part (09:00-10:00) -> 0
        List<Assignment> caseB = List.of(new Assignment(1L, t0900, t1000));
        assertEquals(0, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseB, Assignment::id, Assignment::start, Assignment::end));

        // Case C: Same identity with adjacent rows (09:00-11:00, 11:00-13:00) -> 1 (not 2)
        List<Assignment> caseC = List.of(
                new Assignment(1L, t0900, t1100),
                new Assignment(1L, t1100, t1300)
        );
        assertEquals(1, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseC, Assignment::id, Assignment::start, Assignment::end));

        // Case D: Distinct identities with adjacent rows (A: 09:00-11:00, B: 11:00-13:00) -> 1
        List<Assignment> caseD = List.of(
                new Assignment(1L, t0900, t1100),
                new Assignment(2L, t1100, t1300)
        );
        assertEquals(1, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseD, Assignment::id, Assignment::start, Assignment::end));

        // Case E: Overlapping staggered (A: 09:00-12:00, B: 10:00-13:00) -> min concurrent = 1
        List<Assignment> caseE = List.of(
                new Assignment(1L, t0900, t1200),
                new Assignment(2L, t1000, t1300)
        );
        assertEquals(1, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseE, Assignment::id, Assignment::start, Assignment::end));

        // Case F: Full coverage A (09:00-13:00) and partial coverage B (09:30-12:30) -> min concurrent = 1
        List<Assignment> caseF = List.of(
                new Assignment(1L, t0900, t1300),
                new Assignment(2L, t0930, t1230)
        );
        assertEquals(1, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseF, Assignment::id, Assignment::start, Assignment::end));

        // Case G: Two identities both covering full session (09:00-13:00) -> 2
        List<Assignment> caseG = List.of(
                new Assignment(1L, t0900, t1300),
                new Assignment(2L, t0900, t1300)
        );
        assertEquals(2, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseG, Assignment::id, Assignment::start, Assignment::end));

        // Case H: Same identity with duplicate overlapping rows (09:00-13:00 and 10:00-12:00) -> 1
        List<Assignment> caseH = List.of(
                new Assignment(1L, t0900, t1300),
                new Assignment(1L, t1000, t1200)
        );
        assertEquals(1, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, caseH, Assignment::id, Assignment::start, Assignment::end));

        // Empty / null handling
        assertEquals(0, calculationService.calculateMinimumConcurrentIdentities(
                t0900, t1300, List.of(), Assignment::id, Assignment::start, Assignment::end));
        assertEquals(0, calculationService.calculateMinimumConcurrentIdentities(
                null, t1300, caseA, Assignment::id, Assignment::start, Assignment::end));
    }
}
