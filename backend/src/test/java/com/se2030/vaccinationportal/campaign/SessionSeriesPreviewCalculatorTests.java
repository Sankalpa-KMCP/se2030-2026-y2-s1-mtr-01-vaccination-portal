package com.se2030.vaccinationportal.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesOccurrencePreviewItem;
import com.se2030.vaccinationportal.campaign.service.SessionImpactReader;
import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesPreviewCalculator;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SessionSeriesPreviewCalculator Unit Tests")
class SessionSeriesPreviewCalculatorTests {

    private VaccinationSessionValidator validator;
    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private SessionRecurrenceRule rule;

    @BeforeEach
    void setUp() throws Exception {
        validator = mock(VaccinationSessionValidator.class);

        VaccineType vaccineType = new VaccineType("VAX-TEST", "Test Vaccine");
        TargetGroup targetGroup = new TargetGroup("TG-TEST", "Test Target Group");

        campaign = CampaignTestFixtures.campaign(
                "CMP-TEST", "Test Campaign", "Testing series calculator",
                vaccineType, targetGroup,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-30T00:00:00Z"),
                CampaignStatus.DRAFT
        );
        setId(campaign, 100L);

        center = new VaccinationCenter(
                "CTR-TEST", "Central Clinic", "123 Main St", "+94 11 111 1111",
                CenterStatus.ACTIVE, 200, 100, "Asia/Colombo"
        );
        setId(center, 200L);

        rule = new SessionRecurrenceRule(
                campaign,
                center,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 14),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "Asia/Colombo"
        );
        setId(rule, 300L);
    }

    @Test
    @DisplayName("Token generation is deterministic and bit-for-bit identical")
    void testTokenDeterminism() {
        RecurrenceRuleDefinitionSnapshot currentDef = RecurrenceRuleDefinitionSnapshot.from(rule);
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 12),
                LocalTime.of(10, 0),
                LocalTime.of(13, 0),
                "Asia/Colombo"
        );

        SessionScheduleSnapshot snap1 = new SessionScheduleSnapshot(
                200L, "Asia/Colombo", Instant.parse("2026-10-05T03:30:00Z"),
                Instant.parse("2026-10-05T06:30:00Z"), Instant.parse("2026-10-05T03:30:00Z"),
                SessionLifecycleStatus.DRAFT);

        SessionScheduleSnapshot snap2 = new SessionScheduleSnapshot(
                200L, "Asia/Colombo", Instant.parse("2026-10-05T04:30:00Z"),
                Instant.parse("2026-10-05T07:30:00Z"), Instant.parse("2026-10-05T04:30:00Z"),
                SessionLifecycleStatus.DRAFT);

        List<SeriesOccurrencePreviewItem> items = List.of(
                new SeriesOccurrencePreviewItem("rule:2026-10-05", LocalDate.of(2026, 10, 5),
                        SeriesOccurrenceCategory.RETAINED, 1L, snap1, snap2, 0, 0)
        );

        String token1 = SessionSeriesPreviewCalculator.generateSeriesPreviewToken(
                300L, currentDef, proposedDef, items, 0, 0);
        String token2 = SessionSeriesPreviewCalculator.generateSeriesPreviewToken(
                300L, currentDef, proposedDef, items, 0, 0);

        assertThat(token1).isNotNull().isNotEmpty().hasSize(64);
        assertThat(token1).isEqualTo(token2);
    }

    @Test
    @DisplayName("calculateSeriesChangePreview categorizes RETAINED, REMOVED, and NEW occurrences correctly")
    void testOccurrenceClassification() throws Exception {
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator);

        // Existing sessions on Mon Oct 5 and Wed Oct 7
        VaccinationSession s1 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-05", LocalDate.of(2026, 10, 5),
                Instant.parse("2026-10-05T03:30:00Z"), Instant.parse("2026-10-05T06:30:00Z"),
                Instant.parse("2026-10-05T03:30:00Z"), "Asia/Colombo"
        );
        setId(s1, 1L);

        VaccinationSession s2 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-07", LocalDate.of(2026, 10, 7),
                Instant.parse("2026-10-07T03:30:00Z"), Instant.parse("2026-10-07T06:30:00Z"),
                Instant.parse("2026-10-07T03:30:00Z"), "Asia/Colombo"
        );
        setId(s2, 2L);

        List<VaccinationSession> existing = List.of(s1, s2);

        // Proposed definition: MONDAY only, Oct 5 to Oct 12 (adds Mon Oct 12 as NEW, removes Wed Oct 7)
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 12),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "Asia/Colombo"
        );

        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        SeriesChangePreview preview = calculator.calculateSeriesChangePreview(
                rule, campaign, center, existing, Collections.emptySet(), proposedDef, now);

        assertThat(preview).isNotNull();
        assertThat(preview.occurrenceEntries()).hasSize(3);

        SeriesOccurrencePreviewItem itemOct5 = preview.occurrenceEntries().stream()
                .filter(e -> e.originOccurrenceDate().equals(LocalDate.of(2026, 10, 5)))
                .findFirst().orElseThrow();
        assertThat(itemOct5.category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);

        SeriesOccurrencePreviewItem itemOct7 = preview.occurrenceEntries().stream()
                .filter(e -> e.originOccurrenceDate().equals(LocalDate.of(2026, 10, 7)))
                .findFirst().orElseThrow();
        assertThat(itemOct7.category()).isEqualTo(SeriesOccurrenceCategory.REMOVED);

        SeriesOccurrencePreviewItem itemOct12 = preview.occurrenceEntries().stream()
                .filter(e -> e.originOccurrenceDate().equals(LocalDate.of(2026, 10, 12)))
                .findFirst().orElseThrow();
        assertThat(itemOct12.category()).isEqualTo(SeriesOccurrenceCategory.NEW);
    }

    @Test
    @DisplayName("calculateSeriesChangePreview preserves overridden occurrences as PRESERVED_EXCEPTION")
    void testPreservedExceptionHandling() throws Exception {
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator);

        VaccinationSession s1 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-05", LocalDate.of(2026, 10, 5),
                Instant.parse("2026-10-05T03:30:00Z"), Instant.parse("2026-10-05T06:30:00Z"),
                Instant.parse("2026-10-05T03:30:00Z"), "Asia/Colombo"
        );
        setId(s1, 1L);

        List<VaccinationSession> existing = List.of(s1);
        Set<Long> overriddenIds = Set.of(1L);

        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 5),
                LocalTime.of(14, 0),
                LocalTime.of(17, 0),
                "Asia/Colombo"
        );

        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        SeriesChangePreview preview = calculator.calculateSeriesChangePreview(
                rule, campaign, center, existing, overriddenIds, proposedDef, now);

        assertThat(preview.occurrenceEntries()).hasSize(1);
        SeriesOccurrencePreviewItem item = preview.occurrenceEntries().get(0);
        assertThat(item.category()).isEqualTo(SeriesOccurrenceCategory.PRESERVED_EXCEPTION);
        assertThat(item.affectedAppointmentCount()).isEqualTo(0);
        assertThat(item.affectedWaitlistCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateSeriesChangePreview classifies elapsed sessions as INELIGIBLE")
    void testIneligibleClassificationForElapsedSession() throws Exception {
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator);

        VaccinationSession s1 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-05", LocalDate.of(2026, 10, 5),
                Instant.parse("2026-10-05T03:30:00Z"), Instant.parse("2026-10-05T06:30:00Z"),
                Instant.parse("2026-10-05T03:30:00Z"), "Asia/Colombo"
        );
        setId(s1, 1L);

        List<VaccinationSession> existing = List.of(s1);

        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 5),
                LocalTime.of(10, 0),
                LocalTime.of(13, 0),
                "Asia/Colombo"
        );

        // now is after session start
        Instant now = Instant.parse("2026-10-05T05:00:00Z");

        SeriesChangePreview preview = calculator.calculateSeriesChangePreview(
                rule, campaign, center, existing, Collections.emptySet(), proposedDef, now);

        assertThat(preview.occurrenceEntries()).hasSize(1);
        assertThat(preview.occurrenceEntries().get(0).category()).isEqualTo(SeriesOccurrenceCategory.INELIGIBLE);
    }

    @Test
    @DisplayName("calculateCancellationPreview marks active occurrences as REMOVED and preserves overrides")
    void testCancellationPreview() throws Exception {
        SessionImpactReader impactReader = sid -> new SessionImpactSummary(4, 1);
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator, impactReader);

        VaccinationSession s1 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-05", LocalDate.of(2026, 10, 5),
                Instant.parse("2026-10-05T03:30:00Z"), Instant.parse("2026-10-05T06:30:00Z"),
                Instant.parse("2026-10-05T03:30:00Z"), "Asia/Colombo"
        );
        setId(s1, 1L);
        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);

        VaccinationSession s2 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-07", LocalDate.of(2026, 10, 7),
                Instant.parse("2026-10-07T03:30:00Z"), Instant.parse("2026-10-07T06:30:00Z"),
                Instant.parse("2026-10-07T03:30:00Z"), "Asia/Colombo"
        );
        setId(s2, 2L);

        List<VaccinationSession> existing = List.of(s1, s2);
        Set<Long> overriddenIds = Set.of(2L);
        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        SeriesChangePreview preview = calculator.calculateCancellationPreview(
                rule, campaign, center, existing, overriddenIds, now);

        assertThat(preview.occurrenceEntries()).hasSize(2);

        SeriesOccurrencePreviewItem item1 = preview.occurrenceEntries().get(0);
        assertThat(item1.category()).isEqualTo(SeriesOccurrenceCategory.REMOVED);
        assertThat(item1.affectedAppointmentCount()).isEqualTo(4);
        assertThat(item1.affectedWaitlistCount()).isEqualTo(1);

        SeriesOccurrencePreviewItem item2 = preview.occurrenceEntries().get(1);
        assertThat(item2.category()).isEqualTo(SeriesOccurrenceCategory.PRESERVED_EXCEPTION);
    }

    @Test
    @DisplayName("Regression: Null SessionImpactReader is allowed when scheduled retained session is unshifted")
    void testNullImpactReaderAllowedWhenUnshifted() throws Exception {
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator, null);

        VaccinationSession s1 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-05", LocalDate.of(2026, 10, 5),
                Instant.parse("2026-10-05T03:30:00Z"), Instant.parse("2026-10-05T06:30:00Z"),
                Instant.parse("2026-10-05T03:30:00Z"), "Asia/Colombo"
        );
        setId(s1, 1L);
        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);

        List<VaccinationSession> existing = List.of(s1);

        // Same time as existing session
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 5),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "Asia/Colombo"
        );

        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        assertDoesNotThrow(() -> {
            SeriesChangePreview preview = calculator.calculateSeriesChangePreview(
                    rule, campaign, center, existing, Collections.emptySet(), proposedDef, now);
            assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Null SessionImpactReader throws IllegalStateException when scheduled session shifts")
    void testNullImpactReaderThrowsWhenScheduledSessionShifts() throws Exception {
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator, null);

        VaccinationSession s1 = new VaccinationSession(
                campaign, center, rule, "rule:2026-10-05", LocalDate.of(2026, 10, 5),
                Instant.parse("2026-10-05T03:30:00Z"), Instant.parse("2026-10-05T06:30:00Z"),
                Instant.parse("2026-10-05T03:30:00Z"), "Asia/Colombo"
        );
        setId(s1, 1L);
        s1.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);

        List<VaccinationSession> existing = List.of(s1);

        // Shifted time (10:00 - 13:00)
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 5),
                LocalTime.of(10, 0),
                LocalTime.of(13, 0),
                "Asia/Colombo"
        );

        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        assertThatThrownBy(() -> calculator.calculateSeriesChangePreview(
                rule, campaign, center, existing, Collections.emptySet(), proposedDef, now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Session impact reader is not available to assess SCHEDULED session changes");
    }

    @Test
    @DisplayName("Timezone mismatch between proposed definition and center throws IllegalArgumentException")
    void testTimezoneMismatchRejection() {
        SessionSeriesPreviewCalculator calculator = new SessionSeriesPreviewCalculator(validator);

        RecurrenceRuleDefinitionSnapshot mismatchedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 5),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "America/New_York"
        );

        Instant now = Instant.parse("2026-10-01T00:00:00Z");

        assertThatThrownBy(() -> calculator.calculateSeriesChangePreview(
                rule, campaign, center, Collections.emptyList(), Collections.emptySet(), mismatchedDef, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match center scheduling timezone");
    }

    @Test
    @DisplayName("Proposed definition exceeding 366-day calendar horizon throws IllegalArgumentException")
    void testHorizonExceededRejection() {
        assertThatThrownBy(() -> new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2028, 1, 1),
                LocalDate.of(2029, 1, 1), // 367 inclusive days
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                "Asia/Colombo"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 366 calendar days");
    }

    private static void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
