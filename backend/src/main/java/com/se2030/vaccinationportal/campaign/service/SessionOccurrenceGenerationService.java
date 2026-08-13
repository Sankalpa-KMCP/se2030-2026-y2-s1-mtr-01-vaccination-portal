package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.validation.VaccinationSessionValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for materializing concrete DRAFT VaccinationSession occurrences
 * from an ACTIVE SessionRecurrenceRule.
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Deterministic global lock ordering (campaign -&gt; center -&gt; rule) with post-lock entity refresh</li>
 *   <li>Authoritative ACTIVE rule status and strict rule/center timezone compatibility</li>
 *   <li>Deterministic rule-date enumeration and DST-safe local date/time conversion</li>
 *   <li>Stable occurrence identity ({@code <generationKey>:<originOccurrenceDate>})</li>
 *   <li>Shared {@link VaccinationSessionValidator} reuse for concrete schedule validity</li>
 *   <li>Idempotent repeated generation and all-or-nothing batch persistence</li>
 * </ul>
 */
@Service
@Transactional
public class SessionOccurrenceGenerationService {

    private final SessionRecurrenceRuleRepository ruleRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final VaccinationSessionValidator validator;
    @PersistenceContext
    private EntityManager entityManager;

    public SessionOccurrenceGenerationService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            VaccinationSessionValidator validator
    ) {
        this.ruleRepository = ruleRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.validator = validator;
    }

    /**
     * Materializes all concrete session occurrences for a rule across its entire configured date range.
     *
     * @param ruleId ID of the target SessionRecurrenceRule
     * @return RecurrenceGenerationResult with created count, existing count, and materialized sessions
     * @throws IllegalArgumentException if rule not found, rule inactive, timezone mismatched, or validation fails
     */
    public RecurrenceGenerationResult generateOccurrences(Long ruleId) {
        requireId(ruleId, "Session recurrence rule ID is required");
        return generateOccurrencesInternal(ruleId, null, null);
    }

    /**
     * Materializes concrete session occurrences for a rule within a specified inclusive date range.
     *
     * @param ruleId             ID of the target SessionRecurrenceRule
     * @param requestedStartDate Inclusive start of requested generation window
     * @param requestedEndDate   Inclusive end of requested generation window
     * @return RecurrenceGenerationResult with created count, existing count, and materialized sessions
     * @throws IllegalArgumentException if parameters or business rules are violated
     */
    public RecurrenceGenerationResult generateOccurrences(
            Long ruleId,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate
    ) {
        requireId(ruleId, "Session recurrence rule ID is required");
        if (requestedStartDate == null) {
            throw new IllegalArgumentException("Requested start date is required");
        }
        if (requestedEndDate == null) {
            throw new IllegalArgumentException("Requested end date is required");
        }
        if (requestedEndDate.isBefore(requestedStartDate)) {
            throw new IllegalArgumentException("Requested start date must be on or before requested end date");
        }
        return generateOccurrencesInternal(ruleId, requestedStartDate, requestedEndDate);
    }

    private RecurrenceGenerationResult generateOccurrencesInternal(
            Long ruleId,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate
    ) {
        // Step 1: Discover routing identifiers via non-locking read
        SessionRecurrenceRule routingRule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Session recurrence rule not found with ID: " + ruleId));
        Long campaignId = routingRule.getCampaign().getId();
        Long centerId = routingRule.getCenter().getId();

        // Step 2: Acquire pessimistic write lock on campaign and refresh
        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }
        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalArgumentException(
                    "Cannot generate session occurrences for operationally closed campaign with ID: " + campaignId);
        }

        // Step 3: Acquire pessimistic write lock on center and refresh
        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }

        // Step 4: Acquire pessimistic write lock on recurrence rule and refresh
        SessionRecurrenceRule rule = ruleRepository.findByIdForUpdate(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Session recurrence rule not found with ID: " + ruleId));
        if (entityManager != null) {
            entityManager.refresh(rule);
        }

        // Step 5: Defensively verify authoritative parent relationships
        if (!rule.getCampaign().getId().equals(campaignId) || !rule.getCenter().getId().equals(centerId)) {
            throw new IllegalStateException("Authoritative rule parent relationship changed concurrently");
        }

        // Step 6: Verify rule is ACTIVE
        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Session recurrence rule must be ACTIVE to generate occurrences");
        }

        // Step 7: Verify strict rule/center timezone compatibility
        String ruleTimezone = rule.getTimezone();
        String centerTimezone = center.getTimezone();
        if (!ruleTimezone.equals(centerTimezone)) {
            throw new IllegalArgumentException(
                    "Rule timezone (" + ruleTimezone + ") does not match center scheduling timezone (" + centerTimezone + ")");
        }

        // Step 8: Compute effective generation window
        LocalDate effectiveStart;
        LocalDate effectiveEnd;
        if (requestedStartDate != null && requestedEndDate != null) {
            effectiveStart = requestedStartDate.isAfter(rule.getStartDate()) ? requestedStartDate : rule.getStartDate();
            effectiveEnd = requestedEndDate.isBefore(rule.getEndDate()) ? requestedEndDate : rule.getEndDate();
        } else {
            effectiveStart = rule.getStartDate();
            effectiveEnd = rule.getEndDate();
        }

        if (effectiveStart.isAfter(effectiveEnd)) {
            return new RecurrenceGenerationResult(0, 0, List.of());
        }

        // Step 9: Enumerate candidate recurrence dates in ascending order
        Set<DayOfWeek> weekdays = rule.getWeekdays();
        List<LocalDate> candidateDates = new ArrayList<>();
        for (LocalDate d = effectiveStart; !d.isAfter(effectiveEnd); d = d.plusDays(1)) {
            if (weekdays.contains(d.getDayOfWeek())) {
                candidateDates.add(d);
            }
        }

        if (candidateDates.isEmpty()) {
            return new RecurrenceGenerationResult(0, 0, List.of());
        }

        // Step 10: Load existing occurrences for the rule and index by occurrenceKey
        List<VaccinationSession> existingRuleSessions =
                sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        Map<String, VaccinationSession> existingByKey = new HashMap<>();
        for (VaccinationSession s : existingRuleSessions) {
            existingByKey.put(s.getOccurrenceKey(), s);
        }

        // Step 11: Validate existing provenance and construct missing candidates
        ZoneId zoneId = ZoneId.of(ruleTimezone);
        ZoneRules zoneRules = zoneId.getRules();
        List<VaccinationSession> newSessions = new ArrayList<>();
        Map<String, VaccinationSession> createdByKey = new HashMap<>();

        for (LocalDate date : candidateDates) {
            String occurrenceKey = rule.getGenerationKey() + ":" + date;
            VaccinationSession existing = existingByKey.get(occurrenceKey);

            if (existing != null) {
                // Defensively verify provenance consistency
                if (existing.getRecurrenceRule() == null || !existing.getRecurrenceRule().getId().equals(rule.getId())) {
                    throw new IllegalStateException("Existing session occurrence rule provenance mismatch for key: " + occurrenceKey);
                }
                if (existing.getOriginOccurrenceDate() == null || !existing.getOriginOccurrenceDate().equals(date)) {
                    throw new IllegalStateException("Existing session origin occurrence date mismatch for key: " + occurrenceKey);
                }
                if (existing.getCampaign() == null || !existing.getCampaign().getId().equals(campaign.getId())) {
                    throw new IllegalStateException("Existing session campaign mismatch for key: " + occurrenceKey);
                }
                if (existing.getCenter() == null) {
                    throw new IllegalStateException("Existing session center missing for key: " + occurrenceKey);
                }
            } else {
                LocalDateTime localStart = LocalDateTime.of(date, rule.getStartTime());
                LocalDateTime localEnd = LocalDateTime.of(date, rule.getEndTime());

                Instant startAt = resolveLocalToInstant(localStart, zoneRules, zoneId, "start");
                Instant endAt = resolveLocalToInstant(localEnd, zoneRules, zoneId, "end");

                if (!startAt.isBefore(endAt)) {
                    throw new IllegalArgumentException(
                            "Session start instant must be before session end instant on " + date);
                }

                // Authoritative decision: bookingCloseAt is exactly startAt
                Instant bookingCloseAt = startAt;

                validator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt);

                VaccinationSession newSession = new VaccinationSession(
                        campaign,
                        center,
                        rule,
                        occurrenceKey,
                        date,
                        startAt,
                        endAt,
                        bookingCloseAt,
                        ruleTimezone
                );
                newSessions.add(newSession);
                createdByKey.put(occurrenceKey, newSession);
            }
        }

        // Step 12: Atomic batch persistence for all missing candidates
        if (!newSessions.isEmpty()) {
            sessionRepository.saveAll(newSessions);
            sessionRepository.flush();
        }

        // Step 13: Assemble full result in originOccurrenceDate ASC order
        List<VaccinationSession> allMaterialized = new ArrayList<>();
        for (LocalDate date : candidateDates) {
            String occurrenceKey = rule.getGenerationKey() + ":" + date;
            VaccinationSession session = existingByKey.containsKey(occurrenceKey)
                    ? existingByKey.get(occurrenceKey)
                    : createdByKey.get(occurrenceKey);
            allMaterialized.add(session);
        }
        allMaterialized.sort(Comparator.comparing(VaccinationSession::getOriginOccurrenceDate));

        return new RecurrenceGenerationResult(
                newSessions.size(),
                candidateDates.size() - newSessions.size(),
                allMaterialized
        );
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
        // If 1 valid offset, use it; if ambiguous (overlap), validOffsets.get(0) deterministically selects earlier offset
        ZoneOffset chosenOffset = validOffsets.get(0);
        return ldt.toInstant(chosenOffset);
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
