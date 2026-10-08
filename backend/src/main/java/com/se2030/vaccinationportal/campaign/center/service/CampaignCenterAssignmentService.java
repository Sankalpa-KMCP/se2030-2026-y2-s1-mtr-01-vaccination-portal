package com.se2030.vaccinationportal.campaign.center.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages dated center assignments without losing campaign assignment history. */
@Service
@Transactional
public class CampaignCenterAssignmentService {

    private static final Set<SessionLifecycleStatus> BLOCKING_SESSION_STATUSES = EnumSet.of(
            SessionLifecycleStatus.DRAFT,
            SessionLifecycleStatus.SCHEDULED,
            SessionLifecycleStatus.IN_PROGRESS
    );

    private final CampaignCenterAssignmentRepository assignmentRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public CampaignCenterAssignmentService(
            CampaignCenterAssignmentRepository assignmentRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            Clock clock
    ) {
        this.assignmentRepository = assignmentRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    public CampaignCenterAssignment createAssignment(
            Long campaignId,
            Long centerId,
            LocalDate activeFrom,
            LocalDate activeTo
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(centerId, "Center ID is required");
        CampaignCenterAssignment.validatePeriod(activeFrom, activeTo);

        VaccinationCampaign campaign = lockCampaign(campaignId);
        VaccinationCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vaccination center not found with ID: " + centerId));
        rejectActiveOverlap(campaignId, centerId, null, activeFrom, activeTo);

        return assignmentRepository.save(
                new CampaignCenterAssignment(campaign, center, activeFrom, activeTo));
    }

    @Transactional(readOnly = true)
    public Optional<CampaignCenterAssignment> getAssignmentById(Long id) {
        requireId(id, "Campaign-center assignment ID is required");
        return assignmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CampaignCenterAssignment> getAssignmentsForCampaign(Long campaignId) {
        requireId(campaignId, "Campaign ID is required");
        return assignmentRepository.findCampaignHistory(campaignId);
    }

    @Transactional(readOnly = true)
    public List<CampaignCenterAssignment> getAssignmentHistoryForCenter(Long centerId) {
        requireId(centerId, "Center ID is required");
        return assignmentRepository.findCenterHistory(centerId);
    }

    @Transactional(readOnly = true)
    public Optional<CampaignCenterAssignment> getEffectiveAssignmentAt(
            Long campaignId,
            Long centerId,
            LocalDate date
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(centerId, "Center ID is required");
        if (date == null) {
            throw new IllegalArgumentException("Effective assignment date is required");
        }
        return assignmentRepository.findEffectiveAssignmentAt(
                campaignId, centerId, CampaignCenterAssignmentStatus.ACTIVE, date);
    }

    /** Read helper for a later session validator; atomic session writes need the campaign lock. */
    @Transactional(readOnly = true)
    public boolean hasActiveAssignmentCovering(
            Long campaignId,
            Long centerId,
            LocalDate requiredFrom,
            LocalDate requiredToExclusive
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(centerId, "Center ID is required");
        validateRequiredSpan(requiredFrom, requiredToExclusive);
        return assignmentRepository.existsActiveAssignmentCovering(
                campaignId,
                centerId,
                CampaignCenterAssignmentStatus.ACTIVE,
                requiredFrom,
                requiredToExclusive
        );
    }

    public CampaignCenterAssignment updateFutureAssignment(
            Long id,
            LocalDate activeFrom,
            LocalDate activeTo
    ) {
        requireId(id, "Campaign-center assignment ID is required");
        Long campaignId = findCampaignId(id);
        return updateFutureAssignment(campaignId, id, activeFrom, activeTo);
    }

    public CampaignCenterAssignment updateFutureAssignment(
            Long campaignId,
            Long id,
            LocalDate activeFrom,
            LocalDate activeTo
    ) {
        requireId(campaignId, "Campaign ID is required");
        requireId(id, "Campaign-center assignment ID is required");
        CampaignCenterAssignment.validatePeriod(activeFrom, activeTo);

        lockCampaign(campaignId);
        CampaignCenterAssignment target = findAuthoritativeAssignment(id);
        verifyOwnership(target, campaignId);
        requireActive(target);

        LocalDate today = LocalDate.now(clock);
        if (!today.isBefore(target.getActiveFrom())) {
            throw new IllegalArgumentException("A started campaign-center assignment cannot be updated");
        }
        if (!today.isBefore(activeFrom)) {
            throw new IllegalArgumentException("Updated assignment must start in the future");
        }
        rejectActiveOverlap(
                campaignId, target.getCenter().getId(), id, activeFrom, activeTo);

        validateAssignmentReverseImpact(campaignId, target.getCenter().getId(), id, activeFrom, activeTo);

        target.updatePeriod(activeFrom, activeTo);
        return assignmentRepository.save(target);
    }

    public CampaignCenterAssignment endAssignment(Long id, LocalDate activeTo) {
        requireId(id, "Campaign-center assignment ID is required");
        Long campaignId = findCampaignId(id);
        return endAssignment(campaignId, id, activeTo);
    }

    public CampaignCenterAssignment endAssignment(Long campaignId, Long id, LocalDate activeTo) {
        requireId(campaignId, "Campaign ID is required");
        requireId(id, "Campaign-center assignment ID is required");
        if (activeTo == null) {
            throw new IllegalArgumentException("Assignment active-to date is required");
        }

        lockCampaign(campaignId);
        CampaignCenterAssignment target = findAuthoritativeAssignment(id);
        verifyOwnership(target, campaignId);
        requireActive(target);

        LocalDate today = LocalDate.now(clock);
        if (!target.isEffectiveOn(today)) {
            throw new IllegalArgumentException("Only a currently effective assignment can be ended");
        }
        if (!today.isBefore(activeTo)) {
            throw new IllegalArgumentException("Assignment end date must be after today");
        }

        validateAssignmentReverseImpact(campaignId, target.getCenter().getId(), id, target.getActiveFrom(), activeTo);

        target.endAt(activeTo);
        return assignmentRepository.save(target);
    }

    public CampaignCenterAssignment cancelFutureAssignment(Long id) {
        requireId(id, "Campaign-center assignment ID is required");
        Long campaignId = findCampaignId(id);
        return cancelFutureAssignment(campaignId, id);
    }

    public CampaignCenterAssignment cancelFutureAssignment(Long campaignId, Long id) {
        requireId(campaignId, "Campaign ID is required");
        requireId(id, "Campaign-center assignment ID is required");

        lockCampaign(campaignId);
        CampaignCenterAssignment target = findAuthoritativeAssignment(id);
        verifyOwnership(target, campaignId);
        requireActive(target);

        LocalDate today = LocalDate.now(clock);
        if (!today.isBefore(target.getActiveFrom())) {
            throw new IllegalArgumentException("A started campaign-center assignment cannot be cancelled");
        }

        validateAssignmentCancellationReverseImpact(campaignId, target.getCenter().getId(), id);

        target.cancel();
        return assignmentRepository.save(target);
    }

    private void validateAssignmentReverseImpact(
            Long campaignId,
            Long centerId,
            Long targetAssignmentId,
            LocalDate proposedActiveFrom,
            LocalDate proposedActiveTo
    ) {
        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCampaignAndCenter(
                campaignId, centerId, BLOCKING_SESSION_STATUSES, clock.instant()
        );
        for (VaccinationSession session : blockingSessions) {
            ZoneId zoneId = ZoneId.of(session.getScheduleTimezone());
            ZonedDateTime startZdt = session.getStartAt().atZone(zoneId);
            LocalDate firstTouchedDate = startZdt.toLocalDate();
            Instant lastContainedInstant = session.getEndAt().minusNanos(1);
            LocalDate lastTouchedDate = lastContainedInstant.atZone(zoneId).toLocalDate();
            LocalDate requiredToExclusive = lastTouchedDate.plusDays(1);

            boolean isCoveredByProposedTarget = (!proposedActiveFrom.isAfter(firstTouchedDate))
                    && (proposedActiveTo == null || !requiredToExclusive.isAfter(proposedActiveTo));

            if (!isCoveredByProposedTarget) {
                boolean isCoveredBySibling = assignmentRepository.existsActiveAssignmentCovering(
                        campaignId,
                        centerId,
                        CampaignCenterAssignmentStatus.ACTIVE,
                        targetAssignmentId,
                        firstTouchedDate,
                        requiredToExclusive
                );
                if (!isCoveredBySibling) {
                    throw new IllegalArgumentException(
                            "Cannot modify campaign-center assignment because active or scheduled session(s) would fall outside active assignment coverage"
                    );
                }
            }
        }
    }

    private void validateAssignmentCancellationReverseImpact(
            Long campaignId,
            Long centerId,
            Long cancelledAssignmentId
    ) {
        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCampaignAndCenter(
                campaignId, centerId, BLOCKING_SESSION_STATUSES, clock.instant()
        );

        for (VaccinationSession session : blockingSessions) {
            ZoneId zoneId = ZoneId.of(session.getScheduleTimezone());
            ZonedDateTime startZdt = session.getStartAt().atZone(zoneId);
            LocalDate firstTouchedDate = startZdt.toLocalDate();
            Instant lastContainedInstant = session.getEndAt().minusNanos(1);
            LocalDate lastTouchedDate = lastContainedInstant.atZone(zoneId).toLocalDate();
            LocalDate requiredToExclusive = lastTouchedDate.plusDays(1);

            boolean isCoveredBySibling = assignmentRepository.existsActiveAssignmentCovering(
                    campaignId,
                    centerId,
                    CampaignCenterAssignmentStatus.ACTIVE,
                    cancelledAssignmentId,
                    firstTouchedDate,
                    requiredToExclusive
            );
            if (!isCoveredBySibling) {
                throw new IllegalArgumentException(
                        "Cannot cancel campaign-center assignment because active or scheduled session(s) depend on it"
                );
            }
        }
    }

    private Long findCampaignId(Long assignmentId) {
        return assignmentRepository.findCampaignIdById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign-center assignment not found with ID: " + assignmentId));
    }

    private VaccinationCampaign lockCampaign(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found with ID: " + campaignId));
    }

    private CampaignCenterAssignment findAuthoritativeAssignment(Long assignmentId) {
        CampaignCenterAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign-center assignment not found with ID: " + assignmentId));
        if (entityManager != null) {
            entityManager.refresh(assignment);
        }
        return assignment;
    }

    private void rejectActiveOverlap(
            Long campaignId,
            Long centerId,
            Long excludedId,
            LocalDate activeFrom,
            LocalDate activeTo
    ) {
        if (assignmentRepository.existsOverlappingAssignment(
                campaignId,
                centerId,
                CampaignCenterAssignmentStatus.ACTIVE,
                excludedId,
                activeFrom,
                activeTo
        )) {
            throw new IllegalArgumentException(
                    "Campaign-center assignment overlaps an active assignment");
        }
    }

    private static void requireActive(CampaignCenterAssignment assignment) {
        if (assignment.getStatus() != CampaignCenterAssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active campaign-center assignment can be changed");
        }
    }

    private static void verifyOwnership(CampaignCenterAssignment assignment, Long campaignId) {
        if (!campaignId.equals(assignment.getCampaign().getId())) {
            throw new ResourceNotFoundException("Campaign-center assignment " + assignment.getId() + " does not belong to campaign " + campaignId);
        }
    }

    private static void validateRequiredSpan(
            LocalDate requiredFrom,
            LocalDate requiredToExclusive
    ) {
        if (requiredFrom == null || requiredToExclusive == null) {
            throw new IllegalArgumentException("Required assignment date span is required");
        }
        if (!requiredFrom.isBefore(requiredToExclusive)) {
            throw new IllegalArgumentException("Required assignment date span must be positive");
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
