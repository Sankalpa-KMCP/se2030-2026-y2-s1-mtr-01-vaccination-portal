package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing VaccinationCampaign operational lifecycles and reverse configuration-impact rules.
 */
@Service
@Transactional
public class VaccinationCampaignService {

    private static final Set<SessionLifecycleStatus> BLOCKING_SESSION_STATUSES = EnumSet.of(
            SessionLifecycleStatus.DRAFT,
            SessionLifecycleStatus.SCHEDULED,
            SessionLifecycleStatus.IN_PROGRESS
    );

    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final CampaignDoseProgrammeRepository doseProgrammeRepository;
    private final CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    private final QuestionnaireVersionPublicationPort publicationPort;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public VaccinationCampaignService(
            VaccinationCampaignRepository campaignRepository,
            VaccinationSessionRepository sessionRepository,
            CampaignDoseProgrammeRepository doseProgrammeRepository,
            CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository,
            @Autowired(required = false) QuestionnaireVersionPublicationPort publicationPort,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.sessionRepository = sessionRepository;
        this.doseProgrammeRepository = doseProgrammeRepository;
        this.questionnaireAssignmentRepository = questionnaireAssignmentRepository;
        this.publicationPort = publicationPort;
        this.clock = clock;
    }

    /**
     * Publishes a VaccinationCampaign (transitions status to PUBLISHED).
     * Enforces all publication prerequisites under the campaign pessimistic write lock:
     * 1. Lock campaign row with findByIdForUpdate.
     * 2. Refresh authoritative state via entityManager.refresh.
     * 3. Allowed status sources: DRAFT -> PUBLISHED or SCHEDULED -> PUBLISHED.
     * 4. Valid campaign date and booking window invariants.
     * 5. Require active VaccineType.
     * 6. Require active TargetGroup with non-blank description.
     * 7. Require exactly one effective CampaignDoseProgramme.
     * 8. Require exactly one effective CampaignQuestionnaireAssignment.
     * 9. Require the assigned QuestionnaireVersion to be in PUBLISHED status via QuestionnaireVersionPublicationPort.
     *
     * Note: Location targets, center assignments, recurrence rules, and sessions are NOT publication prerequisites.
     *
     * @param campaignId ID of the VaccinationCampaign to publish
     * @return Saved VaccinationCampaign in PUBLISHED status
     */
    public VaccinationCampaign publishCampaign(Long campaignId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot publish campaign in " + campaign.getStatus() + " status");
        }

        // Validate date & booking window invariants
        campaign.validateDateConsistency();

        // Require VaccineType
        VaccineType vaccineType = campaign.getVaccineType();
        if (vaccineType == null || !vaccineType.isActive()) {
            throw new IllegalStateException("Campaign requires an active VaccineType for publication");
        }

        // Require TargetGroup description
        TargetGroup targetGroup = campaign.getTargetGroup();
        if (targetGroup == null || !targetGroup.isActive() || targetGroup.getDescription() == null || targetGroup.getDescription().trim().isEmpty()) {
            throw new IllegalStateException("Campaign requires a TargetGroup with a non-blank description for publication");
        }

        // Evaluation date for effective version resolution
        LocalDate today = LocalDate.now(clock);
        LocalDate evaluationDate = today;
        if (campaign.getServiceStartDate() != null && campaign.getServiceStartDate().isAfter(today)) {
            evaluationDate = campaign.getServiceStartDate();
        }

        // Require exactly one effective CampaignDoseProgramme
        List<CampaignDoseProgramme> effectiveProgrammes = doseProgrammeRepository.findEffectiveProgrammesForDateList(campaignId, evaluationDate);
        if (effectiveProgrammes.isEmpty() && !evaluationDate.equals(today)) {
            effectiveProgrammes = doseProgrammeRepository.findEffectiveProgrammesForDateList(campaignId, today);
        }
        if (effectiveProgrammes.isEmpty()) {
            throw new IllegalStateException("Campaign requires exactly one effective dose programme for publication; found 0");
        }
        if (effectiveProgrammes.size() > 1) {
            throw new IllegalStateException("Campaign requires exactly one effective dose programme for publication; found " + effectiveProgrammes.size());
        }

