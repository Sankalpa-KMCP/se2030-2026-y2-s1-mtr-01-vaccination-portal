package com.se2030.vaccinationportal.campaign.session.recurrence.service;

import com.se2030.vaccinationportal.campaign.api.session.SessionImpactReader;


import com.se2030.vaccinationportal.campaign.session.recurrence.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.validation.VaccinationSessionValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Collaborator responsible for calculating recurrence schedule diffs, occurrence category classifications
 * (RETAINED, REMOVED, NEW, INELIGIBLE, PRESERVED_EXCEPTION), and deterministic SHA-256 fingerprint tokens
 * for session series preview and confirmation.
 *
 * <p>This component is repository-free and transaction-free. It delegates candidate schedule validation
 * to {@link VaccinationSessionValidator} and conditionally evaluates impact via {@link SessionImpactReader}
 * only when required for changed SCHEDULED sessions.</p>
 */
@Component
public class SessionSeriesPreviewCalculator {

    private final VaccinationSessionValidator validator;
    private final SessionImpactReader sessionImpactReader;

    @Autowired
    public SessionSeriesPreviewCalculator(
            VaccinationSessionValidator validator,
            @Autowired(required = false) SessionImpactReader sessionImpactReader
    ) {
        this.validator = validator;
        this.sessionImpactReader = sessionImpactReader;
    }

    public SessionSeriesPreviewCalculator(VaccinationSessionValidator validator) {
        this(validator, null);
    }

