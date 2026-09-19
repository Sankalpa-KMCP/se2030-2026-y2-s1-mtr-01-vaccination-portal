package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.dto.response.CitizenCampaignDiscoveryResponse;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
    private final CampaignLifecycleEventRepository campaignLifecycleEventRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final CampaignDoseProgrammeRepository doseProgrammeRepository;
    private final CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    private final CampaignCenterAssignmentRepository centerAssignmentRepository;
    private final VaccineTypeRepository vaccineTypeRepository;
    private final TargetGroupRepository targetGroupRepository;
    private final QuestionnaireVersionPublicationPort publicationPort;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public VaccinationCampaignService(
            VaccinationCampaignRepository campaignRepository,
            CampaignLifecycleEventRepository campaignLifecycleEventRepository,
            VaccinationSessionRepository sessionRepository,
            CampaignDoseProgrammeRepository doseProgrammeRepository,
            CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository,
            CampaignCenterAssignmentRepository centerAssignmentRepository,
            VaccineTypeRepository vaccineTypeRepository,
            TargetGroupRepository targetGroupRepository,
            @Autowired(required = false) QuestionnaireVersionPublicationPort publicationPort,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignLifecycleEventRepository = campaignLifecycleEventRepository;
        this.sessionRepository = sessionRepository;
        this.doseProgrammeRepository = doseProgrammeRepository;
        this.questionnaireAssignmentRepository = questionnaireAssignmentRepository;
        this.centerAssignmentRepository = centerAssignmentRepository;
        this.vaccineTypeRepository = vaccineTypeRepository;
        this.targetGroupRepository = targetGroupRepository;
        this.publicationPort = publicationPort;
        this.clock = clock;
    }

    /**
     * Approves and schedules a future VaccinationCampaign (transitions status to SCHEDULED).
     * Enforces all scheduling prerequisites under the campaign pessimistic write lock:
     * 1. Lock campaign row with findByIdForUpdate.
     * 2. Refresh authoritative state via entityManager.refresh.
     * 3. Allowed status source: DRAFT -> SCHEDULED only.
     * 4. Valid campaign date and booking window invariants (validateDateConsistency).
     * 5. Strictly future service start date: serviceStartDate > LocalDate.now(clock).
     * 6. Require active VaccineType.
     * 7. Require active TargetGroup (description check is publication-only, not required here).
     * 8. Require exactly one effective CampaignDoseProgramme at serviceStartDate.
     * 9. Require exactly one effective CampaignQuestionnaireAssignment at serviceStartDate.
     * 10. Require at least one relevant overlapping ACTIVE CampaignCenterAssignment whose center is in ACTIVE status.
     *
     * Note: OperatingHours, CenterClosure, and QuestionnaireVersion publication in MF-01 are NOT scheduling prerequisites.
     *
     * @param campaignId ID of the VaccinationCampaign to schedule
     * @return Saved VaccinationCampaign in SCHEDULED status
     */
    public VaccinationCampaign scheduleCampaign(Long campaignId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Cannot schedule campaign in " + campaign.getStatus() + " status. Only DRAFT campaigns can be scheduled.");
        }

        // Validate date & booking window invariants
        campaign.validateDateConsistency();

        // Validate strictly future service start date against clock
        LocalDate today = LocalDate.now(clock);
        LocalDate serviceStartDate = campaign.getServiceStartDate();
        if (serviceStartDate == null || !serviceStartDate.isAfter(today)) {
            throw new IllegalStateException(
                    "Campaign service start date (" + serviceStartDate + ") must be strictly in the future for scheduling approval [current date: " + today + "]"
            );
        }

        // Require active VaccineType
        VaccineType vaccineType = campaign.getVaccineType();
        if (vaccineType == null || !vaccineType.isActive()) {
            throw new IllegalStateException("Campaign requires an active VaccineType for scheduling approval");
        }

        // Require active TargetGroup
        TargetGroup targetGroup = campaign.getTargetGroup();
        if (targetGroup == null || !targetGroup.isActive()) {
            throw new IllegalStateException("Campaign requires an active TargetGroup for scheduling approval");
        }

        // Require exactly one effective CampaignDoseProgramme at serviceStartDate
        List<CampaignDoseProgramme> effectiveProgrammes = doseProgrammeRepository.findEffectiveProgrammesForDateList(campaignId, serviceStartDate);
        if (effectiveProgrammes.isEmpty()) {
            throw new IllegalStateException("Campaign requires exactly one effective dose programme for scheduling approval; found 0");
        }
        if (effectiveProgrammes.size() > 1) {
            throw new IllegalStateException("Campaign requires exactly one effective dose programme for scheduling approval; found " + effectiveProgrammes.size());
        }

        // Require exactly one effective CampaignQuestionnaireAssignment at serviceStartDate
        List<CampaignQuestionnaireAssignment> effectiveAssignments = questionnaireAssignmentRepository.findEffectiveAssignmentsAtList(campaignId, serviceStartDate);
        if (effectiveAssignments.isEmpty()) {
            throw new IllegalStateException("Campaign requires exactly one effective questionnaire assignment for scheduling approval; found 0");
        }
        if (effectiveAssignments.size() > 1) {
            throw new IllegalStateException("Campaign requires exactly one effective questionnaire assignment for scheduling approval; found " + effectiveAssignments.size());
        }

        // Require assigned QuestionnaireVersion to be PUBLISHED in MF-01
        CampaignQuestionnaireAssignment questionnaireAssignment = effectiveAssignments.get(0);
        Long questionnaireVersionId = questionnaireAssignment.getQuestionnaireVersionId();
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

        // Require relevant overlapping ACTIVE center assignments and active centers
        LocalDate serviceEndDate = campaign.getServiceEndDate();
        List<CampaignCenterAssignment> allAssignments = centerAssignmentRepository.findCampaignHistory(campaignId);
        List<CampaignCenterAssignment> relevantAssignments = allAssignments.stream()
                .filter(a -> a.getStatus() == CampaignCenterAssignmentStatus.ACTIVE)
                .filter(a -> !a.getActiveFrom().isAfter(serviceEndDate)
                        && (a.getActiveTo() == null || a.getActiveTo().isAfter(serviceStartDate)))
                .toList();

        if (relevantAssignments.isEmpty()) {
            throw new IllegalStateException("Campaign requires at least one active center assignment overlapping its service period for scheduling approval; found 0");
        }

        for (CampaignCenterAssignment assignment : relevantAssignments) {
            VaccinationCenter center = assignment.getCenter();
            if (center == null || center.getCenterStatus() != CenterStatus.ACTIVE) {
                throw new IllegalStateException("Campaign center assignment " + assignment.getId() + " references an INACTIVE center: " + (center != null ? center.getCode() : "null"));
            }
        }

        campaign.schedule();
        return campaignRepository.save(campaign);
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
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

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

    /**
     * Activates a PUBLISHED VaccinationCampaign (transitions status to ACTIVE).
     * Enforces all activation prerequisites under the campaign pessimistic write lock:
     * 1. Lock campaign row with findByIdForUpdate.
     * 2. Refresh authoritative state via entityManager.refresh.
     * 3. Allowed status source: PUBLISHED -> ACTIVE only.
     * 4. Verify current application date (derived from Clock) satisfies:
     *    serviceStartDate <= today <= serviceEndDate.
     *
     * @param campaignId ID of the VaccinationCampaign to activate
     * @return Saved VaccinationCampaign in ACTIVE status
     */
    public VaccinationCampaign activateCampaign(Long campaignId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        if (campaign.getStatus() != CampaignStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Cannot activate campaign in " + campaign.getStatus() + " status. Only PUBLISHED campaigns can be activated."
            );
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate serviceStartDate = campaign.getServiceStartDate();
        LocalDate serviceEndDate = campaign.getServiceEndDate();

        if (serviceStartDate == null || serviceEndDate == null) {
            throw new IllegalStateException("Campaign service start and end dates are required for activation");
        }

        if (today.isBefore(serviceStartDate)) {
            throw new IllegalStateException(
                    "Campaign service start date (" + serviceStartDate + ") has not been reached [current date: " + today + "]"
            );
        }

        if (today.isAfter(serviceEndDate)) {
            throw new IllegalStateException(
                    "Campaign service end date (" + serviceEndDate + ") has already passed [current date: " + today + "]"
            );
        }

        campaign.activate();
        return campaignRepository.save(campaign);
    }

    /**
     * Suspends a PUBLISHED or ACTIVE campaign and records the impact as an append-only lifecycle event.
     * The campaign status and event are persisted atomically under the campaign pessimistic write lock.
     *
     * @param campaignId ID of the campaign to suspend
     * @param reason mandatory operational reason
     * @param actorId mandatory opaque identifier of the trusted actor
     * @return managed campaign in SUSPENDED status
     */
    public VaccinationCampaign suspendCampaign(Long campaignId, String reason, String actorId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        String validatedReason = CampaignLifecycleEvent.validateReason(reason);
        String validatedActorId = CampaignLifecycleEvent.validateActorId(actorId);

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        CampaignStatus previousStatus = campaign.getStatus();
        campaign.suspend();

        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                campaign,
                CampaignLifecycleChangeType.SUSPENDED,
                previousStatus,
                campaign.getStatus(),
                validatedReason,
                validatedActorId,
                clock.instant()
        );
        campaignLifecycleEventRepository.save(event);

        return campaign;
    }

    /**
     * Resumes a SUSPENDED VaccinationCampaign to a target status derived from the application date.
     * Date-driven target resolution:
     * - today < serviceStartDate -> PUBLISHED (Resume before service start)
     * - serviceStartDate <= today <= serviceEndDate -> ACTIVE (Resume during service period)
     * - today > serviceEndDate -> throws IllegalStateException (service end date has passed)
     *
     * Enforces all resumption prerequisites under the campaign pessimistic write lock:
     * 1. Validate mandatory reason and actor ID inputs.
     * 2. Lock campaign row with findByIdForUpdate.
     * 3. Refresh authoritative state via entityManager.refresh.
     * 4. Verify authoritative status is SUSPENDED.
     * 5. Resolve target status based on current date derived from Clock.
     * 6. Mutate campaign status and record an append-only CampaignLifecycleEvent with changeType RESUMED.
     *
     * @param campaignId ID of the VaccinationCampaign to resume
     * @param reason mandatory operational reason
     * @param actorId mandatory opaque identifier of the trusted actor
     * @return managed campaign in PUBLISHED or ACTIVE status
     */
    public VaccinationCampaign resumeCampaign(Long campaignId, String reason, String actorId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        String validatedReason = CampaignLifecycleEvent.validateReason(reason);
        String validatedActorId = CampaignLifecycleEvent.validateActorId(actorId);

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        if (campaign.getStatus() != CampaignStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Cannot resume campaign in " + campaign.getStatus() + " status. Only SUSPENDED campaigns can be resumed."
            );
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate serviceStartDate = campaign.getServiceStartDate();
        LocalDate serviceEndDate = campaign.getServiceEndDate();

        if (serviceStartDate == null || serviceEndDate == null) {
            throw new IllegalStateException("Campaign service start and end dates are required for resumption");
        }

        if (today.isAfter(serviceEndDate)) {
            throw new IllegalStateException(
                    "Campaign service end date (" + serviceEndDate + ") has already passed [current date: " + today + "]"
            );
        }

        if (today.isBefore(serviceStartDate)) {
            campaign.resumeToPublished();
        } else {
            campaign.resumeToActive();
        }

        VaccinationCampaign savedCampaign = campaignRepository.save(campaign);

        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                savedCampaign,
                CampaignLifecycleChangeType.RESUMED,
                CampaignStatus.SUSPENDED,
                savedCampaign.getStatus(),
                validatedReason,
                validatedActorId,
                clock.instant()
        );
        campaignLifecycleEventRepository.save(event);

        return savedCampaign;
    }

    /**
     * Completes an ACTIVE, PUBLISHED, or SUSPENDED campaign and records the impact as an append-only lifecycle event.
     * Fails if any unfinished sessions (in DRAFT, SCHEDULED, or IN_PROGRESS status) remain under the campaign.
     * The campaign status and event are persisted atomically under the campaign pessimistic write lock.
     *
     * @param campaignId ID of the campaign to complete
     * @param reason mandatory operational reason
     * @param actorId mandatory identifier of the trusted actor
     * @return managed campaign in COMPLETED status
     */
    public VaccinationCampaign completeCampaign(Long campaignId, String reason, String actorId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        String validatedReason = CampaignLifecycleEvent.validateReason(reason);
        String validatedActorId = CampaignLifecycleEvent.validateActorId(actorId);

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        List<VaccinationSession> sessions = sessionRepository.findByCampaignId(campaignId);
        List<VaccinationSession> unfinishedSessions = sessions.stream()
                .filter(s -> BLOCKING_SESSION_STATUSES.contains(s.getLifecycleStatus()))
                .toList();
        if (!unfinishedSessions.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot complete campaign with ID " + campaignId + " because " + unfinishedSessions.size() +
                            " unfinished session(s) remain in " +
                            unfinishedSessions.stream().map(s -> s.getLifecycleStatus().name()).distinct().toList() + " status"
            );
        }

        CampaignStatus previousStatus = campaign.getStatus();
        campaign.complete();

        VaccinationCampaign savedCampaign = campaignRepository.save(campaign);

        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                savedCampaign,
                CampaignLifecycleChangeType.COMPLETED,
                previousStatus,
                savedCampaign.getStatus(),
                validatedReason,
                validatedActorId,
                clock.instant()
        );
        campaignLifecycleEventRepository.save(event);

        return savedCampaign;
    }

    /**
     * Cancels a non-terminal campaign (in DRAFT, SCHEDULED, PUBLISHED, ACTIVE, or SUSPENDED status)
     * and records the impact as an append-only lifecycle event.
     * The campaign status and event are persisted atomically under the campaign pessimistic write lock.
     *
     * @param campaignId ID of the campaign to cancel
     * @param reason mandatory operational reason
     * @param actorId mandatory identifier of the trusted actor
     * @return managed campaign in CANCELLED status
     */
    public VaccinationCampaign cancelCampaign(Long campaignId, String reason, String actorId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        String validatedReason = CampaignLifecycleEvent.validateReason(reason);
        String validatedActorId = CampaignLifecycleEvent.validateActorId(actorId);

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        CampaignStatus previousStatus = campaign.getStatus();
        campaign.cancel();

        VaccinationCampaign savedCampaign = campaignRepository.save(campaign);

        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                savedCampaign,
                CampaignLifecycleChangeType.CANCELLED,
                previousStatus,
                savedCampaign.getStatus(),
                validatedReason,
                validatedActorId,
                clock.instant()
        );
        campaignLifecycleEventRepository.save(event);

        return savedCampaign;
    }

    /**
     * Archives a COMPLETED or CANCELLED campaign and records the impact as an append-only lifecycle event.
     * The campaign status and event are persisted atomically under the campaign pessimistic write lock.
     *
     * @param campaignId ID of the campaign to archive
     * @param reason mandatory operational reason
     * @param actorId mandatory identifier of the trusted actor
     * @return managed campaign in ARCHIVED status
     */
    public VaccinationCampaign archiveCampaign(Long campaignId, String reason, String actorId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        String validatedReason = CampaignLifecycleEvent.validateReason(reason);
        String validatedActorId = CampaignLifecycleEvent.validateActorId(actorId);

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        CampaignStatus previousStatus = campaign.getStatus();
        campaign.archive();

        VaccinationCampaign savedCampaign = campaignRepository.save(campaign);

        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                savedCampaign,
                CampaignLifecycleChangeType.ARCHIVED,
                previousStatus,
                savedCampaign.getStatus(),
                validatedReason,
                validatedActorId,
                clock.instant()
        );
        campaignLifecycleEventRepository.save(event);

        return savedCampaign;
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
            Instant bookingCloseAt
    ) {
        if (vaccineType != null && !vaccineType.isActive()) {
            throw new IllegalArgumentException("Cannot create campaign with inactive VaccineType: " + vaccineType.getCode());
        }
        if (targetGroup != null && !targetGroup.isActive()) {
            throw new IllegalArgumentException("Cannot create campaign with inactive TargetGroup: " + targetGroup.getCode());
        }
        if (serviceStartDate == null) {
            throw new IllegalArgumentException("Service start date is required");
        }
        if (serviceEndDate == null) {
            throw new IllegalArgumentException("Service end date is required");
        }
        if (bookingOpenAt == null) {
            throw new IllegalArgumentException("Booking open timestamp is required");
        }
        if (bookingCloseAt == null) {
            throw new IllegalArgumentException("Booking close timestamp is required");
        }
        if (!serviceEndDate.isAfter(serviceStartDate)) {
            throw new IllegalArgumentException("Campaign service end date (" + serviceEndDate +
                    ") must be after service start date (" + serviceStartDate + ") [MF02-BR-01]");
        }
        if (!bookingCloseAt.isAfter(bookingOpenAt)) {
            throw new IllegalArgumentException("Campaign booking close time (" + bookingCloseAt +
                    ") must be after booking open time (" + bookingOpenAt + ") [MF02-BR-01]");
        }
        LocalDate bookingCloseDate = bookingCloseAt.atZone(ZoneOffset.UTC).toLocalDate();
        if (bookingCloseDate.isAfter(serviceEndDate)) {
            throw new IllegalArgumentException("Campaign booking close time (" + bookingCloseAt +
                    ") must not be after campaign service end date (" + serviceEndDate + ") [MF02-BR-01]");
        }

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
                bookingCloseAt
        );

        return campaignRepository.save(campaign);
    }

    /**
     * Creates and saves a new VaccinationCampaign resolving VaccineType and TargetGroup by ID.
     */
    public VaccinationCampaign createCampaign(
            String code,
            String name,
            String description,
            Long vaccineTypeId,
            Long targetGroupId,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt
    ) {
        return createCampaign(
                code,
                name,
                description,
                vaccineTypeId,
                targetGroupId,
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt,
                null
        );
    }

    public VaccinationCampaign createCampaign(
            String code,
            String name,
            String description,
            Long vaccineTypeId,
            Long targetGroupId,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt,
            Long questionnaireVersionId
    ) {
        if (vaccineTypeId == null) {
            throw new IllegalArgumentException("Vaccine type ID is required");
        }
        if (targetGroupId == null) {
            throw new IllegalArgumentException("Target group ID is required");
        }
        if (questionnaireVersionId != null) {
            if (questionnaireVersionId <= 0) {
                throw new IllegalArgumentException("Questionnaire version ID must be positive: " + questionnaireVersionId);
            }
            if (publicationPort == null) {
                throw new IllegalStateException(
                        "QuestionnaireVersionPublicationPort implementation is not registered. MF-01 integration pending."
                );
            }
            if (!publicationPort.isPublished(questionnaireVersionId)) {
                throw new IllegalArgumentException(
                        "Questionnaire version " + questionnaireVersionId + " is not in PUBLISHED status in MF-01"
                );
            }
        }

        VaccineType vaccineType = vaccineTypeRepository.findById(vaccineTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("VaccineType not found with ID: " + vaccineTypeId));
        TargetGroup targetGroup = targetGroupRepository.findById(targetGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("TargetGroup not found with ID: " + targetGroupId));

        VaccinationCampaign campaign = createCampaign(
                code,
                name,
                description,
                vaccineType,
                targetGroup,
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt
        );

        if (questionnaireVersionId != null) {
            LocalDate today = LocalDate.now(clock);
            LocalDate effectiveStartDate = serviceStartDate.isBefore(today) ? today : serviceStartDate;
            questionnaireAssignmentRepository.save(new CampaignQuestionnaireAssignment(
                    campaign,
                    questionnaireVersionId,
                    effectiveStartDate,
                    null
            ));
        }

        return campaign;
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
        if (!serviceEndDate.isAfter(serviceStartDate)) {
            throw new IllegalArgumentException("Campaign service end date (" + serviceEndDate +
                    ") must be after service start date (" + serviceStartDate + ")");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));
        entityManager.refresh(campaign);

        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalStateException(
                    "Cannot update service dates for campaign in " + campaign.getStatus() + " status"
            );
        }

        campaign.validateDateConsistency();
        if (campaign.getBookingCloseAt() != null) {
            LocalDate bookingCloseDate = campaign.getBookingCloseAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (bookingCloseDate.isAfter(serviceEndDate)) {
                throw new IllegalArgumentException("Campaign booking close time (" + campaign.getBookingCloseAt() +
                        ") must not be after campaign service end date (" + serviceEndDate + ")");
            }
        }
        validateCampaignServiceDateReverseImpact(campaignId, serviceStartDate, serviceEndDate);

        campaign.setServiceStartDate(serviceStartDate);
        campaign.setServiceEndDate(serviceEndDate);

        return campaignRepository.save(campaign);
    }

    /**
     * Updates editable configuration attributes of an existing campaign.
     * Enforces date consistency, reverse-impact session containment, and closed-state guard.
     */
    public VaccinationCampaign updateCampaign(
            Long campaignId,
            String name,
            String description,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt
    ) {
        return updateCampaign(
                campaignId,
                name,
                description,
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt,
                null
        );
    }

    public VaccinationCampaign updateCampaign(
            Long campaignId,
            String name,
            String description,
            LocalDate serviceStartDate,
            LocalDate serviceEndDate,
            Instant bookingOpenAt,
            Instant bookingCloseAt,
            Long questionnaireVersionId
    ) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        if (serviceStartDate == null) {
            throw new IllegalArgumentException("Service start date is required");
        }
        if (serviceEndDate == null) {
            throw new IllegalArgumentException("Service end date is required");
        }
        if (bookingOpenAt == null) {
            throw new IllegalArgumentException("Booking open timestamp is required");
        }
        if (bookingCloseAt == null) {
            throw new IllegalArgumentException("Booking close timestamp is required");
        }

        if (!serviceEndDate.isAfter(serviceStartDate)) {
            throw new IllegalArgumentException("Campaign service end date (" + serviceEndDate +
                    ") must be after service start date (" + serviceStartDate + ")");
        }
        if (!bookingCloseAt.isAfter(bookingOpenAt)) {
            throw new IllegalArgumentException("Campaign booking close time (" + bookingCloseAt +
                    ") must be after booking open time (" + bookingOpenAt + ")");
        }
        LocalDate bookingCloseDate = bookingCloseAt.atZone(ZoneOffset.UTC).toLocalDate();
        if (bookingCloseDate.isAfter(serviceEndDate)) {
            throw new IllegalArgumentException("Campaign booking close time (" + bookingCloseAt +
                    ") must not be after campaign service end date (" + serviceEndDate + ")");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (entityManager != null) {
            entityManager.refresh(campaign);
        }

        if (campaign.getStatus().isOperationallyClosed()) {
            throw new IllegalStateException(
                    "Cannot update campaign in " + campaign.getStatus() + " status"
            );
        }

        validateCampaignServiceDateReverseImpact(campaignId, serviceStartDate, serviceEndDate);

        campaign.setName(name.trim());
        campaign.setDescription(description);
        campaign.setServiceStartDate(serviceStartDate);
        campaign.setServiceEndDate(serviceEndDate);
        campaign.setBookingOpenAt(bookingOpenAt);
        campaign.setBookingCloseAt(bookingCloseAt);
        campaign.validateDateConsistency();

        VaccinationCampaign savedCampaign = campaignRepository.save(campaign);

        if (questionnaireVersionId != null) {
            if (questionnaireVersionId <= 0) {
                throw new IllegalArgumentException("Questionnaire version ID must be positive: " + questionnaireVersionId);
            }
            Optional<CampaignQuestionnaireAssignment> currentOpenEndedOpt = questionnaireAssignmentRepository.findOpenEndedAssignment(campaignId);
            Long currentAssignedId = currentOpenEndedOpt.map(CampaignQuestionnaireAssignment::getQuestionnaireVersionId).orElse(null);

            if (!questionnaireVersionId.equals(currentAssignedId)) {
                if (publicationPort == null) {
                    throw new IllegalStateException(
                            "QuestionnaireVersionPublicationPort implementation is not registered. MF-01 integration pending."
                    );
                }
                if (!publicationPort.isPublished(questionnaireVersionId)) {
                    throw new IllegalArgumentException(
                            "Questionnaire version " + questionnaireVersionId + " is not in PUBLISHED status in MF-01"
                    );
                }
                if (publicationPort.hasCampaignSubmissions(campaignId)) {
                    throw new IllegalStateException(
                            "Cannot change questionnaire assignment for a campaign with existing citizen screening submissions"
                    );
                }

                LocalDate today = LocalDate.now(clock);
                LocalDate effectiveStartDate = serviceStartDate.isBefore(today) ? today : serviceStartDate;

                if (savedCampaign.getStatus() == CampaignStatus.DRAFT && currentOpenEndedOpt.isPresent()) {
                    CampaignQuestionnaireAssignment current = currentOpenEndedOpt.get();
                    current.setQuestionnaireVersionId(questionnaireVersionId);
                    questionnaireAssignmentRepository.save(current);
                } else {
                    currentOpenEndedOpt.ifPresent(current -> {
                        LocalDate closeDate = effectiveStartDate.isAfter(current.getActiveFrom()) ? effectiveStartDate : current.getActiveFrom().plusDays(1);
                        current.closeAt(closeDate);
                        questionnaireAssignmentRepository.save(current);
                    });
                    questionnaireAssignmentRepository.save(new CampaignQuestionnaireAssignment(
                            savedCampaign,
                            questionnaireVersionId,
                            effectiveStartDate,
                            null
                    ));
                }
            }
        }

        return savedCampaign;
    }

    @Transactional(readOnly = true)
    public Long getAssignedQuestionnaireVersionId(Long campaignId) {
        if (campaignId == null) {
            return null;
        }
        return questionnaireAssignmentRepository.findOpenEndedAssignment(campaignId)
                .map(CampaignQuestionnaireAssignment::getQuestionnaireVersionId)
                .orElseGet(() -> {
                    List<CampaignQuestionnaireAssignment> history = questionnaireAssignmentRepository.findByCampaignIdOrderByActiveFromDescIdDesc(campaignId);
                    return history.isEmpty() ? null : history.get(0).getQuestionnaireVersionId();
                });
    }

    @Transactional(readOnly = true)
    public Optional<VaccinationCampaign> getCampaignById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return campaignRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public VaccinationCampaign getRequiredCampaignById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<VaccinationCampaign> getCampaigns(CampaignStatus status) {
        if (status != null) {
            return campaignRepository.findByStatus(status);
        }
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CampaignLifecycleEvent> getCampaignLifecycleEvents(Long campaignId) {
        if (campaignId == null) {
            throw new IllegalArgumentException("Campaign ID is required");
        }
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found with ID: " + campaignId);
        }
        return campaignLifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaignId);
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

    /**
     * Retrieve campaigns visible for authenticated citizen discovery (MF02-R-001).
     * Returns only PUBLISHED and ACTIVE campaigns ordered by ID ascending.
     * Derives currentlyAcceptingScreening from canonical booking window and lifecycle openness.
     *
     * @return List of minimal citizen campaign projections
     */
    @Transactional(readOnly = true)
    public List<CitizenCampaignDiscoveryResponse> getCitizenDiscoveryCampaigns() {
        List<VaccinationCampaign> campaigns = campaignRepository.findByStatusInOrderByIdAsc(
                List.of(CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE)
        );
        Instant now = clock.instant();
        return campaigns.stream()
                .map(campaign -> CampaignDtoMapper.toCitizenDiscoveryResponse(
                        campaign,
                        campaign.isBookingWindowOpenAt(now)
                ))
                .toList();
    }
}
