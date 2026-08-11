package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calculating, previewing, and orchestrating series-wide changes across
 * {@link SessionRecurrenceRule} definitions and their generated {@link VaccinationSession} occurrences.
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Preservation of individually overridden occurrences as exceptions</li>
 *   <li>Read-only impact evaluation with real {@link SessionImpactReader} queries for SCHEDULED sessions</li>
 *   <li>Candidate schedule validation using shared {@link VaccinationSessionValidator}</li>
 *   <li>Deterministic SHA-256 fingerprinting for stale-preview protection</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SessionSeriesChangeService {

    private static final Set<SessionChangeType> INDIVIDUAL_OVERRIDE_TYPES = Collections.unmodifiableSet(EnumSet.of(
            SessionChangeType.RESCHEDULED,
            SessionChangeType.CENTER_TRANSFERRED,
            SessionChangeType.CANCELLED
    ));

    private final SessionRecurrenceRuleRepository ruleRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final SessionChangeEventRepository sessionChangeEventRepository;
    private final VaccinationSessionValidator validator;
    private final SessionImpactReader sessionImpactReader;
    private final Clock clock;
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public SessionSeriesChangeService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator,
            @Autowired(required = false) SessionImpactReader sessionImpactReader,
            Clock clock
    ) {
        this.ruleRepository = ruleRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.sessionChangeEventRepository = sessionChangeEventRepository;
        this.validator = validator;
        this.sessionImpactReader = sessionImpactReader;
        this.clock = clock;
    }

    public SessionSeriesChangeService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator,
            Clock clock
    ) {
        this(ruleRepository, campaignRepository, centerRepository, sessionRepository, sessionChangeEventRepository, validator, null, clock);
    }

    public SessionSeriesChangeService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator
    ) {
        this(ruleRepository, campaignRepository, centerRepository, sessionRepository, sessionChangeEventRepository, validator, null, Clock.systemUTC());
    }

    /**
     * Previews a proposed series-wide change to a recurrence rule definition and its generated sessions.
     *
     * @param ruleId       ID of the target SessionRecurrenceRule
     * @param newWeekdays  Proposed new active days of the week
     * @param newStartDate Proposed new series start date
     * @param newEndDate   Proposed new series end date
     * @param newStartTime Proposed new session start time
     * @param newEndTime   Proposed new session end time
     * @param newTimezone  Proposed new schedule timezone (must match center scheduling timezone)
     * @return Immutable {@link SeriesChangePreview}
     */
    public SeriesChangePreview previewSeriesChange(
            Long ruleId,
            Set<DayOfWeek> newWeekdays,
            LocalDate newStartDate,
            LocalDate newEndDate,
            LocalTime newStartTime,
            LocalTime newEndTime,
            String newTimezone
    ) {
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                newWeekdays, newStartDate, newEndDate, newStartTime, newEndTime, newTimezone);
        return previewSeriesChange(ruleId, proposedDef);
    }

    /**
     * Previews a proposed series-wide change against a target recurrence rule definition snapshot.
     *
     * @param ruleId             ID of the target SessionRecurrenceRule
     * @param proposedDefinition Snapshot of the proposed recurrence rule definition
     * @return Immutable {@link SeriesChangePreview}
     */
    public SeriesChangePreview previewSeriesChange(
            Long ruleId,
            RecurrenceRuleDefinitionSnapshot proposedDefinition
    ) {
        requireId(ruleId, "Session recurrence rule ID is required");
        if (proposedDefinition == null) {
            throw new IllegalArgumentException("Proposed recurrence rule definition is required");
        }

        // Step 1: Discover authoritative recurrence rule and parent entities
        SessionRecurrenceRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Session recurrence rule not found with ID: " + ruleId));

        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Session recurrence rule must be ACTIVE to preview changes");
        }

        VaccinationCenter center = rule.getCenter();
        VaccinationCampaign campaign = rule.getCampaign();

        // Step 2: Validate timezone compatibility with hosting center
        if (!center.getTimezone().equals(proposedDefinition.getTimezone())) {
            throw new IllegalArgumentException(
                    "Proposed recurrence timezone (" + proposedDefinition.getTimezone() +
                            ") does not match center scheduling timezone (" + center.getTimezone() + ")");
        }

        RecurrenceRuleDefinitionSnapshot currentDefinition = RecurrenceRuleDefinitionSnapshot.from(rule);

        // Step 3: Determine evaluation time horizon
        Instant now = Instant.now(clock);
        ZoneId zoneId = ZoneId.of(center.getTimezone());
        ZoneRules zoneRules = zoneId.getRules();

        // Step 4: Load existing sessions for the recurrence rule
        List<VaccinationSession> existingSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(ruleId);
        Map<LocalDate, VaccinationSession> existingByOriginDate = new HashMap<>();
        for (VaccinationSession s : existingSessions) {
            existingByOriginDate.put(s.getOriginOccurrenceDate(), s);
        }

        // Step 5: Detect individually overridden occurrences via durable SessionChangeEvent history
        Set<Long> overriddenSessionIds = new HashSet<>();
        if (!existingSessions.isEmpty()) {
            List<Long> sessionIds = existingSessions.stream().map(VaccinationSession::getId).toList();
            List<SessionChangeEvent> changeEvents = sessionChangeEventRepository.findBySessionIdIn(sessionIds);
            for (SessionChangeEvent event : changeEvents) {
                if (INDIVIDUAL_OVERRIDE_TYPES.contains(event.getChangeType())) {
                    overriddenSessionIds.add(event.getSession().getId());
                }
            }
        }

        // Step 6: Collect sorted union of all relevant candidate and existing dates
        Set<LocalDate> allDates = new TreeSet<>(existingByOriginDate.keySet());
        for (LocalDate d = proposedDefinition.getStartDate(); !d.isAfter(proposedDefinition.getEndDate()); d = d.plusDays(1)) {
            if (proposedDefinition.getWeekdays().contains(d.getDayOfWeek())) {
                allDates.add(d);
            }
        }

        // Step 7: Classify each occurrence and compute impact counts
        List<SeriesOccurrencePreviewItem> entries = new ArrayList<>();
        int totalAffectedAppointments = 0;
        int totalAffectedWaitlist = 0;

        for (LocalDate date : allDates) {
            String occurrenceKey = rule.getGenerationKey() + ":" + date;
            VaccinationSession existing = existingByOriginDate.get(date);

            if (existing != null) {
                SessionScheduleSnapshot currentSnapshot = SessionScheduleSnapshot.from(existing);

                // Case A: Preserved individual exception
                if (overriddenSessionIds.contains(existing.getId())) {
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
                            proposedStartAt,
                            proposedEndAt,
                            proposedBookingCloseAt,
                            existing.getLifecycleStatus()
                    );

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

                    if (proposedStartAt.isAfter(now)) {
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
                    } else {
                        // Candidate date in the past -> INELIGIBLE
                        entries.add(new SeriesOccurrencePreviewItem(
                                occurrenceKey,
                                date,
                                SeriesOccurrenceCategory.INELIGIBLE,
                                null,
                                null,
                                null,
                                0,
                                0
                        ));
                    }
                }
            }
        }

        // Step 8: Compute deterministic SHA-256 preview token
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

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