    /**
     * Calculates a series-wide schedule change preview comparing existing sessions with proposed recurrence definition.
     */
    public SeriesChangePreview calculateSeriesChangePreview(
            SessionRecurrenceRule rule,
            VaccinationCampaign campaign,
            VaccinationCenter center,
            List<VaccinationSession> existingSessions,
            Set<Long> overriddenSessionIds,
            RecurrenceRuleDefinitionSnapshot proposedDefinition,
            Instant now
    ) {
        Long ruleId = rule.getId();

        // Validate timezone compatibility with hosting center
        if (!center.getTimezone().equals(proposedDefinition.getTimezone())) {
            throw new IllegalArgumentException(
                    "Proposed recurrence timezone (" + proposedDefinition.getTimezone() +
                            ") does not match center scheduling timezone (" + center.getTimezone() + ")");
        }

        SessionRecurrenceRule.validateDateHorizon(proposedDefinition.getStartDate(), proposedDefinition.getEndDate());

        RecurrenceRuleDefinitionSnapshot currentDefinition = RecurrenceRuleDefinitionSnapshot.from(rule);

        ZoneId zoneId = ZoneId.of(center.getTimezone());
        ZoneRules zoneRules = zoneId.getRules();

        Map<LocalDate, VaccinationSession> existingByOriginDate = new HashMap<>();
        for (VaccinationSession s : existingSessions) {
            existingByOriginDate.put(s.getOriginOccurrenceDate(), s);
        }

        Set<Long> effectiveOverriddenIds = (overriddenSessionIds != null) ? overriddenSessionIds : Collections.emptySet();

        // Collect sorted union of all relevant candidate and existing dates
        Set<LocalDate> allDates = new TreeSet<>(existingByOriginDate.keySet());
        for (LocalDate d = proposedDefinition.getStartDate(); !d.isAfter(proposedDefinition.getEndDate()); d = d.plusDays(1)) {
            if (proposedDefinition.getWeekdays().contains(d.getDayOfWeek())) {
                allDates.add(d);
            }
        }

        List<SeriesOccurrencePreviewItem> entries = new ArrayList<>();
        int totalAffectedAppointments = 0;
        int totalAffectedWaitlist = 0;

        for (LocalDate date : allDates) {
            String occurrenceKey = rule.getGenerationKey() + ":" + date;
            VaccinationSession existing = existingByOriginDate.get(date);

            if (existing != null) {
                SessionScheduleSnapshot currentSnapshot = SessionScheduleSnapshot.from(existing);

                // Case A: Preserved individual exception
                if (effectiveOverriddenIds.contains(existing.getId())) {
                    entries.add(new SeriesOccurrencePreviewItem(
                            occurrenceKey,
                            date,
                            SeriesOccurrenceCategory.PRESERVED_EXCEPTION,
                            existing.getId(),
                            currentSnapshot,
                            currentSnapshot,
                            0,
                            0
                    ));
                    continue;
                }

                // Case B: Ineligible (past/elapsed or terminal/non-mutable lifecycle)
                boolean isElapsed = !existing.getStartAt().isAfter(now);
                boolean isTerminalOrSpecial = existing.getLifecycleStatus() == SessionLifecycleStatus.IN_PROGRESS
                        || existing.getLifecycleStatus() == SessionLifecycleStatus.COMPLETED
                        || existing.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED;

                if (isElapsed || isTerminalOrSpecial) {
                    entries.add(new SeriesOccurrencePreviewItem(
                            occurrenceKey,
                            date,
                            SeriesOccurrenceCategory.INELIGIBLE,
                            existing.getId(),
                            currentSnapshot,
                            currentSnapshot,
                            0,
                            0
                    ));
                    continue;
                }

                // Case C: Eligible future DRAFT or SCHEDULED session
                boolean matchesNewRecurrence = proposedDefinition.occursOn(date);

                if (matchesNewRecurrence) {
                    // RETAINED
                    LocalDateTime localStart = LocalDateTime.of(date, proposedDefinition.getStartTime());
                    LocalDateTime localEnd = LocalDateTime.of(date, proposedDefinition.getEndTime());

                    Instant proposedStartAt = resolveLocalToInstant(localStart, zoneRules, zoneId, "start");
                    Instant proposedEndAt = resolveLocalToInstant(localEnd, zoneRules, zoneId, "end");
                    Instant proposedBookingCloseAt = proposedStartAt;

                    validator.validateSessionSchedule(campaign, center, proposedStartAt, proposedEndAt, proposedBookingCloseAt);

                    SessionScheduleSnapshot proposedSnapshot = new SessionScheduleSnapshot(
                            center.getId(),
                            center.getTimezone(),
                            proposedStartAt,
                            proposedEndAt,
                            proposedBookingCloseAt,
                            existing.getLifecycleStatus()
                    );

                    boolean scheduleShifted = !currentSnapshot.getStartAt().equals(proposedStartAt)
                            || !currentSnapshot.getEndAt().equals(proposedEndAt)
                            || !currentSnapshot.getBookingCloseAt().equals(proposedBookingCloseAt)
                            || !currentSnapshot.getCenterId().equals(center.getId())
                            || !currentSnapshot.getScheduleTimezone().equals(center.getTimezone());

                    int affectedAppts = 0;
                    int affectedWait = 0;
                    if (scheduleShifted && existing.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
                        if (sessionImpactReader == null) {
                            throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
                        }
                        var impact = sessionImpactReader.getSessionImpact(existing.getId());
                        if (impact == null) {
                            throw new IllegalStateException("Session impact reader returned null summary for session ID: " + existing.getId());
                        }
                        affectedAppts = impact.affectedAppointmentCount();
                        affectedWait = impact.affectedWaitlistCount();
                    }

                    totalAffectedAppointments += affectedAppts;
                    totalAffectedWaitlist += affectedWait;

                    entries.add(new SeriesOccurrencePreviewItem(
                            occurrenceKey,
                            date,
                            SeriesOccurrenceCategory.RETAINED,
                            existing.getId(),
                            currentSnapshot,
                            proposedSnapshot,
                            affectedAppts,
                            affectedWait
                    ));
                } else {
                    // REMOVED
                    int affectedAppts = 0;
                    int affectedWait = 0;
                    if (existing.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
                        if (sessionImpactReader == null) {
                            throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
                        }
                        var impact = sessionImpactReader.getSessionImpact(existing.getId());
                        if (impact == null) {
                            throw new IllegalStateException("Session impact reader returned null summary for session ID: " + existing.getId());
                        }
                        affectedAppts = impact.affectedAppointmentCount();
                        affectedWait = impact.affectedWaitlistCount();
                    }

                    totalAffectedAppointments += affectedAppts;
                    totalAffectedWaitlist += affectedWait;

                    SessionScheduleSnapshot proposedSnapshot = new SessionScheduleSnapshot(
                            center.getId(),
                            center.getTimezone(),
                            existing.getStartAt(),
                            existing.getEndAt(),
                            existing.getBookingCloseAt(),
                            SessionLifecycleStatus.CANCELLED
                    );

                    entries.add(new SeriesOccurrencePreviewItem(
                            occurrenceKey,
                            date,
                            SeriesOccurrenceCategory.REMOVED,
                            existing.getId(),
                            currentSnapshot,
                            proposedSnapshot,
                            affectedAppts,
                            affectedWait
                    ));
                }
            } else {
                // Case D: No existing session on this date
                boolean matchesNewRecurrence = proposedDefinition.occursOn(date);
                if (matchesNewRecurrence) {
                    LocalDateTime localStart = LocalDateTime.of(date, proposedDefinition.getStartTime());
                    LocalDateTime localEnd = LocalDateTime.of(date, proposedDefinition.getEndTime());

                    Instant proposedStartAt = resolveLocalToInstant(localStart, zoneRules, zoneId, "start");
                    Instant proposedEndAt = resolveLocalToInstant(localEnd, zoneRules, zoneId, "end");
                    Instant proposedBookingCloseAt = proposedStartAt;

                    if (!proposedStartAt.isAfter(now)) {
                        throw new IllegalArgumentException(
                                "Proposed series change cannot introduce past occurrence on " + date +
                                        " with start time " + proposedStartAt + " at or before current time " + now);
                    }

                    // NEW occurrence
                    validator.validateSessionSchedule(campaign, center, proposedStartAt, proposedEndAt, proposedBookingCloseAt);

                    SessionScheduleSnapshot proposedSnapshot = new SessionScheduleSnapshot(
                            center.getId(),
                            center.getTimezone(),
                            proposedStartAt,
                            proposedEndAt,
                            proposedBookingCloseAt,
                            SessionLifecycleStatus.DRAFT
                    );

                    entries.add(new SeriesOccurrencePreviewItem(
                            occurrenceKey,
                            date,
                            SeriesOccurrenceCategory.NEW,
                            null,
                            null,
                            proposedSnapshot,
                            0,
                            0
                    ));
                }
            }
        }

        String previewToken = generateSeriesPreviewToken(
                ruleId,
                currentDefinition,
                proposedDefinition,
                entries,
                totalAffectedAppointments,
                totalAffectedWaitlist
        );

        return new SeriesChangePreview(
                ruleId,
                currentDefinition,
                proposedDefinition,
                entries,
                totalAffectedAppointments,
                totalAffectedWaitlist,
                previewToken
        );
    }

