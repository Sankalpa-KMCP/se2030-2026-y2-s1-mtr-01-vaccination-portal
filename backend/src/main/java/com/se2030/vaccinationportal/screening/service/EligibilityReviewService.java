package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewDetailsResponse;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.mapper.ScreeningReviewDetailsMapper;
import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningTriggeredRuleEvidenceRepository;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing Healthcare Officer clinical reviews and final eligibility decisions.
 * Enforces business rules:
 * - Only Healthcare Officer recorded assessments become final.
 * - Assessment stores citizen, campaign, submission, questionnaire version, reviewer, status, reason, reviewed_at and valid_until.
 * - Manages active validity windows, revocations, and rescreening authorizations.
 */
@Service
@Transactional
public class EligibilityReviewService {

    private final EligibilityAssessmentRepository assessmentRepository;
    private final ScreeningSubmissionRepository submissionRepository;
    private final QuestionnaireItemRepository questionnaireItemRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ScreeningTriggeredRuleEvidenceRepository evidenceRepository;
    private final ScreeningReviewDetailsMapper reviewDetailsMapper;
    private final VaccinationCampaignService campaignService;
    private final Clock clock;
    private final UserAccountRepository userAccountRepository;

    public EligibilityReviewService(
            EligibilityAssessmentRepository assessmentRepository,
            ScreeningSubmissionRepository submissionRepository,
            QuestionnaireItemRepository questionnaireItemRepository,
            AnswerOptionRepository answerOptionRepository,
            ScreeningTriggeredRuleEvidenceRepository evidenceRepository,
            ScreeningReviewDetailsMapper reviewDetailsMapper,
            VaccinationCampaignService campaignService,
            Clock clock,
            UserAccountRepository userAccountRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.submissionRepository = submissionRepository;
        this.questionnaireItemRepository = questionnaireItemRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.evidenceRepository = evidenceRepository;
        this.reviewDetailsMapper = reviewDetailsMapper;
        this.campaignService = campaignService;
        this.clock = clock;
        this.userAccountRepository = userAccountRepository;
    }

    // ==========================================
    // Review Queue & Assessment Retrieval
    // ==========================================

    @Transactional(readOnly = true)
    public List<ScreeningSubmission> getReviewQueue(Long campaignId) {
        List<ScreeningSubmission> submissions;
        if (campaignId != null) {
            submissions = submissionRepository.findByCampaignIdAndStatus(campaignId, ScreeningSubmissionStatus.PENDING_REVIEW);
        } else {
            submissions = submissionRepository.findByStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        }
        submissions.forEach(this::initializeSubmissionResponseRelations);
        return submissions;
    }

