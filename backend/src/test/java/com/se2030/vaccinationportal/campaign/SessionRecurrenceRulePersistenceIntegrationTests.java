package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
class SessionRecurrenceRulePersistenceIntegrationTests {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);

    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    private VaccinationCampaign campaignA;
    private VaccinationCampaign campaignB;
    private VaccinationCenter centerA;
    private VaccinationCenter centerB;

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-RULE-P", "Recurrence Persistence Vaccine"));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-RULE-P", "Recurrence Persistence Group"));
        campaignA = saveCampaign("CMP-RULE-PA", CampaignStatus.DRAFT, vaccine, group);
        campaignB = saveCampaign("CMP-RULE-PB", CampaignStatus.ACTIVE, vaccine, group);
        centerA = saveCenter("CTR-RULE-PA", CenterStatus.ACTIVE);
        centerB = saveCenter("CTR-RULE-PB", CenterStatus.INACTIVE);
    }

    @Test
    void persistsDefinitionWeekdaysStatusKeyAndAudit() {
        SessionRecurrenceRule saved = ruleRepository.saveAndFlush(rule(
                campaignA, centerA, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                MONDAY, MONDAY.plusWeeks(2), LocalTime.of(9, 0), LocalTime.of(12, 0), "Asia/Colombo"));
        String key = saved.getGenerationKey();
        entityManager.clear();

        SessionRecurrenceRule reloaded = ruleRepository.findById(saved.getId()).orElseThrow();

        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), reloaded.getWeekdays());
        assertEquals("Asia/Colombo", reloaded.getTimezone());
        assertEquals(SessionRecurrenceRuleStatus.ACTIVE, reloaded.getStatus());
        assertEquals(key, reloaded.getGenerationKey());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());
        assertEquals(reloaded.getId(), ruleRepository.findByGenerationKey(key).orElseThrow().getId());
    }

    @Test
    void parentsRemainLazyWhileEssentialWeekdaysAreLoaded() {
        SessionRecurrenceRule saved = ruleRepository.saveAndFlush(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "UTC"));
        Long id = saved.getId();
        entityManager.clear();

        SessionRecurrenceRule reloaded = ruleRepository.findById(id).orElseThrow();
        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertFalse(util.isLoaded(reloaded, "campaign"));
        assertFalse(util.isLoaded(reloaded, "center"));
        assertTrue(util.isLoaded(reloaded, "weekdays"));
        assertEquals(Set.of(DayOfWeek.MONDAY), reloaded.getWeekdays());
    }

    @Test
    void weekdayCollectionRejectsDuplicateRuleDay() {
        SessionRecurrenceRule saved = ruleRepository.saveAndFlush(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "UTC"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO session_recurrence_rule_weekdays (recurrence_rule_id, day_of_week) VALUES (?, ?)",
                saved.getId(), "MONDAY"));
    }

    @Test
    void databaseRejectsDuplicateGenerationKey() {
        SessionRecurrenceRule saved = ruleRepository.saveAndFlush(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "UTC"));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO session_recurrence_rules " +
                        "(campaign_id, center_id, start_date, end_date, start_time, end_time, timezone, " +
                        "recurrence_status, generation_key, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                campaignA.getId(), centerA.getId(), MONDAY, MONDAY,
                LocalTime.of(13, 0), LocalTime.of(14, 0), "UTC", "ACTIVE",
                saved.getGenerationKey(), Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z")));
    }

    @Test
    void campaignAndCenterHistoryOrderingIsDeterministicAndIsolated() {
        SessionRecurrenceRule laterTime = save(campaignA, centerA, MONDAY, LocalTime.of(13, 0));
        SessionRecurrenceRule early = save(campaignA, centerA, MONDAY.minusWeeks(1), LocalTime.of(10, 0));
        SessionRecurrenceRule earlierTime = save(campaignA, centerA, MONDAY, LocalTime.of(9, 0));
        save(campaignB, centerA, MONDAY.minusWeeks(2), LocalTime.of(8, 0));
        save(campaignA, centerB, MONDAY.minusWeeks(2), LocalTime.of(8, 0));
        ruleRepository.flush();

        assertEquals(
                List.of(early.getId(), earlierTime.getId(), laterTime.getId()),
                ruleRepository.findByCampaignIdOrderByStartDateAscStartTimeAscIdAsc(campaignA.getId())
                        .stream().filter(rule -> rule.getCenter().getId().equals(centerA.getId()))
                        .map(SessionRecurrenceRule::getId).toList());
        assertEquals(
                List.of(early.getId(), earlierTime.getId(), laterTime.getId()),
                ruleRepository.findByCenterIdOrderByStartDateAscStartTimeAscIdAsc(centerA.getId())
                        .stream().filter(rule -> rule.getCampaign().getId().equals(campaignA.getId()))
                        .map(SessionRecurrenceRule::getId).toList());
    }

    @Test
    void activeDateWeekdayQueryUsesInclusiveRangeAndDeterministicTimes() {
        SessionRecurrenceRule later = ruleRepository.save(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.plusWeeks(2),
                LocalTime.of(13, 0), LocalTime.of(15, 0), "UTC"));
        SessionRecurrenceRule earlier = ruleRepository.save(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), MONDAY, MONDAY.plusWeeks(2),
                LocalTime.of(9, 0), LocalTime.of(11, 0), "UTC"));
        SessionRecurrenceRule cancelled = ruleRepository.save(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.plusWeeks(2),
                LocalTime.of(8, 0), LocalTime.of(9, 0), "UTC"));
        cancelled.cancel();
        ruleRepository.flush();

        assertEquals(
                List.of(earlier.getId(), later.getId()),
                activeOn(campaignA, centerA, MONDAY).stream().map(SessionRecurrenceRule::getId).toList());
        assertEquals(
                List.of(earlier.getId(), later.getId()),
                activeOn(campaignA, centerA, MONDAY.plusWeeks(2)).stream()
                        .map(SessionRecurrenceRule::getId).toList());
        assertTrue(activeOn(campaignA, centerA, MONDAY.minusWeeks(1)).isEmpty());
        assertTrue(activeOn(campaignA, centerA, MONDAY.plusDays(1)).isEmpty());
        assertTrue(activeOn(campaignB, centerA, MONDAY).isEmpty());
        assertTrue(activeOn(campaignA, centerB, MONDAY).isEmpty());
    }

    @Test
    void parentForeignKeysPreventRemovalWithoutCascadingFromRule() {
        ruleRepository.saveAndFlush(rule(
                campaignA, centerA, Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "UTC"));

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("DELETE FROM vaccination_campaigns WHERE id = ?", campaignA.getId()));
    }

    private List<SessionRecurrenceRule> activeOn(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate date
    ) {
        return ruleRepository.findActiveRulesForCampaignCenterOn(
                campaign.getId(), center.getId(), SessionRecurrenceRuleStatus.ACTIVE,
                date, date.getDayOfWeek());
    }

    private SessionRecurrenceRule save(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            LocalDate startDate,
            LocalTime startTime
    ) {
        return ruleRepository.save(rule(
                campaign, center, Set.of(startDate.getDayOfWeek()), startDate, startDate.plusWeeks(4),
                startTime, startTime.plusHours(1), "UTC"));
    }

    private SessionRecurrenceRule rule(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        return new SessionRecurrenceRule(
                campaign, center, weekdays, startDate, endDate, startTime, endTime, timezone);
    }

    private VaccinationCampaign saveCampaign(
            String code,
            CampaignStatus status,
            VaccineType vaccine,
            TargetGroup group
    ) {
        return campaignRepository.saveAndFlush(new VaccinationCampaign(
                code, "Recurrence Campaign " + code, null, vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"), status));
    }

    private VaccinationCenter saveCenter(String code, CenterStatus status) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                code, "Recurrence Center " + code, "1 Health Road", "+94 11 000 0000",
                status, 100, 50));
    }
}
