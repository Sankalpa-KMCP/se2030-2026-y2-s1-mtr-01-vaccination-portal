package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    public EligibilityReviewService(
            EligibilityAssessmentRepository assessmentRepository,
            ScreeningSubmissionRepository submissionRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.submissionRepository = submissionRepository;
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

        return assessmentRepository.findByCitizenIdAndCampaignIdAndStatus(citizenId, campaignId, EligibilityAssessmentStatus.ELIGIBLE)
                .map(assessment -> {
                    if (assessment.getRevokedAt() != null) {
                        return false;
                    }
                    if (assessment.getValidUntil() != null && assessment.getValidUntil().isBefore(Instant.now())) {
                        return false;
                    }
                    return true;
                })
                .orElse(false);
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
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        if (submission.getStatus() != ScreeningSubmissionStatus.PENDING_REVIEW && submission.getStatus() != ScreeningSubmissionStatus.CORRECTIONS_REQUIRED) {
            throw new IllegalStateException("Cannot review submission in " + submission.getStatus() + " status");
        }

        Instant now = Instant.now();
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
                now,
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
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        if (submission.getStatus() != ScreeningSubmissionStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Can only request corrections for submissions in PENDING_REVIEW status");
        }

        submission.setStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        return initializeSubmissionResponseRelations(submissionRepository.save(submission));
    }

    /**
     * Revokes an existing ELIGIBLE assessment.
     */
    public EligibilityAssessment revokeAssessment(Long assessmentId, Long reviewerId, String revocationReason) {
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID is required");
        if (reviewerId == null) throw new IllegalArgumentException("Reviewer ID is required");
        if (revocationReason == null || revocationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Revocation reason is required");
        }

        EligibilityAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with ID: " + assessmentId));

        if (assessment.getStatus() != EligibilityAssessmentStatus.ELIGIBLE) {
            throw new IllegalStateException("Only active ELIGIBLE assessments can be revoked");
        }

        assessment.setStatus(EligibilityAssessmentStatus.REVOKED);
        assessment.setRevokedAt(Instant.now());
        assessment.setRevocationReason(revocationReason.trim());

        return assessmentRepository.save(assessment);
    }

    /**
     * Authorizes a citizen to undergo rescreening following an INELIGIBLE, EXPIRED, or REVOKED assessment.
     */
    public EligibilityAssessment authorizeRescreening(Long assessmentId, Long authorizedByReviewerId, String reason) {
        if (assessmentId == null) throw new IllegalArgumentException("Assessment ID is required");
        if (authorizedByReviewerId == null) throw new IllegalArgumentException("Reviewer ID is required");
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rescreening authorization reason is required");
        }

        EligibilityAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with ID: " + assessmentId));

        assessment.setRescreeningAuthorizedAt(Instant.now());
        assessment.setRescreeningAuthorizedBy(authorizedByReviewerId);
        assessment.setRescreeningReason(reason.trim());

        return assessmentRepository.save(assessment);
    }

    private ScreeningSubmission initializeSubmissionResponseRelations(ScreeningSubmission submission) {
        submission.getAnswers().size();
        return submission;
    }
}