    @Transactional(readOnly = true)
    public ScreeningReviewDetailsResponse getReviewDetails(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }

        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screening submission not found with ID: " + submissionId
                ));
        submission.getAnswers().size();

        Long questionnaireVersionId = submission.getQuestionnaireVersion().getId();
        List<QuestionnaireItem> questionnaireItems = questionnaireItemRepository
                .findByQuestionnaireVersionIdOrderByDisplayOrderAsc(questionnaireVersionId);

        Map<Long, List<AnswerOption>> optionsByQuestionId = new HashMap<>();
        for (QuestionnaireItem item : questionnaireItems) {
            Long questionId = item.getQuestion().getId();
            boolean hasPersistedAnswer = submission.getAnswers().stream()
                    .anyMatch(answer -> questionId.equals(answer.getQuestion().getId()));
            QuestionType questionType = item.getQuestion().getQuestionType();
            if (hasPersistedAnswer
                    && (questionType == QuestionType.SINGLE_CHOICE
                    || questionType == QuestionType.MULTI_CHOICE)) {
                optionsByQuestionId.put(
                        questionId,
                        answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId)
                );
            }
        }

        List<ScreeningTriggeredRuleEvidence> evidence = evidenceRepository
                .findBySubmissionIdOrderByEvaluationOrderAsc(submissionId);
        EligibilityAssessment assessment = assessmentRepository.findBySubmissionId(submissionId).orElse(null);
        String citizenFullName = userAccountRepository.findById(submission.getCitizenId())
                .map(UserAccount::getFullName)
                .orElse(null);

        return reviewDetailsMapper.toResponse(
                submission,
                citizenFullName,
                questionnaireItems,
                optionsByQuestionId,
                evidence,
                assessment
        );
    }

    @Transactional(readOnly = true)
    public Optional<EligibilityAssessment> getAssessmentBySubmissionId(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        return assessmentRepository.findBySubmissionId(submissionId);
    }

    @Transactional(readOnly = true)
    public List<EligibilityAssessment> getAssessmentsForCitizen(Long citizenId) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID cannot be null");
        }
        return assessmentRepository.findByCitizenIdOrderByReviewedAtDesc(citizenId);
    }

    /**
     * Checks if a citizen holds an active, valid ELIGIBLE assessment for a specific campaign.
     */
    @Transactional(readOnly = true)
    public boolean isCitizenCurrentlyEligible(Long citizenId, Long campaignId) {
        if (citizenId == null || campaignId == null) {
            return false;
        }

        List<EligibilityAssessment> assessments = assessmentRepository.findByCitizenIdAndCampaignId(citizenId, campaignId);
        if (assessments.isEmpty()) {
            return false;
        }

        return isAssessmentCurrentlyEligible(assessments.get(0), campaignId);
    }

    /**
     * Enforces MF01-BR-13 before a citizen starts a new screening submission.
     */
    @Transactional(readOnly = true)
    public void validateScreeningAllowed(Long citizenId, Long campaignId) {
        if (citizenId == null || campaignId == null) {
            throw new IllegalArgumentException("Citizen ID and Campaign ID are required");
        }

        List<EligibilityAssessment> assessments = assessmentRepository.findByCitizenIdAndCampaignId(citizenId, campaignId);
        if (assessments.isEmpty()) {
            return;
        }

        EligibilityAssessment latestAssessment = assessments.get(0);
        switch (latestAssessment.getStatus()) {
            case ELIGIBLE -> {
                if (isAssessmentCurrentlyEligible(latestAssessment, campaignId)) {
                    throw new IllegalStateException(
                            "New screening is not allowed while the latest assessment is currently ELIGIBLE"
                    );
                }
            }
            case INELIGIBLE -> {
                if (latestAssessment.getRescreeningAuthorizedAt() == null) {
                    throw new IllegalStateException(
                            "New screening requires rescreening authorization for the latest INELIGIBLE assessment"
                    );
                }
            }
            case REVOKED -> {
                if (latestAssessment.getRescreeningAuthorizedAt() == null) {
                    throw new IllegalStateException(
                            "New screening requires rescreening authorization for the latest REVOKED assessment"
                    );
                }
            }
            case EXPIRED -> {
                // Expired eligibility permits a new screening without manual authorization.
            }
        }
    }

    // ==========================================
    // Healthcare Officer Review Actions
    // ==========================================

    /**
     * Records a final eligibility assessment for a pending submission.
     *
     * @param submissionId ID of the ScreeningSubmission
     * @param reviewerId ID of the reviewing Healthcare Officer
     * @param decisionStatus ELIGIBLE or INELIGIBLE
     * @param reason Clinical reason for the decision
     * @param clinicalNotes Optional internal notes
     * @param validUntil Expiration timestamp for the eligibility
     * @return Saved EligibilityAssessment
     */
    public EligibilityAssessment recordDecision(
            Long submissionId,
            Long reviewerId,
            EligibilityAssessmentStatus decisionStatus,
            String reason,
            String clinicalNotes,
            Instant validUntil
    ) {
        if (submissionId == null) throw new IllegalArgumentException("Submission ID is required");
        if (reviewerId == null) throw new IllegalArgumentException("Reviewer ID is required");
        if (decisionStatus == null) throw new IllegalArgumentException("Decision status is required");
        if (decisionStatus != EligibilityAssessmentStatus.ELIGIBLE && decisionStatus != EligibilityAssessmentStatus.INELIGIBLE) {
            throw new IllegalArgumentException("Initial decision must be either ELIGIBLE or INELIGIBLE");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Decision reason is mandatory");
        }

        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        if (submission.getStatus() != ScreeningSubmissionStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Final decision requires submission to be in PENDING_REVIEW status");
        }

        Instant reviewedAt = Instant.now(clock);
        if (decisionStatus == EligibilityAssessmentStatus.ELIGIBLE) {
            if (validUntil == null) {
                throw new IllegalArgumentException("validUntil is required for an ELIGIBLE decision");
            }
            if (!validUntil.isAfter(reviewedAt)) {
                throw new IllegalArgumentException("validUntil must be after reviewedAt for an ELIGIBLE decision");
            }
        }

        PreliminaryScreeningResult prelim = submission.getPreliminaryResult() != null
                ? submission.getPreliminaryResult()
                : PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED;

        EligibilityAssessment assessment = new EligibilityAssessment(
                submission,
                submission.getCitizenId(),
                submission.getCampaignId(),
                reviewerId,
                decisionStatus,
                prelim,
                reason.trim(),
                reviewedAt,
                validUntil
        );
        assessment.setClinicalNotes(clinicalNotes != null ? clinicalNotes.trim() : null);

        // Update submission status to REVIEWED
        submission.setStatus(ScreeningSubmissionStatus.REVIEWED);
        submissionRepository.save(submission);

        return assessmentRepository.save(assessment);
    }

    /**
     * Requests corrections from the citizen, placing the submission into CORRECTIONS_REQUIRED status.
     */
    public ScreeningSubmission requestCorrections(Long submissionId, String instructions) {
        if (submissionId == null) throw new IllegalArgumentException("Submission ID is required");
        if (instructions == null || instructions.trim().isEmpty()) {
            throw new IllegalArgumentException("Correction instructions are required");
        }

        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        if (submission.getStatus() != ScreeningSubmissionStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Can only request corrections for submissions in PENDING_REVIEW status");
        }

        submission.setCorrectionInstructions(instructions.trim());
        submission.setCorrectionRequestedAt(Instant.now(clock));
        submission.setStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        return initializeSubmissionResponseRelations(submissionRepository.save(submission));
    }

    /**
     * Revokes an existing ELIGIBLE assessment.
     */
    public EligibilityAssessment revokeAssessment(Long assessmentId, Long revokerId, String revocationReason) {
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID is required");
        if (revokerId == null) throw new IllegalArgumentException("Revoker ID is required");
        if (revocationReason == null || revocationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Revocation reason is required");
        }

        EligibilityAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with ID: " + assessmentId));

        if (assessment.getStatus() != EligibilityAssessmentStatus.ELIGIBLE) {
            throw new IllegalStateException("Only active ELIGIBLE assessments can be revoked");
        }

        assessment.setStatus(EligibilityAssessmentStatus.REVOKED);
        assessment.setRevokedAt(Instant.now(clock));
        assessment.setRevocationReason(revocationReason.trim());

        return assessmentRepository.save(assessment);
    }

    /**
     * Authorizes a citizen to undergo rescreening following an INELIGIBLE or REVOKED assessment.
     */
    public EligibilityAssessment authorizeRescreening(Long assessmentId, Long authorizedByReviewerId, String reason) {
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID is required");
        if (authorizedByReviewerId == null) throw new IllegalArgumentException("Reviewer ID is required");
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rescreening authorization reason is required");
        }

        EligibilityAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with ID: " + assessmentId));

        if (assessment.getStatus() != EligibilityAssessmentStatus.INELIGIBLE
                && assessment.getStatus() != EligibilityAssessmentStatus.REVOKED) {
            throw new IllegalStateException(
                    "Cannot authorize rescreening for an " + assessment.getStatus()
                            + " assessment; only INELIGIBLE or REVOKED assessments are allowed"
            );
        }

        assessment.setRescreeningAuthorizedAt(Instant.now(clock));
        assessment.setRescreeningAuthorizedBy(authorizedByReviewerId);
        assessment.setRescreeningReason(reason.trim());

        return assessmentRepository.save(assessment);
    }

    private boolean isAssessmentCurrentlyEligible(EligibilityAssessment assessment, Long campaignId) {
        if (assessment.getStatus() != EligibilityAssessmentStatus.ELIGIBLE
                || assessment.getRevokedAt() != null) {
            return false;
        }

        Instant now = Instant.now(clock);
        if (assessment.getValidUntil() != null && assessment.getValidUntil().isBefore(now)) {
            return false;
        }

        VaccinationCampaign campaign = campaignService.getRequiredCampaignById(campaignId);
        LocalDate serviceEndDate = campaign.getServiceEndDate();
        return serviceEndDate == null || !LocalDate.now(clock).isAfter(serviceEndDate);
    }

    private ScreeningSubmission initializeSubmissionResponseRelations(ScreeningSubmission submission) {
        submission.getAnswers().size();
        return submission;
    }
}
