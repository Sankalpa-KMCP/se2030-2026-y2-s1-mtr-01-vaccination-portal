package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages retained bounded recurrence definitions without generating concrete sessions. */
@Service
@Transactional
public class SessionRecurrenceRuleService {

    private final SessionRecurrenceRuleRepository ruleRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public SessionRecurrenceRuleService(
            SessionRecurrenceRuleRepository ruleRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            Clock clock
    ) {
        this.ruleRepository = ruleRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    public SessionRecurrenceRule createRule(
            Long campaignId,
            Long centerId,
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(centerId, "Center ID is required");
        SessionRecurrenceRule.validateDefinition(
                weekdays, startDate, endDate, startTime, endTime, timezone);

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }
        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalArgumentException(
                    "Cannot create session recurrence rule for operationally closed campaign with ID: " + campaignId);
        }

        VaccinationCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vaccination center not found with ID: " + centerId));

        return ruleRepository.save(new SessionRecurrenceRule(
                campaign, center, weekdays, startDate, endDate, startTime, endTime, timezone));
    }

    @Transactional(readOnly = true)
    public Optional<SessionRecurrenceRule> getRuleById(Long id) {
        requireId(id, "Session recurrence rule ID is required");
        return ruleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<SessionRecurrenceRule> getRuleByGenerationKey(String generationKey) {
        if (generationKey == null || generationKey.isBlank()) {
            throw new IllegalArgumentException("Session recurrence generation key is required");
        }
        return ruleRepository.findByGenerationKey(generationKey.trim());
    }

    @Transactional(readOnly = true)
    public List<SessionRecurrenceRule> getRulesForCampaign(Long campaignId) {
        requireId(campaignId, "Campaign ID is required");
        return ruleRepository.findByCampaignIdOrderByStartDateAscStartTimeAscIdAsc(campaignId);
    }

    @Transactional(readOnly = true)
    public List<SessionRecurrenceRule> getRulesForCenter(Long centerId) {
        requireId(centerId, "Center ID is required");
        return ruleRepository.findByCenterIdOrderByStartDateAscStartTimeAscIdAsc(centerId);
    }

    @Transactional(readOnly = true)
    public List<SessionRecurrenceRule> getActiveRulesForCampaignCenterOn(
            Long campaignId,
            Long centerId,
            LocalDate date
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(centerId, "Center ID is required");
        if (date == null) {
            throw new IllegalArgumentException("Recurrence evaluation date is required");
        }
        return ruleRepository.findActiveRulesForCampaignCenterOn(
                campaignId, centerId, SessionRecurrenceRuleStatus.ACTIVE, date, date.getDayOfWeek());
    }

    public SessionRecurrenceRule updateFutureRule(
            Long id,
            Set<DayOfWeek> weekdays,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            String timezone
    ) {
        requireId(id, "Session recurrence rule ID is required");
        SessionRecurrenceRule.validateDefinition(
                weekdays, startDate, endDate, startTime, endTime, timezone);

        SessionRecurrenceRule unlockedTarget = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session recurrence rule not found with ID: " + id));
        Long campaignId = unlockedTarget.getCampaign().getId();

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
        if (entityManager != null) {
            entityManager.refresh(campaign);
        }
        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalArgumentException(
                    "Cannot update future session recurrence rule for operationally closed campaign with ID: " + campaignId);
        }

        SessionRecurrenceRule target = findAuthoritativeRule(id);
        requireActive(target);
        if (sessionRepository.existsByRecurrenceRuleId(id)) {
            throw new IllegalArgumentException(
                    "Cannot update recurrence definition for a rule with existing generated sessions. Series-wide modification workflow is required.");
        }

        LocalDate existingToday = todayIn(target.getTimezone());
        if (!existingToday.isBefore(target.getStartDate())) {
            throw new IllegalArgumentException("A started session recurrence rule cannot be fully updated");
        }
        LocalDate candidateToday = todayIn(timezone.trim());
        if (!candidateToday.isBefore(startDate)) {
            throw new IllegalArgumentException("Updated session recurrence rule must start in the future");
        }

        target.updateDefinition(weekdays, startDate, endDate, startTime, endTime, timezone);
        return target;
    }

    public SessionRecurrenceRule shortenRuleEnd(Long id, LocalDate newEndDate) {
        requireId(id, "Session recurrence rule ID is required");
        if (newEndDate == null) {
            throw new IllegalArgumentException("Recurrence end date is required");
        }

        SessionRecurrenceRule target = findAuthoritativeRule(id);
        requireActive(target);
        LocalDate today = todayIn(target.getTimezone());
        if (today.isAfter(target.getEndDate())) {
            throw new IllegalArgumentException("An expired session recurrence rule cannot be shortened");
        }
        if (newEndDate.isBefore(today)) {
            throw new IllegalArgumentException("Recurrence end date cannot rewrite elapsed dates");
        }
        if (sessionRepository.existsByRecurrenceRuleIdAndOriginOccurrenceDateAfter(id, newEndDate)) {
            throw new IllegalArgumentException(
                    "Cannot shorten recurrence rule end date before existing generated session occurrences");
        }

        target.shortenEndDate(newEndDate);
        return target;
    }

    public SessionRecurrenceRule cancelRule(Long id) {
        requireId(id, "Session recurrence rule ID is required");

        SessionRecurrenceRule target = findAuthoritativeRule(id);
        requireActive(target);
        LocalDate today = todayIn(target.getTimezone());
        if (today.isAfter(target.getEndDate())) {
            throw new IllegalArgumentException("An expired session recurrence rule cannot be cancelled");
        }

        target.cancel();
        return target;
    }

    private SessionRecurrenceRule findAuthoritativeRule(Long id) {
        SessionRecurrenceRule rule = ruleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Session recurrence rule not found with ID: " + id));
        entityManager.refresh(rule);
        return rule;
    }

    private LocalDate todayIn(String timezone) {
        return LocalDate.now(clock.withZone(ZoneId.of(timezone)));
    }

    private static void requireActive(SessionRecurrenceRule rule) {
        if (rule.getStatus() != SessionRecurrenceRuleStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active session recurrence rule can be changed");
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