        // Require exactly one effective CampaignQuestionnaireAssignment
        List<CampaignQuestionnaireAssignment> effectiveAssignments = questionnaireAssignmentRepository.findEffectiveAssignmentsAtList(campaignId, evaluationDate);
        if (effectiveAssignments.isEmpty() && !evaluationDate.equals(today)) {
            effectiveAssignments = questionnaireAssignmentRepository.findEffectiveAssignmentsAtList(campaignId, today);
        }
        if (effectiveAssignments.isEmpty()) {
            throw new IllegalStateException("Campaign requires exactly one effective questionnaire assignment for publication; found 0");
        }
        if (effectiveAssignments.size() > 1) {
            throw new IllegalStateException("Campaign requires exactly one effective questionnaire assignment for publication; found " + effectiveAssignments.size());
        }

        // Require assigned QuestionnaireVersion to be PUBLISHED in MF-01
        CampaignQuestionnaireAssignment assignment = effectiveAssignments.get(0);
        Long questionnaireVersionId = assignment.getQuestionnaireVersionId();
        if (questionnaireVersionId == null) {
            throw new IllegalStateException("Assigned questionnaire version ID is required");
        }

        if (publicationPort == null) {
            throw new IllegalStateException(
                    "QuestionnaireVersionPublicationPort implementation is not registered. MF-01 integration pending."
            );
        }

        if (!publicationPort.isPublished(questionnaireVersionId)) {
            throw new IllegalStateException(
                    "Assigned questionnaire version " + questionnaireVersionId + " is not in PUBLISHED status in MF-01"
            );
        }

        campaign.publish();
        return campaignRepository.save(campaign);
    }


    public VaccinationCampaign createCampaign(
            String code,
            String name,
            String description,
            VaccineType vaccineType,
            TargetGroup targetGroup,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt,
            CampaignStatus status
    ) {
        String normalizedCode = code != null ? code.trim() : null;
        if (campaignRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Campaign already exists with code: " + normalizedCode);
        }

        VaccinationCampaign campaign = new VaccinationCampaign(
                normalizedCode,
                name,
                description,
                vaccineType,
                targetGroup,
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt,
                status != null ? status : CampaignStatus.DRAFT
        );

        return campaignRepository.save(campaign);
    }

    public VaccinationCampaign updateServiceDates(
            Long campaignId,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate
    ) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (serviceStartDate == null) {
            throw new IllegalArgumentException("Service start date is required");
        }
        if (serviceEndDate == null) {
            throw new IllegalArgumentException("Service end date is required");
        }
        if (serviceStartDate.isAfter(serviceEndDate)) {
            throw new IllegalArgumentException("Service start date (" + serviceStartDate +
                    ") cannot be after service end date (" + serviceEndDate + ")");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
        entityManager.refresh(campaign);

        validateCampaignServiceDateReverseImpact(campaignId, serviceStartDate, serviceEndDate);

        campaign.setServiceStartDate(serviceStartDate);
        campaign.setServiceEndDate(serviceEndDate);

        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Optional<VaccinationCampaign> getCampaignById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return campaignRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<VaccinationCampaign> getCampaignByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return campaignRepository.findByCode(code.trim());
    }

    private void validateCampaignServiceDateReverseImpact(
            Long campaignId,
            LocalDate proposedServiceStart,
            LocalDate proposedServiceEnd
    ) {
        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCampaign(

                campaignId, BLOCKING_SESSION_STATUSES, clock.instant()
        );
        for (VaccinationSession session : blockingSessions) {
            ZoneId zoneId = ZoneId.of(session.getScheduleTimezone());
            ZonedDateTime startZdt = session.getStartAt().atZone(zoneId);
            LocalDate firstTouchedDate = startZdt.toLocalDate();
            Instant lastContainedInstant = session.getEndAt().minusNanos(1);
            LocalDate lastTouchedDate = lastContainedInstant.atZone(zoneId).toLocalDate();

            if (firstTouchedDate.isBefore(proposedServiceStart) || lastTouchedDate.isAfter(proposedServiceEnd)) {
                throw new IllegalArgumentException(
                        "Cannot shorten campaign service dates because active or scheduled session(s) fall outside proposed service dates [" +
                                proposedServiceStart + " to " + proposedServiceEnd + "]"
                );
            }
        }
    }
}
