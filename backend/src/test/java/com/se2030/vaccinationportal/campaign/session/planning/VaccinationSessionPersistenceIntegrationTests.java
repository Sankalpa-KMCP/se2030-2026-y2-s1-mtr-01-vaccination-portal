package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.session.recurrence.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationSessionPersistenceIntegrationTests {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant BOOKING_CLOSE = Instant.parse("2026-10-15T08:00:00Z");
    private static final LocalDate ORIGIN_DATE = LocalDate.of(2026, 10, 15);

    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private SessionRecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @PersistenceContext private EntityManager entityManager;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private SessionRecurrenceRule recurrenceRule;

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-SESS-P", "Session Persistence Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-SESS-P", "Session Persistence Group"));
        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SESS-P", "Session Persistence Campaign", null,
                vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT));
        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-SESS-P", "Session Persistence Center", "1 Persistence Way", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "Asia/Colombo"));
        recurrenceRule = recurrenceRuleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, center, Set.of(DayOfWeek.THURSDAY),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "Asia/Colombo"));
    }

    @Test
    void persistsOneOffSessionAndReloads() {
        VaccinationSession oneOff = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo"));
        Long id = oneOff.getId();
        assertNotNull(id);
        entityManager.clear();

        VaccinationSession reloaded = sessionRepository.findById(id).orElseThrow();
        assertEquals(campaign.getId(), reloaded.getCampaign().getId());
        assertEquals(center.getId(), reloaded.getCenter().getId());
        assertNull(reloaded.getRecurrenceRule());
        assertNull(reloaded.getOccurrenceKey());
        assertNull(reloaded.getOriginOccurrenceDate());
        assertFalse(reloaded.isGenerated());
        assertEquals(START, reloaded.getStartAt());
        assertEquals(END, reloaded.getEndAt());
        assertEquals(BOOKING_CLOSE, reloaded.getBookingCloseAt());
        assertEquals("Asia/Colombo", reloaded.getScheduleTimezone());
        assertEquals(SessionLifecycleStatus.DRAFT, reloaded.getLifecycleStatus());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());
    }

    @Test
    void multipleOneOffSessionsWithNullOccurrenceKeyCanBePersisted() {
        VaccinationSession session1 = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, START, END, BOOKING_CLOSE, "Asia/Colombo"));
        VaccinationSession session2 = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, START.plusSeconds(3600), END.plusSeconds(3600),
                BOOKING_CLOSE.plusSeconds(3600), "Asia/Colombo"));

        assertNotNull(session1.getId());
        assertNotNull(session2.getId());
        assertNull(session1.getOccurrenceKey());
        assertNull(session2.getOccurrenceKey());
    }

    @Test
    void persistsGeneratedOccurrenceAndReloadsWithProvenance() {
        String key = "REC-P-UUID-20261015";
        VaccinationSession generated = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, recurrenceRule, key, ORIGIN_DATE,
                START, END, BOOKING_CLOSE, "Asia/Colombo"));
        Long id = generated.getId();
        entityManager.clear();

        VaccinationSession reloaded = sessionRepository.findById(id).orElseThrow();
        assertEquals(campaign.getId(), reloaded.getCampaign().getId());
        assertEquals(center.getId(), reloaded.getCenter().getId());
        assertNotNull(reloaded.getRecurrenceRule());
        assertEquals(recurrenceRule.getId(), reloaded.getRecurrenceRule().getId());
        assertEquals(key, reloaded.getOccurrenceKey());
        assertEquals(ORIGIN_DATE, reloaded.getOriginOccurrenceDate());
        assertTrue(reloaded.isGenerated());

        VaccinationSession byKey = sessionRepository.findByOccurrenceKey(key).orElseThrow();
        assertEquals(id, byKey.getId());
    }

    @Test
    void duplicateNonNullOccurrenceKeyIsRejectedByDatabaseConstraint() {
        String duplicateKey = "REC-DUP-KEY-20261015";
        sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, recurrenceRule, duplicateKey, ORIGIN_DATE,
                START, END, BOOKING_CLOSE, "Asia/Colombo"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            sessionRepository.saveAndFlush(new VaccinationSession(
                    campaign, center, recurrenceRule, duplicateKey, ORIGIN_DATE.plusDays(7),
                    START.plusSeconds(86400 * 7), END.plusSeconds(86400 * 7),
                    BOOKING_CLOSE.plusSeconds(86400 * 7), "Asia/Colombo"));
        });
    }

    @Test
    void differentNonNullOccurrenceKeysAreAccepted() {
        VaccinationSession s1 = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, recurrenceRule, "KEY-OCC-1", ORIGIN_DATE,
                START, END, BOOKING_CLOSE, "Asia/Colombo"));
        VaccinationSession s2 = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, recurrenceRule, "KEY-OCC-2", ORIGIN_DATE.plusDays(7),
                START.plusSeconds(86400 * 7), END.plusSeconds(86400 * 7),
                BOOKING_CLOSE.plusSeconds(86400 * 7), "Asia/Colombo"));

        assertNotNull(s1.getId());
        assertNotNull(s2.getId());
        assertTrue(sessionRepository.findByOccurrenceKey("KEY-OCC-1").isPresent());
        assertTrue(sessionRepository.findByOccurrenceKey("KEY-OCC-2").isPresent());
    }

    @Test
    void parentsAreNotDeletedWhenSessionIsDeleted() {
        VaccinationSession session = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, recurrenceRule, "KEY-CASCADE-CHECK", ORIGIN_DATE,
                START, END, BOOKING_CLOSE, "Asia/Colombo"));
        Long sessionId = session.getId();
        Long campaignId = campaign.getId();
        Long centerId = center.getId();
        Long ruleId = recurrenceRule.getId();

        sessionRepository.deleteById(sessionId);
        sessionRepository.flush();
        entityManager.clear();

        assertFalse(sessionRepository.findById(sessionId).isPresent());
        assertTrue(campaignRepository.findById(campaignId).isPresent());
        assertTrue(centerRepository.findById(centerId).isPresent());
        assertTrue(recurrenceRuleRepository.findById(ruleId).isPresent());
    }
}
