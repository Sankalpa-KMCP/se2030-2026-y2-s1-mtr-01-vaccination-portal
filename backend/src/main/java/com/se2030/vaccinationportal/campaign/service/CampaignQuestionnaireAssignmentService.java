package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing effective-dated assignments of published questionnaire versions to vaccination campaigns.
 * Defined by canonical specification WBVP-SE2030 section 2, 5, 6, 12 (MF02-BR-02 / MF02-BR-17).
 *
 * <p>Enforces:</p>
 * <ul>
 *   <li>Questionnaire versions must be in PUBLISHED status via {@link QuestionnaireVersionPublicationPort}.</li>
 *   <li>No overlapping effective periods for the same campaign (MF02-BR-02).</li>
 *   <li>Atomic version transition: closing previous open-ended version at {@code effectiveStartDate - 1 day}.</li>
 *   <li>Immutable historical retention without hard deletion.</li>
 * </ul>
 */
@Service
@Transactional
public class CampaignQuestionnaireAssignmentService {

    private final CampaignQuestionnaireAssignmentRepository assignmentRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final QuestionnaireVersionPublicationPort publicationPort;
    private final Clock clock;

    public CampaignQuestionnaireAssignmentService(
            CampaignQuestionnaireAssignmentRepository assignmentRepository,
            VaccinationCampaignRepository campaignRepository,
            @Autowired(required = false) QuestionnaireVersionPublicationPort publicationPort,
            Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.campaignRepository = campaignRepository;
        this.publicationPort = publicationPort;
        this.clock = clock;
    }

    /**
     * Assigns a published questionnaire version to a campaign effective from the specified start date.
     * If an open-ended assignment currently exists, it is atomically closed on the day prior to {@code effectiveStartDate}.
     *
     * @param campaignId Unique identifier of the VaccinationCampaign
     * @param questionnaireVersionId Unique identifier of the QuestionnaireVersion in MF-01
     * @param effectiveStartDate Date on which this questionnaire version becomes effective for screening
     * @return Saved CampaignQuestionnaireAssignment entity
     */
    public CampaignQuestionnaireAssignment assignQuestionnaireVersion(
            Long campaignId,
            Long questionnaireVersionId,
            LocalDate effectiveStartDate) {

        VaccinationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));

        if (questionnaireVersionId == null || questionnaireVersionId <= 0) {
            throw new IllegalArgumentException("Questionnaire version ID must be positive: " + questionnaireVersionId);
        }

        if (effectiveStartDate == null) {
            throw new IllegalArgumentException("Effective start date is required");
        }

        // Verify with MF-01 publication port that version exists and is in PUBLISHED status
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

        // Handle existing open-ended assignment closure and overlap prevention
        Optional<CampaignQuestionnaireAssignment> currentOpenEndedOpt = assignmentRepository.findOpenEndedAssignment(campaignId);

        if (currentOpenEndedOpt.isPresent()) {
            CampaignQuestionnaireAssignment current = currentOpenEndedOpt.get();

            LocalDate today = LocalDate.now(clock);
            if (effectiveStartDate.isBefore(today)) {
                throw new IllegalArgumentException(
                        "Questionnaire assignment effective date cannot be in the past [MF02-BR-17]: " + effectiveStartDate
                );
            }

            if (!effectiveStartDate.isAfter(current.getActiveFrom())) {
                throw new IllegalArgumentException(
                        "Effective start date (" + effectiveStartDate +
                        ") must be strictly after the current assignment activeFrom date (" + current.getActiveFrom() + ")"
                );
            }

            // Check for overlaps excluding the open-ended version we are about to close
            List<CampaignQuestionnaireAssignment> overlaps = assignmentRepository.findOverlappingAssignments(
                    campaignId,
                    effectiveStartDate,
                    null,
                    current.getId()
            );
            if (!overlaps.isEmpty()) {
                throw new IllegalArgumentException(
                        "New questionnaire assignment date range overlaps with an existing assignment for campaign ID: " + campaignId
                );
            }

            // Atomically close previous open-ended version at effectiveStartDate - 1 day
            current.setActiveTo(effectiveStartDate.minusDays(1));
            assignmentRepository.save(current);
        } else {
            // Check for overlaps across all existing bounded versions
            List<CampaignQuestionnaireAssignment> overlaps = assignmentRepository.findOverlappingAssignments(
                    campaignId,
                    effectiveStartDate,
                    null,
                    null
            );
            if (!overlaps.isEmpty()) {
                throw new IllegalArgumentException(
                        "New questionnaire assignment date range overlaps with an existing assignment for campaign ID: " + campaignId
                );
            }
        }

        CampaignQuestionnaireAssignment newAssignment = new CampaignQuestionnaireAssignment(
                campaign,
                questionnaireVersionId,
                effectiveStartDate,
                null
        );

        return assignmentRepository.save(newAssignment);
    }

    /**
     * Resolves the effective questionnaire assignment for a campaign on a specific evaluation date.
     *
     * @param campaignId Unique identifier of the VaccinationCampaign
     * @param date Evaluation date
     * @return Optional containing the effective assignment if found
     */
    @Transactional(readOnly = true)
    public Optional<CampaignQuestionnaireAssignment> getEffectiveAssignmentAt(Long campaignId, LocalDate date) {
        if (campaignId == null || date == null) {
            return Optional.empty();
        }
        return assignmentRepository.findEffectiveAssignmentAt(campaignId, date);
    }

    /**
     * Resolves the open-ended questionnaire assignment for a campaign if present.
     * Note: Open-ended does not imply effective today if activeFrom is in the future.
     *
     * @param campaignId Unique identifier of the VaccinationCampaign
     * @return Optional containing the open-ended assignment
     */
    @Transactional(readOnly = true)
    public Optional<CampaignQuestionnaireAssignment> getOpenEndedAssignment(Long campaignId) {
        if (campaignId == null) {
            return Optional.empty();
        }
        return assignmentRepository.findOpenEndedAssignment(campaignId);
    }

    /**
     * Retrieves the complete version history of questionnaire assignments for a campaign.
     *
     * @param campaignId Unique identifier of the VaccinationCampaign
     * @return List of assignments ordered by activeFrom descending
     */
    @Transactional(readOnly = true)
    public List<CampaignQuestionnaireAssignment> getAssignmentHistory(Long campaignId) {
        if (campaignId == null) {
            return List.of();
        }
        return assignmentRepository.findByCampaignIdOrderByActiveFromDescIdDesc(campaignId);
    }

    /**
     * Retrieves all assignments linking to a specific questionnaire version ID.
     *
     * @param questionnaireVersionId Unique identifier of the QuestionnaireVersion in MF-01
     * @return List of assignments
     */
    @Transactional(readOnly = true)
    public List<CampaignQuestionnaireAssignment> getAssignmentsForQuestionnaireVersion(Long questionnaireVersionId) {
        if (questionnaireVersionId == null) {
            return List.of();
        }
        return assignmentRepository.findByQuestionnaireVersionId(questionnaireVersionId);
    }
}
