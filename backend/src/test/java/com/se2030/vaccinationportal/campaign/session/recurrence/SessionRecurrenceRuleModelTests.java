package com.se2030.vaccinationportal.campaign.session.recurrence;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import java.lang.reflect.Modifier;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionRecurrenceRuleModelTests {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(12, 0);

    private VaccinationCampaign campaign;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        campaign = CampaignTestFixtures.campaign(
                "CMP-RULE-M", "Recurrence Model Campaign", null,
                new VaccineType("VAX-RULE-M", "Recurrence Vaccine"),
                new TargetGroup("TG-RULE-M", "Recurrence Group"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT);
        center = new VaccinationCenter(
                "CTR-RULE-M", "Recurrence Center", "1 Test Road", "+94 11 000 0000",
                CenterStatus.ACTIVE, 100, 50, "UTC");
    }

    @Test
    void constructionSupportsSingleDayMultiWeekAndAllWeekdays() {
        SessionRecurrenceRule single = rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY, "UTC");
        SessionRecurrenceRule multi = rule(EnumSet.allOf(DayOfWeek.class), MONDAY, MONDAY.plusWeeks(3),
                " Asia/Colombo ");

        assertAll(
                () -> assertEquals(Set.of(DayOfWeek.MONDAY), single.getWeekdays()),
                () -> assertEquals(MONDAY, single.getStartDate()),
                () -> assertEquals(MONDAY, single.getEndDate()),
                () -> assertEquals(SessionRecurrenceRuleStatus.ACTIVE, single.getStatus()),
                () -> assertEquals("Asia/Colombo", multi.getTimezone()),
                () -> assertEquals(EnumSet.allOf(DayOfWeek.class), multi.getWeekdays()),
                () -> assertEquals(multi.getGenerationKey(), UUID.fromString(multi.getGenerationKey()).toString())
        );
    }

    @Test
    void requiredParentsAndWeekdaysAreEnforced() {
        Set<DayOfWeek> withNull = new HashSet<>();
        withNull.add(DayOfWeek.MONDAY);
        withNull.add(null);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(null, center, Set.of(DayOfWeek.MONDAY),
                                MONDAY, MONDAY, START_TIME, END_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, null, Set.of(DayOfWeek.MONDAY),
                                MONDAY, MONDAY, START_TIME, END_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, null,
                                MONDAY, MONDAY, START_TIME, END_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(),
                                MONDAY, MONDAY, START_TIME, END_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, withNull,
                                MONDAY, MONDAY, START_TIME, END_TIME, "UTC"))
        );
    }

    @Test
    void weekdayStateIsDefensivelyCopiedAndReadOnly() {
        Set<DayOfWeek> input = EnumSet.of(DayOfWeek.MONDAY);
        SessionRecurrenceRule rule = rule(input, MONDAY, MONDAY.plusWeeks(1), "UTC");

        input.add(DayOfWeek.TUESDAY);

        assertEquals(Set.of(DayOfWeek.MONDAY), rule.getWeekdays());
        assertThrows(UnsupportedOperationException.class,
                () -> rule.getWeekdays().add(DayOfWeek.WEDNESDAY));
        assertEquals(Set.of(DayOfWeek.MONDAY), rule.getWeekdays());
    }

    @Test
    void dateRangeRequiresSelectedOccurrenceAndInclusiveBoundaries() {
        SessionRecurrenceRule rule = rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.plusWeeks(1), "UTC");

        assertAll(
                () -> assertTrue(rule.occursOn(MONDAY)),
                () -> assertTrue(rule.occursOn(MONDAY.plusWeeks(1))),
                () -> assertFalse(rule.occursOn(MONDAY.minusDays(1))),
                () -> assertFalse(rule.occursOn(MONDAY.plusDays(1))),
                () -> assertFalse(rule.occursOn(MONDAY.plusWeeks(1).plusDays(1))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> rule(Set.of(DayOfWeek.TUESDAY), MONDAY, MONDAY, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.minusDays(1), "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(DayOfWeek.MONDAY),
                                null, MONDAY, START_TIME, END_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(DayOfWeek.MONDAY),
                                MONDAY, null, START_TIME, END_TIME, "UTC"))
        );
    }

    @Test
    void timeIntervalMustBePositiveAndSameDay() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(DayOfWeek.MONDAY),
                                MONDAY, MONDAY, null, END_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(DayOfWeek.MONDAY),
                                MONDAY, MONDAY, START_TIME, null, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(DayOfWeek.MONDAY),
                                MONDAY, MONDAY, START_TIME, START_TIME, "UTC")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SessionRecurrenceRule(campaign, center, Set.of(DayOfWeek.MONDAY),
                                MONDAY, MONDAY, END_TIME, START_TIME, "UTC"))
        );
    }

    @Test
    void timezoneMustBeValidAndNonblank() {
        assertAll(
                () -> assertEquals("UTC", rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY, "UTC").getTimezone()),
                () -> assertEquals("Asia/Colombo",
                        rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY, "Asia/Colombo").getTimezone()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY, "   ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY, "Mars/Olympus"))
        );
    }

    @Test
    void mutationIsCohesiveStableAndExceptionSafe() {
        SessionRecurrenceRule rule = rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.plusWeeks(4), "UTC");
        String key = rule.getGenerationKey();

        rule.updateDefinition(Set.of(DayOfWeek.TUESDAY), MONDAY.plusDays(1), MONDAY.plusWeeks(3),
                LocalTime.of(13, 0), LocalTime.of(16, 0), "Asia/Colombo");
        rule.shortenEndDate(MONDAY.plusWeeks(2).plusDays(1));

        LocalDate retainedEnd = rule.getEndDate();
        assertThrows(IllegalArgumentException.class, () -> rule.shortenEndDate(MONDAY));
        assertThrows(IllegalArgumentException.class,
                () -> rule.updateDefinition(Set.of(DayOfWeek.WEDNESDAY), MONDAY, MONDAY,
                        END_TIME, START_TIME, "UTC"));

        assertAll(
                () -> assertEquals(Set.of(DayOfWeek.TUESDAY), rule.getWeekdays()),
                () -> assertEquals(retainedEnd, rule.getEndDate()),
                () -> assertEquals("Asia/Colombo", rule.getTimezone()),
                () -> assertEquals(key, rule.getGenerationKey()),
                () -> assertEquals(campaign, rule.getCampaign()),
                () -> assertEquals(center, rule.getCenter())
        );
    }

    @Test
    void cancellationIsTerminalAndMutationSurfaceIsNarrow() {
        SessionRecurrenceRule rule = rule(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.plusWeeks(2), "UTC");
        rule.cancel();

        var publicMethods = Arrays.stream(SessionRecurrenceRule.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName())
                .toList();

        assertAll(
                () -> assertEquals(SessionRecurrenceRuleStatus.CANCELLED, rule.getStatus()),
                () -> assertFalse(rule.occursOn(MONDAY)),
                () -> assertThrows(IllegalArgumentException.class, rule::cancel),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> rule.updateDefinition(Set.of(DayOfWeek.MONDAY), MONDAY, MONDAY.plusWeeks(1),
                                START_TIME, END_TIME, "UTC")),
                () -> assertFalse(publicMethods.contains("setId")),
                () -> assertFalse(publicMethods.contains("setCampaign")),
                () -> assertFalse(publicMethods.contains("setCenter")),
                () -> assertFalse(publicMethods.contains("setGenerationKey")),
                () -> assertFalse(publicMethods.contains("setCreatedAt")),
                () -> assertFalse(publicMethods.contains("setUpdatedAt")),
                () -> assertFalse(publicMethods.contains("equals")),
                () -> assertFalse(publicMethods.contains("hashCode"))
        );
    }

    @Test
    void recurrenceHorizonLimitEnforcesMax366Days() {
        assertEquals(366, SessionRecurrenceRule.MAX_RECURRENCE_HORIZON_DAYS);

        // Exactly 366 inclusive days (e.g., leap year span 2028-01-01 to 2028-12-31)
        LocalDate leapStart = LocalDate.of(2028, 1, 1);
        LocalDate leapEnd = LocalDate.of(2028, 12, 31);
        assertDoesNotThrow(() -> rule(Set.of(DayOfWeek.MONDAY), leapStart, leapEnd, "UTC"));

        // 367 inclusive days (e.g., 2028-01-01 to 2029-01-01)
        LocalDate overHorizonEnd = LocalDate.of(2029, 1, 1);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> rule(Set.of(DayOfWeek.MONDAY), leapStart, overHorizonEnd, "UTC")
        );
        assertTrue(ex.getMessage().contains("must not exceed 366 calendar days"));
    }

    private SessionRecurrenceRule rule(
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            String timezone
    ) {
        return new SessionRecurrenceRule(
                campaign, center, weekdays, startDate, endDate, START_TIME, END_TIME, timezone);
    }
}
