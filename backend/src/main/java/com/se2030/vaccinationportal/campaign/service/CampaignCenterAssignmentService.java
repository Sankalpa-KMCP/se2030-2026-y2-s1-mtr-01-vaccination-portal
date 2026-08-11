package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages retained campaign-center authorization history and its active-overlap invariant. */
@Service
@Transactional
public class CampaignCenterAssignmentService {

    private final CampaignCenterAssignmentRepository assignmentRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final VaccinationCenterRepository centerRepository;
    private final Clock clock;

    public CampaignCenterAssignmentService(
            CampaignCenterAssignmentRepository assignmentRepository,
            VaccinationCampaignRepository campaignRepository,
            VaccinationCenterRepository centerRepository,
            Clock clock
    ) {
        this.assignmentRepository = assignmentRepository;
        this.campaignRepository = campaignRepository;
        this.centerRepository = centerRepository;
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
                .orElseThrow(() -> new IllegalArgumentException(
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
        CampaignCenterAssignment.validatePeriod(activeFrom, activeTo);

        Long campaignId = findCampaignId(id);
        lockCampaign(campaignId);
        CampaignCenterAssignment target = findAssignment(id);
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

        target.updatePeriod(activeFrom, activeTo);
        return assignmentRepository.save(target);
    }

    public CampaignCenterAssignment endAssignment(Long id, LocalDate activeTo) {
        requireId(id, "Campaign-center assignment ID is required");
        if (activeTo == null) {
            throw new IllegalArgumentException("Assignment active-to date is required");
        }

        Long campaignId = findCampaignId(id);
        lockCampaign(campaignId);
        CampaignCenterAssignment target = findAssignment(id);
        verifyOwnership(target, campaignId);
        requireActive(target);

        LocalDate today = LocalDate.now(clock);
        if (!target.isEffectiveOn(today)) {
            throw new IllegalArgumentException("Only a currently effective assignment can be ended");
        }
        if (!today.isBefore(activeTo)) {
            throw new IllegalArgumentException("Assignment end date must be after today");
        }

        target.endAt(activeTo);
        return assignmentRepository.save(target);
    }

    public CampaignCenterAssignment cancelFutureAssignment(Long id) {
        requireId(id, "Campaign-center assignment ID is required");

        Long campaignId = findCampaignId(id);
        lockCampaign(campaignId);
        CampaignCenterAssignment target = findAssignment(id);
        verifyOwnership(target, campaignId);
        requireActive(target);

        LocalDate today = LocalDate.now(clock);
        if (!today.isBefore(target.getActiveFrom())) {
            throw new IllegalArgumentException("A started campaign-center assignment cannot be cancelled");
        }

        target.cancel();
        return assignmentRepository.save(target);
    }

    private Long findCampaignId(Long assignmentId) {
        return assignmentRepository.findCampaignIdById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campaign-center assignment not found with ID: " + assignmentId));
    }

    private VaccinationCampaign lockCampaign(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campaign not found with ID: " + campaignId));
    }

    private CampaignCenterAssignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campaign-center assignment not found with ID: " + assignmentId));
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
            throw new IllegalArgumentException("Campaign-center assignment ownership changed during update");
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
