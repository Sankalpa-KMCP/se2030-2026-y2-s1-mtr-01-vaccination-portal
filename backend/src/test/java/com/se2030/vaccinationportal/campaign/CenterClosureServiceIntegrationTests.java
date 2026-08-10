package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.service.CenterClosureService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(CenterClosureServiceIntegrationTests.FixedClockConfig.class)
class CenterClosureServiceIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private CenterClosureService service;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccinationCenterRepository centerRepository;

    @ParameterizedTest
    @EnumSource(CenterStatus.class)
    void createsFutureClosureForEitherCenterLifecycleStatus(CenterStatus status) {
        VaccinationCenter center = center(status, "STATUS-" + status);

        CenterClosure closure = service.createClosure(
                center.getId(), future(2), future(4), "  Planned maintenance  ");

        assertNotNull(closure.getId());
        assertEquals("Planned maintenance", closure.getReason());
        assertEquals(CenterClosureStatus.ACTIVE, closure.getStatus());
        assertSame(center, closure.getCenter());
    }

    @Test
    void creationAllowsOngoingAndHistoricalClosures() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "IMPORT");

        service.createClosure(center.getId(), NOW.minusSeconds(3600), NOW.plusSeconds(3600), "Ongoing");
        service.createClosure(center.getId(), NOW.minusSeconds(10800), NOW.minusSeconds(7200), "Historical");

        assertEquals(2, service.getClosuresForCenter(center.getId()).size());
    }

    @Test
    void creationRejectsMissingCenterAndInvalidCandidate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createClosure(999_999L, future(1), future(2), "Missing"));

        VaccinationCenter center = center(CenterStatus.ACTIVE, "INVALID");
        assertThrows(IllegalArgumentException.class,
                () -> service.createClosure(center.getId(), future(2), future(2), "Invalid"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createClosure(center.getId(), future(2), future(3), "  "));
        assertThrows(IllegalArgumentException.class,
                () -> service.createClosure(center.getId(), future(2), future(3), "x".repeat(256)));
    }

    @ParameterizedTest
    @MethodSource("conflictingIntervals")
    void creationRejectsEveryActiveOverlapShape(Instant startAt, Instant endAt) {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "OVERLAP");
        service.createClosure(center.getId(), future(4), future(8), "Existing");

        assertThrows(IllegalArgumentException.class,
                () -> service.createClosure(center.getId(), startAt, endAt, "Candidate"));
    }

    static Stream<Arguments> conflictingIntervals() {
        return Stream.of(
                Arguments.of(future(4), future(8)),
                Arguments.of(future(3), future(6)),
                Arguments.of(future(6), future(9)),
                Arguments.of(future(5), future(7)),
                Arguments.of(future(3), future(9))
        );
    }

    @Test
    void creationAllowsAdjacencyAndSameIntervalOnAnotherCenter() {
        VaccinationCenter first = center(CenterStatus.ACTIVE, "FIRST");
        VaccinationCenter second = center(CenterStatus.ACTIVE, "SECOND");
        service.createClosure(first.getId(), future(4), future(8), "Existing");

        service.createClosure(first.getId(), future(2), future(4), "Before");
        service.createClosure(first.getId(), future(8), future(10), "After");
        service.createClosure(second.getId(), future(4), future(8), "Independent");

        assertEquals(3, service.getClosuresForCenter(first.getId()).size());
        assertEquals(1, service.getClosuresForCenter(second.getId()).size());
    }

    @Test
    void cancelledSiblingDoesNotBlockCreation() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "CANCELLED-SIBLING");
        CenterClosure cancelled = service.createClosure(center.getId(), future(4), future(8), "Original");
        service.cancelFutureClosure(cancelled.getId());

        CenterClosure replacement = service.createClosure(
                center.getId(), future(4), future(8), "Replacement");

        assertEquals(CenterClosureStatus.ACTIVE, replacement.getStatus());
        assertEquals(2, closureRepository.count());
    }

    @Test
    void futureUpdateCanChangeIntervalAndReason() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "UPDATE");
        CenterClosure closure = service.createClosure(center.getId(), future(3), future(5), "Original");

        CenterClosure updated = service.updateFutureClosure(
                closure.getId(), future(4), future(7), "  Revised  ");

        assertEquals(future(4), updated.getStartAt());
        assertEquals(future(7), updated.getEndAt());
        assertEquals("Revised", updated.getReason());
    }

    @Test
    void reasonOnlyFutureEditUsesNormalLifecycle() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "REASON-EDIT");
        CenterClosure closure = service.createClosure(center.getId(), future(3), future(5), "Original");

        service.updateFutureClosure(closure.getId(), closure.getStartAt(), closure.getEndAt(), "Revised");

        assertEquals("Revised", closure.getReason());
    }

    @Test
    void conflictAndInvalidUpdateLeaveExistingStateUnchanged() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "SAFE-UPDATE");
        CenterClosure target = service.createClosure(center.getId(), future(3), future(5), "Original");
        service.createClosure(center.getId(), future(7), future(9), "Sibling");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(target.getId(), future(6), future(8), "Conflict"));
        assertOriginal(target);
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(target.getId(), future(6), future(6), "Invalid"));
        assertOriginal(target);
    }

    @Test
    void updateRejectsCancelledMissingAndStartedTargets() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "UPDATE-RULES");
        CenterClosure cancelled = service.createClosure(center.getId(), future(2), future(3), "Cancelled");
        service.cancelFutureClosure(cancelled.getId());
        CenterClosure atNow = service.createClosure(center.getId(), NOW, future(1), "Started now");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(cancelled.getId(), future(4), future(5), "No"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(atNow.getId(), future(4), future(5), "No"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(999_999L, future(4), future(5), "Missing"));
    }

    @Test
    void updateRejectsOngoingAndNonfutureCandidateStart() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "UPDATE-TIME");
        CenterClosure ongoing = service.createClosure(
                center.getId(), NOW.minusSeconds(1), future(1), "Ongoing");
        CenterClosure future = service.createClosure(center.getId(), future(3), future(4), "Future");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(ongoing.getId(), future(5), future(6), "No"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(future.getId(), NOW, future(1), "No"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureClosure(future.getId(), NOW.minusSeconds(1), future(1), "No"));
    }

    @Test
    void futureCancellationIsTerminalRetainedAndPreservesDetails() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "CANCEL");
        CenterClosure closure = service.createClosure(center.getId(), future(3), future(5), "Original reason");
        Instant startAt = closure.getStartAt();
        Instant endAt = closure.getEndAt();

        CenterClosure cancelled = service.cancelFutureClosure(closure.getId());

        assertEquals(CenterClosureStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Original reason", cancelled.getReason());
        assertEquals(startAt, cancelled.getStartAt());
        assertEquals(endAt, cancelled.getEndAt());
        assertTrue(closureRepository.existsById(cancelled.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelFutureClosure(cancelled.getId()));
    }

    @Test
    void cancellationRejectsExactStartOngoingPastAndMissing() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "CANCEL-TIME");
        CenterClosure exact = service.createClosure(center.getId(), NOW, future(1), "Exact");
        CenterClosure past = service.createClosure(
                center.getId(), NOW.minusSeconds(7200), NOW.minusSeconds(3600), "Past");

        assertThrows(IllegalArgumentException.class, () -> service.cancelFutureClosure(exact.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.cancelFutureClosure(past.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.cancelFutureClosure(999_999L));
    }

    @Test
    void cancellationMakesIntervalAvailableForNewActiveClosure() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "REUSE");
        CenterClosure closure = service.createClosure(center.getId(), future(3), future(5), "Original");
        service.cancelFutureClosure(closure.getId());

        service.createClosure(center.getId(), future(3), future(5), "Replacement");

        assertEquals(2, service.getClosuresForCenter(center.getId()).size());
    }

    @Test
    void readsRetainDeterministicHistoricalAndCancelledHistory() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "READ");
        CenterClosure later = service.createClosure(center.getId(), future(5), future(6), "Later");
        CenterClosure historical = service.createClosure(
                center.getId(), NOW.minusSeconds(7200), NOW.minusSeconds(3600), "Historical");
        service.cancelFutureClosure(later.getId());

        List<CenterClosure> history = service.getClosuresForCenter(center.getId());

        assertEquals(List.of(historical.getId(), later.getId()),
                history.stream().map(CenterClosure::getId).toList());
        assertEquals(later.getId(), service.getClosureById(later.getId()).orElseThrow().getId());
        assertTrue(service.getClosureById(999_999L).isEmpty());
    }

    @Test
    void overlapHelperUsesHalfOpenActiveSemantics() {
        VaccinationCenter center = center(CenterStatus.ACTIVE, "HELPER");
        CenterClosure active = service.createClosure(center.getId(), future(3), future(5), "Active");

        assertTrue(service.hasActiveClosureOverlap(center.getId(), future(4), future(6)));
        assertFalse(service.hasActiveClosureOverlap(center.getId(), future(5), future(6)));
        service.cancelFutureClosure(active.getId());
        assertFalse(service.hasActiveClosureOverlap(center.getId(), future(4), future(6)));
        assertThrows(IllegalArgumentException.class,
                () -> service.hasActiveClosureOverlap(center.getId(), future(4), future(4)));
    }

    private VaccinationCenter center(CenterStatus status, String suffix) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-" + suffix,
                "Center " + suffix,
                "1 Test Road",
                "+94 11 000 0000",
                status,
                100,
                50
        ));
    }

    private void assertOriginal(CenterClosure closure) {
        assertEquals(future(3), closure.getStartAt());
        assertEquals(future(5), closure.getEndAt());
        assertEquals("Original", closure.getReason());
    }

    private static Instant future(long hours) {
        return NOW.plusSeconds(hours * 3600);
    }
}