    /**
     * Calculates a series cancellation preview comparing existing sessions with cancellation state.
     */
    public SeriesChangePreview calculateCancellationPreview(
            SessionRecurrenceRule rule,
            VaccinationCampaign campaign,
            VaccinationCenter center,
            List<VaccinationSession> existingSessions,
            Set<Long> overriddenSessionIds,
            Instant now
    ) {
        Long ruleId = rule.getId();
        RecurrenceRuleDefinitionSnapshot currentDefinition = RecurrenceRuleDefinitionSnapshot.from(rule);
        RecurrenceRuleDefinitionSnapshot proposedDefinition = currentDefinition;

        Set<Long> effectiveOverriddenIds = (overriddenSessionIds != null) ? overriddenSessionIds : Collections.emptySet();

        List<SeriesOccurrencePreviewItem> entries = new ArrayList<>();
        int totalAffectedAppointments = 0;
        int totalAffectedWaitlist = 0;

        for (VaccinationSession existing : existingSessions) {
            LocalDate date = existing.getOriginOccurrenceDate();
            String occurrenceKey = rule.getGenerationKey() + ":" + date;
            SessionScheduleSnapshot currentSnapshot = SessionScheduleSnapshot.from(existing);

            // Case A: Preserved individual exception
            if (effectiveOverriddenIds.contains(existing.getId())) {
                entries.add(new SeriesOccurrencePreviewItem(
                        occurrenceKey,
                        date,
                        SeriesOccurrenceCategory.PRESERVED_EXCEPTION,
                        existing.getId(),
                        currentSnapshot,
                        currentSnapshot,
                        0,
                        0
                ));
                continue;
            }

            // Case B: Ineligible (past/elapsed or terminal/non-mutable lifecycle)
            boolean isElapsed = !existing.getStartAt().isAfter(now);
            boolean isTerminalOrSpecial = existing.getLifecycleStatus() == SessionLifecycleStatus.IN_PROGRESS
                    || existing.getLifecycleStatus() == SessionLifecycleStatus.COMPLETED
                    || existing.getLifecycleStatus() == SessionLifecycleStatus.CANCELLED;

            if (isElapsed || isTerminalOrSpecial) {
                entries.add(new SeriesOccurrencePreviewItem(
                        occurrenceKey,
                        date,
                        SeriesOccurrenceCategory.INELIGIBLE,
                        existing.getId(),
                        currentSnapshot,
                        currentSnapshot,
                        0,
                        0
                ));
                continue;
            }

            // Case C: Eligible future DRAFT or SCHEDULED session -> REMOVED
            int affectedAppts = 0;
            int affectedWait = 0;
            if (existing.getLifecycleStatus() == SessionLifecycleStatus.SCHEDULED) {
                if (sessionImpactReader == null) {
                    throw new IllegalStateException("Session impact reader is not available to assess SCHEDULED session changes");
                }
                var impact = sessionImpactReader.getSessionImpact(existing.getId());
                if (impact == null) {
                    throw new IllegalStateException("Session impact reader returned null summary for session ID: " + existing.getId());
                }
                affectedAppts = impact.affectedAppointmentCount();
                affectedWait = impact.affectedWaitlistCount();
            }

            totalAffectedAppointments += affectedAppts;
            totalAffectedWaitlist += affectedWait;

            SessionScheduleSnapshot proposedSnapshot = new SessionScheduleSnapshot(
                    center.getId(),
                    center.getTimezone(),
                    existing.getStartAt(),
                    existing.getEndAt(),
                    existing.getBookingCloseAt(),
                    SessionLifecycleStatus.CANCELLED
            );

            entries.add(new SeriesOccurrencePreviewItem(
                    occurrenceKey,
                    date,
                    SeriesOccurrenceCategory.REMOVED,
                    existing.getId(),
                    currentSnapshot,
                    proposedSnapshot,
                    affectedAppts,
                    affectedWait
            ));
        }

        String previewToken = generateSeriesPreviewToken(
                ruleId,
                currentDefinition,
                proposedDefinition,
                entries,
                totalAffectedAppointments,
                totalAffectedWaitlist
        );

        return new SeriesChangePreview(
                ruleId,
                currentDefinition,
                proposedDefinition,
                entries,
                totalAffectedAppointments,
                totalAffectedWaitlist,
                previewToken
        );
    }

