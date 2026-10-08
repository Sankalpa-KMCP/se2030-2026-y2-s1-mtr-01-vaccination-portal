package com.se2030.vaccinationportal.campaign.session.recurrence;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRuleStatus;
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
import com.se2030.vaccinationportal.campaign.session.recurrence.service.SessionRecurrenceRuleService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SessionRecurrenceRuleServiceIntegrationTests.FixedClockConfig.class)
class SessionRecurrenceRuleServiceIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-09-15T23:30:00Z");
    private static final LocalDate UTC_TODAY = LocalDate.of(2026, 9, 15);
    private static final LocalTime MORNING = LocalTime.of(9, 0);
    private static final LocalTime NOON = LocalTime.of(12, 0);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired private SessionRecurrenceRuleService service;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;

    @Test
    void createsHistoricalCurrentAndFutureRulesWithIndependentKeys() {
        VaccinationCampaign campaign = campaign("CREATE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("CREATE", CenterStatus.ACTIVE);

        SessionRecurrenceRule historical = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 31), MORNING, NOON, "UTC");
        SessionRecurrenceRule current = create(
                campaign, center, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21), MORNING, NOON, "UTC");
        SessionRecurrenceRule future = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC");

        assertEquals(List.of(historical.getId(), current.getId(), future.getId()),
                service.getRulesForCampaign(campaign.getId()).stream()
                        .map(SessionRecurrenceRule::getId).toList());
        assertNotEquals(historical.getGenerationKey(), current.getGenerationKey());
        assertNotEquals(current.getGenerationKey(), future.getGenerationKey());
    }

    @Test
    void creationRejectsMissingParentsAndInvalidDefinition() {
        VaccinationCampaign campaign = campaign("INVALID", CampaignStatus.DRAFT);
        VaccinationCenter center = center("INVALID", CenterStatus.ACTIVE);

        assertThrows(ResourceNotFoundException.class,
                () -> service.createRule(999_999L, center.getId(), Set.of(DayOfWeek.MONDAY),
                        LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), MORNING, NOON, "UTC"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.createRule(campaign.getId(), 999_999L, Set.of(DayOfWeek.MONDAY),
                        LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), MORNING, NOON, "UTC"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createRule(campaign.getId(), center.getId(), Set.of(),
                        LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), MORNING, NOON, "UTC"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createRule(campaign.getId(), center.getId(), Set.of(DayOfWeek.MONDAY),
                        LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), NOON, MORNING, "UTC"));
    }

    @Test
    void configurationIgnoresParentLifecyclesServiceDatesAndOtherFoundations() {
        VaccinationCampaign campaign = campaign("INDEPENDENT", CampaignStatus.DRAFT);
        VaccinationCenter center = center("INDEPENDENT", CenterStatus.INACTIVE);

        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 2, 15), MORNING, NOON, "UTC");

        assertEquals(campaign.getId(), rule.getCampaign().getId());
        assertEquals(center.getId(), rule.getCenter().getId());
        assertEquals(SessionRecurrenceRuleStatus.ACTIVE, rule.getStatus());
    }

    @Test
    void identicalAndOverlappingRuleDefinitionsAreAllowed() {
        VaccinationCampaign campaign = campaign("OVERLAP", CampaignStatus.DRAFT);
        VaccinationCenter center = center("OVERLAP", CenterStatus.ACTIVE);
        LocalDate start = LocalDate.of(2026, 9, 21);

        SessionRecurrenceRule first = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), start, start.plusWeeks(2),
                MORNING, NOON, "UTC");
        SessionRecurrenceRule identical = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), start, start.plusWeeks(2),
                MORNING, NOON, "UTC");
        SessionRecurrenceRule overlapping = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), start, start.plusWeeks(3),
                LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC");

        assertEquals(3, service.getRulesForCampaign(campaign.getId()).size());
        assertNotEquals(first.getGenerationKey(), identical.getGenerationKey());
        assertNotEquals(identical.getGenerationKey(), overlapping.getGenerationKey());
    }

    @Test
    void readsByIdKeyCampaignCenterAndActiveDateWeekday() {
        VaccinationCampaign campaign = campaign("READ", CampaignStatus.DRAFT);
        VaccinationCenter center = center("READ", CenterStatus.ACTIVE);
        LocalDate monday = LocalDate.of(2026, 9, 21);
        SessionRecurrenceRule later = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), monday, monday.plusWeeks(1),
                LocalTime.of(13, 0), LocalTime.of(15, 0), "UTC");
        SessionRecurrenceRule earlier = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), monday, monday.plusWeeks(1),
                MORNING, NOON, "UTC");

        assertEquals(earlier.getId(), service.getRuleById(earlier.getId()).orElseThrow().getId());
        assertEquals(earlier.getId(),
                service.getRuleByGenerationKey(earlier.getGenerationKey()).orElseThrow().getId());
        assertTrue(service.getRuleById(999_999L).isEmpty());
        assertEquals(List.of(earlier.getId(), later.getId()),
                service.getRulesForCenter(center.getId()).stream()
                        .map(SessionRecurrenceRule::getId).toList());
        assertEquals(List.of(earlier.getId(), later.getId()),
                service.getActiveRulesForCampaignCenterOn(campaign.getId(), center.getId(), monday)
                        .stream().map(SessionRecurrenceRule::getId).toList());
        assertTrue(service.getActiveRulesForCampaignCenterOn(
                campaign.getId(), center.getId(), monday.plusDays(1)).isEmpty());
    }

    @Test
    void futureFullUpdateChangesPatternDatesTimesTimezoneAndPreservesIdentity() {
        VaccinationCampaign campaign = campaign("UPDATE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("UPDATE", CenterStatus.ACTIVE);
        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC");
        String key = rule.getGenerationKey();

        SessionRecurrenceRule updated = service.updateFutureRule(
                rule.getId(), Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                LocalDate.of(2026, 9, 22), LocalDate.of(2026, 10, 8),
                LocalTime.of(13, 0), LocalTime.of(16, 0), "Asia/Colombo");

        assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), updated.getWeekdays());
        assertEquals(LocalDate.of(2026, 9, 22), updated.getStartDate());
        assertEquals(LocalTime.of(13, 0), updated.getStartTime());
        assertEquals("Asia/Colombo", updated.getTimezone());
        assertEquals(key, updated.getGenerationKey());
        assertEquals(campaign.getId(), updated.getCampaign().getId());
        assertEquals(center.getId(), updated.getCenter().getId());
    }

    @Test
    void invalidFutureUpdatePreservesStoredDefinition() {
        VaccinationCampaign campaign = campaign("UPDATE-INVALID", CampaignStatus.DRAFT);
        VaccinationCenter center = center("UPDATE-INVALID", CenterStatus.ACTIVE);
        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureRule(rule.getId(), Set.of(DayOfWeek.TUESDAY),
                        LocalDate.of(2026, 9, 22), LocalDate.of(2026, 10, 6),
                        NOON, MORNING, "UTC"));

        SessionRecurrenceRule retained = service.getRuleById(rule.getId()).orElseThrow();
        assertEquals(Set.of(DayOfWeek.MONDAY), retained.getWeekdays());
        assertEquals(LocalDate.of(2026, 9, 21), retained.getStartDate());
        assertEquals(MORNING, retained.getStartTime());
        assertEquals("UTC", retained.getTimezone());
    }

    @Test
    void fullUpdateRejectsStartedExpiredAndCancelledRules() {
        VaccinationCampaign campaign = campaign("UPDATE-STATE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("UPDATE-STATE", CenterStatus.ACTIVE);
        SessionRecurrenceRule started = create(
                campaign, center, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 28), MORNING, NOON, "UTC");
        SessionRecurrenceRule expired = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 31), MORNING, NOON, "UTC");
        SessionRecurrenceRule cancelled = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC");
        service.cancelRule(cancelled.getId());

        assertThrows(IllegalArgumentException.class, () -> updateToTuesday(started));
        assertThrows(IllegalArgumentException.class, () -> updateToTuesday(expired));
        assertThrows(IllegalArgumentException.class, () -> updateToTuesday(cancelled));
    }

    @Test
    void fullUpdateUsesExistingAndCandidateRuleTimezones() {
        VaccinationCampaign campaign = campaign("UPDATE-ZONE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("UPDATE-ZONE", CenterStatus.ACTIVE);
        SessionRecurrenceRule existingColombo = create(
                campaign, center, Set.of(DayOfWeek.WEDNESDAY), LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 30), MORNING, NOON, "Asia/Colombo");
        SessionRecurrenceRule existingUtc = create(
                campaign, center, Set.of(DayOfWeek.WEDNESDAY), LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 30), MORNING, NOON, "UTC");

        assertThrows(IllegalArgumentException.class, () -> updateToThursday(existingColombo, "UTC",
                LocalDate.of(2026, 9, 17)));
        assertThrows(IllegalArgumentException.class, () -> updateToThursday(existingUtc, "Asia/Colombo",
                LocalDate.of(2026, 9, 16)));
    }

    @Test
    void shorteningAllowsInclusiveTodayAndPreservesHistoryCriticalFields() {
        VaccinationCampaign campaign = campaign("SHORTEN", CampaignStatus.DRAFT);
        VaccinationCenter center = center("SHORTEN", CenterStatus.ACTIVE);
        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC");
        String key = rule.getGenerationKey();

        SessionRecurrenceRule shortened = service.shortenRuleEnd(rule.getId(), UTC_TODAY);

        assertEquals(UTC_TODAY, shortened.getEndDate());
        assertEquals(SessionRecurrenceRuleStatus.ACTIVE, shortened.getStatus());
        assertEquals(key, shortened.getGenerationKey());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), shortened.getWeekdays());
        assertEquals(MORNING, shortened.getStartTime());
        assertEquals("UTC", shortened.getTimezone());
    }

    @Test
    void shorteningRejectsNoOpExtensionRetroactiveExpiredAndCancelled() {
        VaccinationCampaign campaign = campaign("SHORTEN-INVALID", CampaignStatus.DRAFT);
        VaccinationCenter center = center("SHORTEN-INVALID", CenterStatus.ACTIVE);
        SessionRecurrenceRule active = create(
                campaign, center, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 28), MORNING, NOON, "UTC");
        SessionRecurrenceRule expired = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 31), MORNING, NOON, "UTC");
        SessionRecurrenceRule cancelled = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC");
        service.cancelRule(cancelled.getId());

        assertThrows(IllegalArgumentException.class,
                () -> service.shortenRuleEnd(active.getId(), active.getEndDate()));
        assertThrows(IllegalArgumentException.class,
                () -> service.shortenRuleEnd(active.getId(), active.getEndDate().plusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.shortenRuleEnd(active.getId(), UTC_TODAY.minusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.shortenRuleEnd(expired.getId(), expired.getEndDate().minusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.shortenRuleEnd(cancelled.getId(), LocalDate.of(2026, 9, 28)));
    }

    @Test
    void cancellationIsTerminalRetainedAndPreservesDefinition() {
        VaccinationCampaign campaign = campaign("CANCEL", CampaignStatus.DRAFT);
        VaccinationCenter center = center("CANCEL", CenterStatus.ACTIVE);
        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 28), MORNING, NOON, "UTC");
        String key = rule.getGenerationKey();

        SessionRecurrenceRule cancelled = service.cancelRule(rule.getId());

        assertEquals(SessionRecurrenceRuleStatus.CANCELLED, cancelled.getStatus());
        assertEquals(key, cancelled.getGenerationKey());
        assertEquals(LocalDate.of(2026, 9, 14), cancelled.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 28), cancelled.getEndDate());
        assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), cancelled.getWeekdays());
        assertThrows(IllegalArgumentException.class, () -> service.cancelRule(rule.getId()));
        assertEquals(1, service.getRulesForCampaign(campaign.getId()).size());
    }

    @Test
    void expiredRuleCannotBeCancelled() {
        VaccinationCampaign campaign = campaign("CANCEL-EXPIRED", CampaignStatus.DRAFT);
        VaccinationCenter center = center("CANCEL-EXPIRED", CenterStatus.ACTIVE);
        SessionRecurrenceRule expired = create(
                campaign, center, Set.of(DayOfWeek.MONDAY), LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 31), MORNING, NOON, "UTC");

        assertThrows(IllegalArgumentException.class, () -> service.cancelRule(expired.getId()));
        assertEquals(SessionRecurrenceRuleStatus.ACTIVE,
                service.getRuleById(expired.getId()).orElseThrow().getStatus());
    }

    @Test
    void readValidationRejectsMissingInputs() {
        assertThrows(IllegalArgumentException.class, () -> service.getRuleById(null));
        assertThrows(IllegalArgumentException.class, () -> service.getRuleByGenerationKey(" "));
        assertThrows(IllegalArgumentException.class, () -> service.getRulesForCampaign(null));
        assertThrows(IllegalArgumentException.class, () -> service.getRulesForCenter(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.getActiveRulesForCampaignCenterOn(1L, 2L, null));
    }

    @Test
    void updateFutureRule_failsWhenGeneratedSessionsExist() {
        VaccinationCampaign campaign = campaign("MUT-UPD", CampaignStatus.DRAFT);
        VaccinationCenter center = center("MUT-UPD", CenterStatus.ACTIVE);

        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 22), LocalDate.of(2026, 10, 6),
                MORNING, NOON, "UTC");

        // Simulate a generated session existing for the rule
        sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, rule, rule.getGenerationKey() + ":2026-09-22",
                LocalDate.of(2026, 9, 22),
                Instant.parse("2026-09-22T09:00:00Z"), Instant.parse("2026-09-22T12:00:00Z"),
                Instant.parse("2026-09-22T09:00:00Z"), "UTC"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureRule(rule.getId(), Set.of(DayOfWeek.WEDNESDAY),
                        LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 7),
                        MORNING, NOON, "UTC"));
        assertTrue(ex.getMessage().contains("Cannot update recurrence definition for a rule with existing generated sessions"));
    }

    @Test
    void shortenRuleEnd_failsWhenGeneratedSessionExistsAfterNewEndDate() {
        VaccinationCampaign campaign = campaign("MUT-SHR-FAIL", CampaignStatus.DRAFT);
        VaccinationCenter center = center("MUT-SHR-FAIL", CenterStatus.ACTIVE);

        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5),
                MORNING, NOON, "UTC");

        // Generated session on 2026-10-05
        sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, rule, rule.getGenerationKey() + ":2026-10-05",
                LocalDate.of(2026, 10, 05),
                Instant.parse("2026-10-05T09:00:00Z"), Instant.parse("2026-10-05T12:00:00Z"),
                Instant.parse("2026-10-05T09:00:00Z"), "UTC"));

        // Attempt shortening to 2026-09-28 (before the Oct 5 session)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.shortenRuleEnd(rule.getId(), LocalDate.of(2026, 9, 28)));
        assertTrue(ex.getMessage().contains("Cannot shorten recurrence rule end date before existing generated session occurrences"));
    }

    @Test
    void shortenRuleEnd_succeedsWhenAllGeneratedSessionsAreWithinNewEndDate() {
        VaccinationCampaign campaign = campaign("MUT-SHR-OK", CampaignStatus.DRAFT);
        VaccinationCenter center = center("MUT-SHR-OK", CenterStatus.ACTIVE);

        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5),
                MORNING, NOON, "UTC");

        // Generated session on 2026-09-21
        sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, rule, rule.getGenerationKey() + ":2026-09-21",
                LocalDate.of(2026, 9, 21),
                Instant.parse("2026-09-21T09:00:00Z"), Instant.parse("2026-09-21T12:00:00Z"),
                Instant.parse("2026-09-21T09:00:00Z"), "UTC"));

        // Shortening to 2026-09-28 retains the 2026-09-21 session
        SessionRecurrenceRule updated = service.shortenRuleEnd(rule.getId(), LocalDate.of(2026, 9, 28));
        assertEquals(LocalDate.of(2026, 9, 28), updated.getEndDate());
    }

    @Test
    void cancelRule_succeedsWhenGeneratedSessionsExist_andLeavesSessionsUntouched() {
        VaccinationCampaign campaign = campaign("MUT-CNC", CampaignStatus.DRAFT);
        VaccinationCenter center = center("MUT-CNC", CenterStatus.ACTIVE);

        SessionRecurrenceRule rule = create(
                campaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5),
                MORNING, NOON, "UTC");

        // Generated session on 2026-09-21
        VaccinationSession session = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, rule, rule.getGenerationKey() + ":2026-09-21",
                LocalDate.of(2026, 9, 21),
                Instant.parse("2026-09-21T09:00:00Z"), Instant.parse("2026-09-21T12:00:00Z"),
                Instant.parse("2026-09-21T09:00:00Z"), "UTC"));

        SessionRecurrenceRule cancelled = service.cancelRule(rule.getId());
        assertEquals(SessionRecurrenceRuleStatus.CANCELLED, cancelled.getStatus());

        // Verify session remains in DB unchanged
        VaccinationSession remainingSession = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(SessionLifecycleStatus.DRAFT, remainingSession.getLifecycleStatus());
        assertEquals(rule.getId(), remainingSession.getRecurrenceRule().getId());
    }

    @Test
    void createRule_whenCampaignIsSuspended_throwsIllegalArgumentException() {
        VaccinationCenter center = center("SUSP-CTR", CenterStatus.ACTIVE);
        VaccinationCampaign suspendedCampaign = campaign("SUSP-CREATE", CampaignStatus.SUSPENDED);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> create(
                suspendedCampaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC"));
        assertTrue(ex.getMessage().contains("Cannot create session recurrence rule for suspended campaign"));
    }

    @Test
    void updateFutureRule_whenCampaignIsSuspended_throwsIllegalArgumentException() {
        VaccinationCenter center = center("UPD-SUSP-CTR", CenterStatus.ACTIVE);
        VaccinationCampaign suspendedCampaign = campaign("UPD-SUSP-RULE", CampaignStatus.SUSPENDED);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                suspendedCampaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> updateToTuesday(rule));
        assertTrue(ex.getMessage().contains("Cannot update future session recurrence rule for suspended campaign"));
    }

    @Test
    void shortenRuleEnd_whenCampaignIsSuspended_succeeds() {
        VaccinationCenter center = center("SHORT-SUSP-CTR", CenterStatus.ACTIVE);
        VaccinationCampaign suspendedCampaign = campaign("SHORT-SUSP-RULE", CampaignStatus.SUSPENDED);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                suspendedCampaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC"));

        SessionRecurrenceRule shortened = service.shortenRuleEnd(rule.getId(), LocalDate.of(2026, 9, 28));
        assertEquals(LocalDate.of(2026, 9, 28), shortened.getEndDate());
    }

    @Test
    void cancelRule_whenCampaignIsSuspended_succeeds() {
        VaccinationCenter center = center("CNC-SUSP-CTR", CenterStatus.ACTIVE);
        VaccinationCampaign suspendedCampaign = campaign("CNC-SUSP-RULE", CampaignStatus.SUSPENDED);
        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                suspendedCampaign, center, Set.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC"));

        SessionRecurrenceRule cancelled = service.cancelRule(rule.getId());
        assertEquals(SessionRecurrenceRuleStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void createRule_whenCampaignIsOperationallyClosed_throwsIllegalArgumentException() {
        VaccinationCenter center = center("CLOSED-CTR", CenterStatus.ACTIVE);
        for (CampaignStatus status : List.of(CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.ARCHIVED)) {
            VaccinationCampaign closedCampaign = campaign("CLOSED-" + status.name(), status);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> create(
                    closedCampaign, center, Set.of(DayOfWeek.MONDAY),
                    LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC"));
            assertTrue(ex.getMessage().contains("Cannot create session recurrence rule for operationally closed campaign"));
        }
    }

    @Test
    void updateFutureRule_whenCampaignIsOperationallyClosed_throwsIllegalArgumentException() {
        VaccinationCenter center = center("UPD-CLOSED-CTR", CenterStatus.ACTIVE);
        for (CampaignStatus status : List.of(CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.ARCHIVED)) {
            VaccinationCampaign closedCampaign = campaign("UPD-CLOSED-" + status.name(), status);
            SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                    closedCampaign, center, Set.of(DayOfWeek.MONDAY),
                    LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 5), MORNING, NOON, "UTC"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> updateToTuesday(rule));
            assertTrue(ex.getMessage().contains("Cannot update future session recurrence rule for operationally closed campaign"));
        }
    }

    private void updateToTuesday(SessionRecurrenceRule rule) {
        service.updateFutureRule(rule.getId(), Set.of(DayOfWeek.TUESDAY),
                LocalDate.of(2026, 9, 22), LocalDate.of(2026, 10, 6),
                MORNING, NOON, "UTC");
    }

    @Test
    void missingRuleId_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () ->
                service.updateFutureRule(999_999L, Set.of(DayOfWeek.MONDAY),
                        LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), MORNING, NOON, "UTC"));
        assertThrows(ResourceNotFoundException.class, () ->
                service.shortenRuleEnd(999_999L, LocalDate.of(2026, 9, 21)));
        assertThrows(ResourceNotFoundException.class, () ->
                service.cancelRule(999_999L));
    }

    private void updateToThursday(SessionRecurrenceRule rule, String timezone, LocalDate startDate) {
        service.updateFutureRule(rule.getId(), Set.of(DayOfWeek.THURSDAY),
                startDate, LocalDate.of(2026, 10, 8), MORNING, NOON, timezone);
    }

    private SessionRecurrenceRule create(
            VaccinationCampaign campaign,
            VaccinationCenter center,
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        return service.createRule(
                campaign.getId(), center.getId(), weekdays, startDate, endDate, startTime, endTime, timezone);
    }

    private VaccinationCampaign campaign(String suffix, CampaignStatus status) {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-RULE-S-" + suffix, "Recurrence Vaccine " + suffix));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-RULE-S-" + suffix, "Recurrence Group " + suffix));
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-RULE-S-" + suffix, "Recurrence Campaign " + suffix, null, vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"), status));
    }

    private VaccinationCenter center(String suffix, CenterStatus status) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-RULE-S-" + suffix, "Recurrence Center " + suffix,
                "1 Health Road", "+94 11 000 0000", status, 100, 50, "UTC"));
    }
}
