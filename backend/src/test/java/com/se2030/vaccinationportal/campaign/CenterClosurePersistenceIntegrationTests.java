package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CenterClosurePersistenceIntegrationTests {

    private static final Instant START = Instant.parse("2026-09-15T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-15T12:00:00Z");

    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        centerA = saveCenter("CTR-CLOSURE-PA");
        centerB = saveCenter("CTR-CLOSURE-PB");
    }

    @Test
    void persistsOwnerStatusAndAuditTimestamps() {
        CenterClosure saved = closureRepository.saveAndFlush(
                new CenterClosure(centerA, START, END, "Maintenance")
        );

        CenterClosure reloaded = closureRepository.findById(saved.getId()).orElseThrow();
        assertEquals(centerA.getId(), reloaded.getCenter().getId());
        assertEquals(CenterClosureStatus.ACTIVE, reloaded.getStatus());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());
        assertEquals(centerA.getId(), closureRepository.findCenterIdById(saved.getId()).orElseThrow());
    }

    @Test
    void databaseRequiresExistingCenterRelationship() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO center_closures " +
                        "(center_id, start_at, end_at, reason, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                999999L, START, END, "Missing center", "ACTIVE", START, START
        ));
    }

    @Test
    void centerHistoryIsDeterministicAndIsolated() {
        CenterClosure later = closureRepository.save(new CenterClosure(
                centerA,
                Instant.parse("2026-09-16T08:00:00Z"),
                Instant.parse("2026-09-16T12:00:00Z"),
                "Later"
        ));
        CenterClosure earlierLong = closureRepository.save(new CenterClosure(
                centerA,
                Instant.parse("2026-09-15T08:00:00Z"),
                Instant.parse("2026-09-15T13:00:00Z"),
                "Earlier long"
        ));
        CenterClosure earlierShort = closureRepository.save(new CenterClosure(
                centerA,
                Instant.parse("2026-09-15T08:00:00Z"),
                Instant.parse("2026-09-15T12:00:00Z"),
                "Earlier short"
        ));
        closureRepository.save(new CenterClosure(centerB, START, END, "Other center"));
        closureRepository.flush();

        List<CenterClosure> history = closureRepository.findByCenterIdOrderByStartAtAscEndAtAscIdAsc(centerA.getId());

        assertEquals(List.of(earlierShort.getId(), earlierLong.getId(), later.getId()),
                history.stream().map(CenterClosure::getId).toList());
    }

    @Test
    void overlapQueryUsesExactHalfOpenSemantics() {
        closureRepository.saveAndFlush(new CenterClosure(centerA, START, END, "Existing"));

        List<Instant[]> overlapping = List.of(
                new Instant[]{START, END},
                new Instant[]{Instant.parse("2026-09-15T07:00:00Z"), Instant.parse("2026-09-15T09:00:00Z")},
                new Instant[]{Instant.parse("2026-09-15T11:00:00Z"), Instant.parse("2026-09-15T13:00:00Z")},
                new Instant[]{Instant.parse("2026-09-15T07:00:00Z"), Instant.parse("2026-09-15T13:00:00Z")},
                new Instant[]{Instant.parse("2026-09-15T09:00:00Z"), Instant.parse("2026-09-15T11:00:00Z")},
                new Instant[]{START, Instant.parse("2026-09-15T10:00:00Z")},
                new Instant[]{Instant.parse("2026-09-15T10:00:00Z"), END}
        );

        for (Instant[] candidate : overlapping) {
            assertTrue(hasOverlap(centerA, null, candidate[0], candidate[1]),
                    () -> "Expected overlap for " + candidate[0] + " to " + candidate[1]);
        }

        assertFalse(hasOverlap(centerA, null, Instant.parse("2026-09-15T06:00:00Z"), START));
        assertFalse(hasOverlap(centerA, null, END, Instant.parse("2026-09-15T14:00:00Z")));
        assertFalse(hasOverlap(centerB, null, START, END));
    }

    @Test
    void cancelledClosureIsIgnoredByActiveOverlapQuery() {
        CenterClosure cancelled = new CenterClosure(centerA, START, END, "Cancelled");
        cancelled.cancel();
        closureRepository.saveAndFlush(cancelled);

        assertFalse(hasOverlap(centerA, null, START, END));
    }

    @Test
    void exclusionIgnoresSelfButStillFindsAnotherSibling() {
        CenterClosure first = closureRepository.save(new CenterClosure(centerA, START, END, "First"));
        CenterClosure second = closureRepository.save(new CenterClosure(
                centerA,
                Instant.parse("2026-09-15T13:00:00Z"),
                Instant.parse("2026-09-15T17:00:00Z"),
                "Second"
        ));
        closureRepository.flush();

        assertFalse(hasOverlap(centerA, first.getId(), START, END));
        assertTrue(hasOverlap(
                centerA,
                first.getId(),
                Instant.parse("2026-09-15T12:30:00Z"),
                Instant.parse("2026-09-15T14:00:00Z")
        ));
        assertTrue(closureRepository.findById(second.getId()).isPresent());
    }

    private boolean hasOverlap(VaccinationCenter center, Long excludeId, Instant startAt, Instant endAt) {
        return closureRepository.existsOverlappingClosure(
                center.getId(), CenterClosureStatus.ACTIVE, excludeId, startAt, endAt
        );
    }

    private VaccinationCenter saveCenter(String code) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                code, "Closure Persistence Center", "1 Health Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50
        ));
    }
}
