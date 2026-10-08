package com.se2030.vaccinationportal.campaign.questionnaire.service;

import com.se2030.vaccinationportal.campaign.api.questionnaire.QuestionnaireVersionPublicationPort;


import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.questionnaire.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.questionnaire.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Links campaigns to published questionnaire versions without overlapping assignment periods. */
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
     * If an open-ended assignment exists, it is atomically closed at the exclusive
     * {@code effectiveStartDate} boundary.
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

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        if (publicationPort.hasCampaignSubmissions(campaignId)) {
            throw new IllegalStateException(
                    "Cannot change questionnaire assignment for a campaign with existing citizen screening submissions"
            );
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException(
                    "Questionnaire version cannot be assigned to campaign in " + campaign.getStatus() +
                    " status. Questionnaire assignment is locked once a campaign is scheduled, active, or closed."
            );
        }

        LocalDate today = LocalDate.now(clock);
        if (effectiveStartDate.isBefore(today)) {
            throw new IllegalArgumentException(
                    "Questionnaire assignment effective date cannot be in the past [MF02-BR-17]: " + effectiveStartDate
            );
        }

        Optional<CampaignQuestionnaireAssignment> currentOpenEndedOpt = assignmentRepository.findOpenEndedAssignment(campaignId);

        if (currentOpenEndedOpt.isPresent()) {
            CampaignQuestionnaireAssignment current = currentOpenEndedOpt.get();

            if (current.getQuestionnaireVersionId().equals(questionnaireVersionId)) {
                return current;
            }

            boolean isInitialSingleAssignment = assignmentRepository.findByCampaignIdOrderByActiveFromDescIdDesc(campaignId).size() <= 1;
            if (isInitialSingleAssignment && effectiveStartDate.equals(current.getActiveFrom())) {
                current.setQuestionnaireVersionId(questionnaireVersionId);
                return assignmentRepository.save(current);
            }

            if (!effectiveStartDate.isAfter(current.getActiveFrom())) {
                throw new IllegalArgumentException(
                        "Effective start date (" + effectiveStartDate +
                        ") must be strictly after the current assignment activeFrom date (" + current.getActiveFrom() + ")"
                );
            }
        }

        CampaignQuestionnaireAssignment newAssignment = new CampaignQuestionnaireAssignment(
                campaign,
                questionnaireVersionId,
                effectiveStartDate,
                null
        );

        Long currentId = currentOpenEndedOpt.map(CampaignQuestionnaireAssignment::getId).orElse(null);
        List<CampaignQuestionnaireAssignment> overlaps = assignmentRepository.findOverlappingAssignments(
                campaignId, effectiveStartDate, null, currentId
        );
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(
                    "New questionnaire assignment date range overlaps with an existing assignment for campaign ID: " + campaignId
            );
        }

        currentOpenEndedOpt.ifPresent(current -> {
            current.closeAt(effectiveStartDate);
            assignmentRepository.save(current);
        });

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