    /**
     * Generates a deterministic SHA-256 fingerprint token for the series preview state.
     */
    public static String generateSeriesPreviewToken(
            Long ruleId,
            RecurrenceRuleDefinitionSnapshot currentDef,
            RecurrenceRuleDefinitionSnapshot proposedDef,
            List<SeriesOccurrencePreviewItem> entries,
            int totalAffectedAppointments,
            int totalAffectedWaitlist
    ) {
        if (ruleId == null || currentDef == null || proposedDef == null || entries == null) {
            throw new IllegalArgumentException("Parameters for series preview token generation cannot be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ruleId).append("|");
        appendDefinition(sb, currentDef);
        sb.append("|");
        appendDefinition(sb, proposedDef);
        sb.append("|");
        sb.append(totalAffectedAppointments).append("|");
        sb.append(totalAffectedWaitlist).append("|");

        for (SeriesOccurrencePreviewItem item : entries) {
            sb.append(item.occurrenceKey()).append(";")
                    .append(item.originOccurrenceDate()).append(";")
                    .append(item.category().name()).append(";")
                    .append(item.sessionId() != null ? item.sessionId() : "null").append(";");
            appendSnapshot(sb, item.currentSnapshot());
            sb.append(";");
            appendSnapshot(sb, item.proposedSnapshot());
            sb.append(";")
                    .append(item.affectedAppointmentCount()).append(";")
                    .append(item.affectedWaitlistCount()).append("|");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static void appendDefinition(StringBuilder sb, RecurrenceRuleDefinitionSnapshot def) {
        String sortedDays = def.getWeekdays().stream()
                .sorted()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
        sb.append(sortedDays).append(":")
                .append(def.getStartDate()).append(":")
                .append(def.getEndDate()).append(":")
                .append(def.getStartTime()).append(":")
                .append(def.getEndTime()).append(":")
                .append(def.getTimezone());
    }

    private static void appendSnapshot(StringBuilder sb, SessionScheduleSnapshot snapshot) {
        if (snapshot == null) {
            sb.append("null");
        } else {
            sb.append(snapshot.getCenterId()).append(",")
                    .append(snapshot.getScheduleTimezone()).append(",")
                    .append(snapshot.getStartAt()).append(",")
                    .append(snapshot.getEndAt()).append(",")
                    .append(snapshot.getBookingCloseAt()).append(",")
                    .append(snapshot.getLifecycleStatus());
        }
    }

    private static Instant resolveLocalToInstant(
            LocalDateTime ldt,
            ZoneRules rules,
            ZoneId zoneId,
            String timeRole
    ) {
        List<ZoneOffset> validOffsets = rules.getValidOffsets(ldt);
        if (validOffsets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Session " + timeRole + " time " + ldt + " does not exist in timezone " + zoneId.getId() + " due to DST transition gap");
        }
        ZoneOffset chosenOffset = validOffsets.get(0);
        return ldt.toInstant(chosenOffset);
    }
}
