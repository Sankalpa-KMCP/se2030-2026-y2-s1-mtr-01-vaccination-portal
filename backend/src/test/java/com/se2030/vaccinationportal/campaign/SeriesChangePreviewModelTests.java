package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesOccurrencePreviewItem;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesChangeService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeriesChangePreviewModelTests {

    private static final String TZ = "Asia/Colombo";
    private static final Set<DayOfWeek> MON_WED = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 30);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(17, 0);

    @Test
    @DisplayName("RecurrenceRuleDefinitionSnapshot correctly encapsulates validated definition and evaluates occursOn")
    void testRecurrenceRuleDefinitionSnapshot() {
        RecurrenceRuleDefinitionSnapshot snapshot = new RecurrenceRuleDefinitionSnapshot(
                MON_WED, START_DATE, END_DATE, START_TIME, END_TIME, TZ);

        assertThat(snapshot.getWeekdays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        assertThat(snapshot.getStartDate()).isEqualTo(START_DATE);
        assertThat(snapshot.getEndDate()).isEqualTo(END_DATE);
        assertThat(snapshot.getStartTime()).isEqualTo(START_TIME);
        assertThat(snapshot.getEndTime()).isEqualTo(END_TIME);
        assertThat(snapshot.getTimezone()).isEqualTo(TZ);

        // Monday 2026-09-07 is within range and is Monday -> true
        assertThat(snapshot.occursOn(LocalDate.of(2026, 9, 7))).isTrue();
        // Wednesday 2026-09-09 is within range and is Wednesday -> true
        assertThat(snapshot.occursOn(LocalDate.of(2026, 9, 9))).isTrue();
        // Tuesday 2026-09-08 is not a configured weekday -> false
        assertThat(snapshot.occursOn(LocalDate.of(2026, 9, 8))).isFalse();
        // Before start date -> false
        assertThat(snapshot.occursOn(LocalDate.of(2026, 8, 31))).isFalse();
        // After end date -> false
        assertThat(snapshot.occursOn(LocalDate.of(2026, 10, 5))).isFalse();
        // Null date -> false
        assertThat(snapshot.occursOn(null)).isFalse();
    }

    @Test
    @DisplayName("RecurrenceRuleDefinitionSnapshot rejects invalid definitions")
    void testRecurrenceRuleDefinitionSnapshotValidation() {
        assertThatThrownBy(() -> new RecurrenceRuleDefinitionSnapshot(
                null, START_DATE, END_DATE, START_TIME, END_TIME, TZ))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RecurrenceRuleDefinitionSnapshot(
                Set.of(), START_DATE, END_DATE, START_TIME, END_TIME, TZ))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RecurrenceRuleDefinitionSnapshot(
                MON_WED, END_DATE, START_DATE, START_TIME, END_TIME, TZ))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RecurrenceRuleDefinitionSnapshot(
                MON_WED, START_DATE, END_DATE, END_TIME, START_TIME, TZ))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SeriesOccurrencePreviewItem enforces mandatory fields and non-negative counts")
    void testSeriesOccurrencePreviewItemValidation() {
        SessionScheduleSnapshot snap = new SessionScheduleSnapshot(
                1L, TZ, Instant.parse("2026-09-01T09:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"),
                Instant.parse("2026-09-01T09:00:00Z"), SessionLifecycleStatus.DRAFT);

        SeriesOccurrencePreviewItem item = new SeriesOccurrencePreviewItem(
                "key-1:2026-09-01", LocalDate.of(2026, 9, 1), SeriesOccurrenceCategory.RETAINED,
                10L, snap, snap, 5, 2);

        assertThat(item.occurrenceKey()).isEqualTo("key-1:2026-09-01");
        assertThat(item.originOccurrenceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(item.category()).isEqualTo(SeriesOccurrenceCategory.RETAINED);
        assertThat(item.sessionId()).isEqualTo(10L);
        assertThat(item.affectedAppointmentCount()).isEqualTo(5);
        assertThat(item.affectedWaitlistCount()).isEqualTo(2);

        assertThatThrownBy(() -> new SeriesOccurrencePreviewItem(
                null, LocalDate.of(2026, 9, 1), SeriesOccurrenceCategory.RETAINED, 10L, snap, snap, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SeriesOccurrencePreviewItem(
                "key", null, SeriesOccurrenceCategory.RETAINED, 10L, snap, snap, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SeriesOccurrencePreviewItem(
                "key", LocalDate.of(2026, 9, 1), null, 10L, snap, snap, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SeriesOccurrencePreviewItem(
                "key", LocalDate.of(2026, 9, 1), SeriesOccurrenceCategory.RETAINED, 10L, snap, snap, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SeriesOccurrencePreviewItem(
                "key", LocalDate.of(2026, 9, 1), SeriesOccurrenceCategory.RETAINED, 10L, snap, snap, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SeriesChangePreview validates invariants and protects list immutability")
    void testSeriesChangePreviewValidation() {
        RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                MON_WED, START_DATE, END_DATE, START_TIME, END_TIME, TZ);
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), START_DATE, END_DATE, START_TIME, END_TIME, TZ);

        SeriesChangePreview preview = new SeriesChangePreview(
                100L, currentDef, proposedDef, List.of(), 10, 3, "token123");

        assertThat(preview.ruleId()).isEqualTo(100L);
        assertThat(preview.currentRuleDefinition()).isEqualTo(currentDef);
        assertThat(preview.proposedRuleDefinition()).isEqualTo(proposedDef);
        assertThat(preview.occurrenceEntries()).isEmpty();
        assertThat(preview.totalAffectedAppointmentsCount()).isEqualTo(10);
        assertThat(preview.totalAffectedWaitlistCount()).isEqualTo(3);
        assertThat(preview.previewToken()).isEqualTo("token123");

        // Verify list immutability
        assertThatThrownBy(() -> preview.occurrenceEntries().add(null))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> new SeriesChangePreview(null, currentDef, proposedDef, List.of(), 0, 0, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeriesChangePreview(100L, null, proposedDef, List.of(), 0, 0, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeriesChangePreview(100L, currentDef, null, List.of(), 0, 0, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeriesChangePreview(100L, currentDef, proposedDef, null, 0, 0, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeriesChangePreview(100L, currentDef, proposedDef, List.of(), -1, 0, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeriesChangePreview(100L, currentDef, proposedDef, List.of(), 0, -1, "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeriesChangePreview(100L, currentDef, proposedDef, List.of(), 0, 0, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Series preview token generation is deterministic and sensitive to changes")
    void testSeriesPreviewTokenDeterminismAndSensitivity() {
        RecurrenceRuleDefinitionSnapshot currentDef = new RecurrenceRuleDefinitionSnapshot(
                MON_WED, START_DATE, END_DATE, START_TIME, END_TIME, TZ);
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), START_DATE, END_DATE, START_TIME, END_TIME, TZ);

        SessionScheduleSnapshot snap = new SessionScheduleSnapshot(
                1L, TZ, Instant.parse("2026-09-02T09:00:00Z"), Instant.parse("2026-09-02T17:00:00Z"),
                Instant.parse("2026-09-02T09:00:00Z"), SessionLifecycleStatus.SCHEDULED);

        SeriesOccurrencePreviewItem item1 = new SeriesOccurrencePreviewItem(
                "genKey:2026-09-02", LocalDate.of(2026, 9, 2), SeriesOccurrenceCategory.REMOVED,
                10L, snap, snap, 4, 1);

        String token1 = SessionSeriesChangeService.generateSeriesPreviewToken(
                100L, currentDef, proposedDef, List.of(item1), 4, 1);
        String token2 = SessionSeriesChangeService.generateSeriesPreviewToken(
                100L, currentDef, proposedDef, List.of(item1), 4, 1);

        // Deterministic
        assertThat(token1).isNotBlank();
        assertThat(token1).isEqualTo(token2);
        assertThat(token1).hasSize(64); // SHA-256 hex length

        // Change ruleId
        String tokenDiffRuleId = SessionSeriesChangeService.generateSeriesPreviewToken(
                101L, currentDef, proposedDef, List.of(item1), 4, 1);
        assertThat(tokenDiffRuleId).isNotEqualTo(token1);

        // Change proposed definition (e.g. time)
        RecurrenceRuleDefinitionSnapshot proposedDiffTime = new RecurrenceRuleDefinitionSnapshot(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), START_DATE, END_DATE, LocalTime.of(10, 0), END_TIME, TZ);
        String tokenDiffProposedTime = SessionSeriesChangeService.generateSeriesPreviewToken(
                100L, currentDef, proposedDiffTime, List.of(item1), 4, 1);
        assertThat(tokenDiffProposedTime).isNotEqualTo(token1);

        // Change occurrence category
        SeriesOccurrencePreviewItem itemDiffCat = new SeriesOccurrencePreviewItem(
                "genKey:2026-09-02", LocalDate.of(2026, 9, 2), SeriesOccurrenceCategory.PRESERVED_EXCEPTION,
                10L, snap, snap, 4, 1);
        String tokenDiffCat = SessionSeriesChangeService.generateSeriesPreviewToken(
                100L, currentDef, proposedDef, List.of(itemDiffCat), 4, 1);
        assertThat(tokenDiffCat).isNotEqualTo(token1);

        // Change impact count
        String tokenDiffImpact = SessionSeriesChangeService.generateSeriesPreviewToken(
                100L, currentDef, proposedDef, List.of(item1), 5, 1);
        assertThat(tokenDiffImpact).isNotEqualTo(token1);
    }
}
