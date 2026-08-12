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

        List<VaccinationSession> existingSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(ruleId);

        Instant now = Instant.now(clock);
        return calculatePreviewInternal(rule, campaign, center, existingSessions, proposedDefinition, now);
    }

    /**
     * Confirms and atomically executes a series-wide schedule change to a recurrence rule and its generated sessions.
     *
     * @param ruleId       ID of the target SessionRecurrenceRule
     * @param newWeekdays  Proposed new active days of the week
     * @param newStartDate Proposed new series start date
     * @param newEndDate   Proposed new series end date
     * @param newStartTime Proposed new session start time
     * @param newEndTime   Proposed new session end time
     * @param newTimezone  Proposed new schedule timezone
     * @param previewToken SHA-256 fingerprint token obtained from a prior preview
     * @param reason       Mandatory business justification for the change (max 500 chars)
     * @param actorId      Mandatory identifier of the user or system actor (max 64 chars)
     * @return Immutable {@link SeriesChangeResult}
     */
    @Transactional
    public SeriesChangeResult confirmSeriesChange(
            Long ruleId,
            Set<DayOfWeek> newWeekdays,
            LocalDate newStartDate,
            LocalDate newEndDate,
            LocalTime newStartTime,
            LocalTime newEndTime,
            String newTimezone,
            String previewToken,
            String reason,
            String actorId
    ) {
        RecurrenceRuleDefinitionSnapshot proposedDef = new RecurrenceRuleDefinitionSnapshot(
                newWeekdays, newStartDate, newEndDate, newStartTime, newEndTime, newTimezone);
        return confirmSeriesChange(ruleId, proposedDef, previewToken, reason, actorId);
    }

    /**
     * Confirms and atomically executes a series-wide schedule change to a recurrence rule and its generated sessions.
     *
     * @param ruleId             ID of the target SessionRecurrenceRule
     * @param proposedDefinition Snapshot of the proposed new recurrence rule definition
     * @param previewToken       SHA-256 fingerprint token obtained from a prior preview
     * @param reason             Mandatory business justification for the change (max 500 chars)
     * @param actorId            Mandatory identifier of the user or system actor (max 64 chars)
     * @return Immutable {@link SeriesChangeResult}
     */
    @Transactional
    public SeriesChangeResult confirmSeriesChange(
            Long ruleId,
            RecurrenceRuleDefinitionSnapshot proposedDefinition,
            String previewToken,
            String reason,
            String actorId
    ) {
        requireId(ruleId, "Session recurrence rule ID is required");
        if (proposedDefinition == null) {
            throw new IllegalArgumentException("Proposed recurrence rule definition is required");
        }
        if (previewToken == null || previewToken.isBlank()) {
            throw new IllegalArgumentException("Preview token is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        String trimmedReason = reason.trim();
        if (trimmedReason.length() > 500) {
            throw new IllegalArgumentException("Reason cannot exceed 500 characters");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("Actor ID is required");
        }
        String trimmedActorId = actorId.trim();
        if (trimmedActorId.length() > 64) {
            throw new IllegalArgumentException("Actor ID cannot exceed 64 characters");
        }

        // Step 1: Discover unlocked rule to obtain campaign and center IDs
        SessionRecurrenceRule unlockedRule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Session recurrence rule not found with ID: " + ruleId));

        Long campaignId = unlockedRule.getCampaign().getId();
        Long centerId = unlockedRule.getCenter().getId();

        // Step 2: Acquire deterministic write locks in lock order: campaign -> center -> rule -> sessions (by ID ASC)
        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination campaign not found with ID: " + campaignId));

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found with ID: " + centerId));

        SessionRecurrenceRule rule = ruleRepository.findByIdForUpdate(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Session recurrence rule not found with ID: " + ruleId));

        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Session recurrence rule must be ACTIVE to confirm changes");
        }

        if (entityManager != null) {
            entityManager.refresh(campaign);
            entityManager.refresh(center);
            entityManager.refresh(rule);
        }

        List<VaccinationSession> unlockedSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(ruleId);

        List<Long> sessionIdsToLock = unlockedSessions.stream()
                .map(VaccinationSession::getId)
                .sorted()
                .toList();

        List<VaccinationSession> lockedSessions = new ArrayList<>();
        for (Long sid : sessionIdsToLock) {
            VaccinationSession s = sessionRepository.findByIdForUpdate(sid)
                    .orElseThrow(() -> new IllegalArgumentException("Vaccination session not found with ID: " + sid));
            if (entityManager != null) {
                entityManager.refresh(s);
            }
            lockedSessions.add(s);
        }

        // Step 3: Re-evaluate preview under locks for stale token protection
        Instant now = Instant.now(clock);
        SeriesChangePreview preview = calculatePreviewInternal(rule, campaign, center, lockedSessions, proposedDefinition, now);

        if (!preview.previewToken().equals(previewToken.trim())) {
            throw new IllegalStateException("Preview is stale or session state has changed; please review the updated preview before confirming");
        }

        // Step 4: Update recurrence rule definition
        rule.updateDefinition(
                proposedDefinition.getWeekdays(),
                proposedDefinition.getStartDate(),
                proposedDefinition.getEndDate(),
                proposedDefinition.getStartTime(),
                proposedDefinition.getEndTime(),
                proposedDefinition.getTimezone()
        );
        ruleRepository.saveAndFlush(rule);

        // Step 5: Execute atomic occurrence mutations
        Map<LocalDate, VaccinationSession> existingByOriginDate = new HashMap<>();
        for (VaccinationSession s : lockedSessions) {
            existingByOriginDate.put(s.getOriginOccurrenceDate(), s);
        }

        List<VaccinationSession> sessionsToSave = new ArrayList<>();
        List<SessionChangeEvent> eventsToSave = new ArrayList<>();

        int rescheduledCount = 0;
        int cancelledCount = 0;
        int newCount = 0;
        int preservedCount = 0;
        int ineligibleCount = 0;

        for (SeriesOccurrencePreviewItem item : preview.occurrenceEntries()) {
            LocalDate date = item.originOccurrenceDate();
            SeriesOccurrenceCategory cat = item.category();
            VaccinationSession existing = existingByOriginDate.get(date);

            switch (cat) {
                case PRESERVED_EXCEPTION -> preservedCount++;
                case INELIGIBLE -> ineligibleCount++;
                case RETAINED -> {
                    if (existing == null) {
                        throw new IllegalStateException("Expected existing session for RETAINED occurrence date: " + date);
                    }
                    SessionScheduleSnapshot currentSnap = item.currentSnapshot();
                    SessionScheduleSnapshot proposedSnap = item.proposedSnapshot();

                    boolean scheduleShifted = !currentSnap.getStartAt().equals(proposedSnap.getStartAt())
                            || !currentSnap.getEndAt().equals(proposedSnap.getEndAt())
                            || !currentSnap.getBookingCloseAt().equals(proposedSnap.getBookingCloseAt());

                    if (scheduleShifted) {
                        SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(existing);
                        existing.reschedule(center, proposedSnap.getStartAt(), proposedSnap.getEndAt(), proposedSnap.getBookingCloseAt());
                        SessionScheduleSnapshot newSnapshot = SessionScheduleSnapshot.from(existing);
                        sessionsToSave.add(existing);

                        SessionChangeEvent event = new SessionChangeEvent(
                                existing,
                                SessionChangeType.SERIES_RESCHEDULED,
                                oldSnapshot,
                                newSnapshot,
                                trimmedReason,
                                item.affectedAppointmentCount(),
                                item.affectedWaitlistCount(),
                                trimmedActorId
                        );
                        eventsToSave.add(event);
                        rescheduledCount++;
                    }
                }
                case REMOVED -> {
                    if (existing == null) {
                        throw new IllegalStateException("Expected existing session for REMOVED occurrence date: " + date);
                    }
                    SessionScheduleSnapshot oldSnapshot = SessionScheduleSnapshot.from(existing);
                    existing.cancel();
                    SessionScheduleSnapshot newSnapshot = SessionScheduleSnapshot.from(existing);
                    sessionsToSave.add(existing);

                    SessionChangeEvent event = new SessionChangeEvent(
                            existing,
                            SessionChangeType.SERIES_CANCELLED,
                            oldSnapshot,
                            newSnapshot,
                            trimmedReason,
                            item.affectedAppointmentCount(),
                            item.affectedWaitlistCount(),
                            trimmedActorId
                    );
                    eventsToSave.add(event);
                    cancelledCount++;
                }
                case NEW -> {
                    SessionScheduleSnapshot proposedSnap = item.proposedSnapshot();
                    VaccinationSession newSession = new VaccinationSession(
                            campaign,
                            center,
                            rule,
                            item.occurrenceKey(),
                            date,
                            proposedSnap.getStartAt(),
                            proposedSnap.getEndAt(),
                            proposedSnap.getBookingCloseAt(),
                            center.getTimezone()
                    );
                    sessionsToSave.add(newSession);
                    newCount++;
                }
            }
        }

        if (!sessionsToSave.isEmpty()) {
            sessionRepository.saveAllAndFlush(sessionsToSave);
        }
        if (!eventsToSave.isEmpty()) {
            sessionChangeEventRepository.saveAllAndFlush(eventsToSave);
        }

        return new SeriesChangeResult(
                rule,
                preview,
                rescheduledCount,
                cancelledCount,
                newCount,
                preservedCount,
                ineligibleCount,
                eventsToSave.size()
        );
    }

    private SeriesChangePreview calculatePreviewInternal(
            SessionRecurrenceRule rule,
            VaccinationCampaign campaign,
            VaccinationCenter center,
            List<VaccinationSession> existingSessions,
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

        RecurrenceRuleDefinitionSnapshot currentDefinition = RecurrenceRuleDefinitionSnapshot.from(rule);

        ZoneId zoneId = ZoneId.of(center.getTimezone());
        ZoneRules zoneRules = zoneId.getRules();

        Map<LocalDate, VaccinationSession> existingByOriginDate = new HashMap<>();
        for (VaccinationSession s : existingSessions) {
            existingByOriginDate.put(s.getOriginOccurrenceDate(), s);
        }

        // Detect individually overridden occurrences via durable SessionChangeEvent history
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
