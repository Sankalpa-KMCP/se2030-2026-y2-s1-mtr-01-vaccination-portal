package com.se2030.vaccinationportal.campaign.session.recurrence.service;

import com.se2030.vaccinationportal.campaign.api.session.SessionImpactReader;


import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SeriesOccurrenceCategory;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeType;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.session.recurrence.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.session.recurrence.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.session.planning.validation.VaccinationSessionValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SessionSeriesPreviewCalculator previewCalculator;
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
            SessionSeriesPreviewCalculator previewCalculator,
            Clock clock
    ) {
        this.ruleRepository = ruleRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.sessionChangeEventRepository = sessionChangeEventRepository;
        this.previewCalculator = previewCalculator;
        this.clock = clock;
    }

    public SessionSeriesChangeService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            SessionSeriesPreviewCalculator previewCalculator
    ) {
        this(ruleRepository, campaignRepository, centerRepository, sessionRepository, sessionChangeEventRepository, previewCalculator, Clock.systemUTC());
    }

    public SessionSeriesChangeService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            SessionChangeEventRepository sessionChangeEventRepository,
            VaccinationSessionValidator validator,
            SessionImpactReader sessionImpactReader,
            Clock clock
    ) {
        this(
                ruleRepository,
                campaignRepository,
                centerRepository,
                sessionRepository,
                sessionChangeEventRepository,
                new SessionSeriesPreviewCalculator(validator, sessionImpactReader),
                clock
        );
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
                .orElseThrow(() -> new ResourceNotFoundException("Session recurrence rule not found with ID: " + ruleId));

        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Session recurrence rule must be ACTIVE to preview changes");
        }

        VaccinationCenter center = rule.getCenter();
        VaccinationCampaign campaign = rule.getCampaign();

        List<VaccinationSession> existingSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(ruleId);

        Set<Long> overriddenSessionIds = findOverriddenSessionIds(existingSessions);
        Instant now = Instant.now(clock);
        return previewCalculator.calculateSeriesChangePreview(
                rule, campaign, center, existingSessions, overriddenSessionIds, proposedDefinition, now);
    }

    /**
     * Previews a full-series cancellation of an active recurrence rule and its generated sessions.
     *
     * @param ruleId ID of the target SessionRecurrenceRule
     * @return Immutable {@link SeriesChangePreview}
     */
    public SeriesChangePreview previewSeriesCancellation(Long ruleId) {
        requireId(ruleId, "Session recurrence rule ID is required");

        SessionRecurrenceRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Session recurrence rule not found with ID: " + ruleId));

        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Session recurrence rule must be ACTIVE to preview cancellation");
        }

        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(rule.getTimezone())));
        if (today.isAfter(rule.getEndDate())) {
            throw new IllegalArgumentException("An expired session recurrence rule cannot be cancelled");
        }

        VaccinationCenter center = rule.getCenter();
        VaccinationCampaign campaign = rule.getCampaign();

        List<VaccinationSession> existingSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(ruleId);

        Set<Long> overriddenSessionIds = findOverriddenSessionIds(existingSessions);
        Instant now = Instant.now(clock);
        return previewCalculator.calculateCancellationPreview(
                rule, campaign, center, existingSessions, overriddenSessionIds, now);
    }

    /**
     * Confirms and atomically executes a full-series cancellation of an active recurrence rule and its generated sessions.
     *
     * @param ruleId       ID of the target SessionRecurrenceRule
     * @param previewToken SHA-256 fingerprint token obtained from a prior preview
     * @param reason       Mandatory business justification for the cancellation (max 500 chars)
     * @param actorId      Mandatory identifier of the user or system actor (max 64 chars)
     * @return Immutable {@link SeriesChangeResult}
     */
    @Transactional
    public SeriesChangeResult confirmSeriesCancellation(
            Long ruleId,
            String previewToken,
            String reason,
            String actorId
    ) {
        requireId(ruleId, "Session recurrence rule ID is required");
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
                .orElseThrow(() -> new ResourceNotFoundException("Session recurrence rule not found with ID: " + ruleId));

        Long campaignId = unlockedRule.getCampaign().getId();
        Long centerId = unlockedRule.getCenter().getId();

        // Step 2: Acquire deterministic write locks in lock order: campaign -> center -> rule -> sessions (by ID ASC)
        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination campaign not found with ID: " + campaignId));

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));

        SessionRecurrenceRule rule = ruleRepository.findByIdForUpdate(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Session recurrence rule not found with ID: " + ruleId));

        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Session recurrence rule must be ACTIVE to confirm cancellation");
        }

        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(rule.getTimezone())));
        if (today.isAfter(rule.getEndDate())) {
            throw new IllegalArgumentException("An expired session recurrence rule cannot be cancelled");
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
                    .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sid));
            if (entityManager != null) {
                entityManager.refresh(s);
            }
            lockedSessions.add(s);
        }

        // Step 3: Re-evaluate preview under locks for stale token protection
        Set<Long> overriddenSessionIds = findOverriddenSessionIds(lockedSessions);
        Instant now = Instant.now(clock);
        SeriesChangePreview preview = previewCalculator.calculateCancellationPreview(
                rule, campaign, center, lockedSessions, overriddenSessionIds, now);

        if (!preview.previewToken().equals(previewToken.trim())) {
            throw new IllegalStateException("Preview is stale or session state has changed; please review the updated preview before confirming");
        }

        // Step 4: Cancel recurrence rule
        rule.cancel();
        ruleRepository.saveAndFlush(rule);

        // Step 5: Execute atomic occurrence mutations
        Map<LocalDate, VaccinationSession> existingByOriginDate = new HashMap<>();
        for (VaccinationSession s : lockedSessions) {
            existingByOriginDate.put(s.getOriginOccurrenceDate(), s);
        }

        List<VaccinationSession> sessionsToSave = new ArrayList<>();
        List<SessionChangeEvent> eventsToSave = new ArrayList<>();

        int cancelledCount = 0;
        int preservedCount = 0;
        int ineligibleCount = 0;

        for (SeriesOccurrencePreviewItem item : preview.occurrenceEntries()) {
            LocalDate date = item.originOccurrenceDate();
            SeriesOccurrenceCategory cat = item.category();
            VaccinationSession existing = existingByOriginDate.get(date);

            switch (cat) {
                case PRESERVED_EXCEPTION -> preservedCount++;
                case INELIGIBLE -> ineligibleCount++;
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
                default -> throw new IllegalStateException("Unexpected occurrence category in series cancellation: " + cat);
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
                0,
                cancelledCount,
                0,
                preservedCount,
                ineligibleCount,
                eventsToSave.size()
        );
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
                .orElseThrow(() -> new ResourceNotFoundException("Session recurrence rule not found with ID: " + ruleId));

        Long campaignId = unlockedRule.getCampaign().getId();
        Long centerId = unlockedRule.getCenter().getId();

        // Step 2: Acquire deterministic write locks in lock order: campaign -> center -> rule -> sessions (by ID ASC)
        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination campaign not found with ID: " + campaignId));

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));

        SessionRecurrenceRule rule = ruleRepository.findByIdForUpdate(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Session recurrence rule not found with ID: " + ruleId));

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
                    .orElseThrow(() -> new ResourceNotFoundException("Vaccination session not found with ID: " + sid));
            if (entityManager != null) {
                entityManager.refresh(s);
            }
            lockedSessions.add(s);
        }

        // Step 3: Re-evaluate preview under locks for stale token protection
        Set<Long> overriddenSessionIds = findOverriddenSessionIds(lockedSessions);
        Instant now = Instant.now(clock);
        SeriesChangePreview preview = previewCalculator.calculateSeriesChangePreview(
                rule, campaign, center, lockedSessions, overriddenSessionIds, proposedDefinition, now);

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

    /**
     * Generates a deterministic SHA-256 fingerprint token for the series preview state.
     * Delegates to {@link SessionSeriesPreviewCalculator#generateSeriesPreviewToken}.
     */
    public static String generateSeriesPreviewToken(
            Long ruleId,
            RecurrenceRuleDefinitionSnapshot currentDef,
            RecurrenceRuleDefinitionSnapshot proposedDef,
            List<SeriesOccurrencePreviewItem> entries,
            int totalAffectedAppointments,
            int totalAffectedWaitlist
    ) {
        return SessionSeriesPreviewCalculator.generateSeriesPreviewToken(
                ruleId, currentDef, proposedDef, entries, totalAffectedAppointments, totalAffectedWaitlist);
    }

    private Set<Long> findOverriddenSessionIds(List<VaccinationSession> existingSessions) {
        if (existingSessions == null || existingSessions.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> sessionIds = existingSessions.stream().map(VaccinationSession::getId).toList();
        List<SessionChangeEvent> changeEvents = sessionChangeEventRepository.findBySessionIdIn(sessionIds);
        Set<Long> overriddenSessionIds = new HashSet<>();
        for (SessionChangeEvent event : changeEvents) {
            if (INDIVIDUAL_OVERRIDE_TYPES.contains(event.getChangeType())) {
                overriddenSessionIds.add(event.getSession().getId());
            }
        }
        return overriddenSessionIds;
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
