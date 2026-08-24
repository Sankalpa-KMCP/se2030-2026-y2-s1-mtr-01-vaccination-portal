package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.*;
import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing citizen screening submissions.
 * Enforces business rules:
 * - One active submission per citizen per campaign.
 * - Mandatory consent capture with exact ConsentNoticeVersion.
 * - Verification of PUBLISHED questionnaire version.
 * - Required answer completeness.
 * - Automatic preliminary rule evaluation calculation.
 */
@Service
@Transactional
public class ScreeningSubmissionService {

    private static final List<ScreeningSubmissionStatus> ACTIVE_SUBMISSION_STATUSES = List.of(
            ScreeningSubmissionStatus.DRAFT,
            ScreeningSubmissionStatus.PENDING_REVIEW,
            ScreeningSubmissionStatus.CORRECTIONS_REQUIRED
    );

    private final ScreeningSubmissionRepository submissionRepository;
    private final ScreeningAnswerRepository answerRepository;
    private final QuestionnaireVersionRepository versionRepository;
    private final QuestionnaireItemRepository itemRepository;
    private final ConsentNoticeVersionRepository consentNoticeVersionRepository;
    private final ScreeningAnswerValidator answerValidator;
    private final PreliminaryScreeningService preliminaryScreeningService;
    private final CampaignQuestionnaireAssignmentService assignmentService;
    private final EligibilityReviewService eligibilityReviewService;
    private final Clock clock;

    public ScreeningSubmissionService(
            ScreeningSubmissionRepository submissionRepository,
            ScreeningAnswerRepository answerRepository,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            ConsentNoticeVersionRepository consentNoticeVersionRepository,
            ScreeningAnswerValidator answerValidator,
            PreliminaryScreeningService preliminaryScreeningService,
            CampaignQuestionnaireAssignmentService assignmentService,
            EligibilityReviewService eligibilityReviewService,
            Clock clock
    ) {
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.consentNoticeVersionRepository = consentNoticeVersionRepository;
        this.answerValidator = answerValidator;
        this.preliminaryScreeningService = preliminaryScreeningService;
        this.assignmentService = assignmentService;
        this.eligibilityReviewService = eligibilityReviewService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScreeningSubmission getSubmissionById(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Screening submission not found with ID: " + submissionId));
        return initializeResponseRelations(submission);
    }

    @Transactional(readOnly = true)
    public List<ScreeningSubmission> getSubmissionsForCitizen(Long citizenId) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID cannot be null");
        }
        List<ScreeningSubmission> submissions = submissionRepository.findByCitizenIdOrderByCreatedAtDesc(citizenId);
        submissions.forEach(this::initializeResponseRelations);
        return submissions;
    }

    @Transactional(readOnly = true)
    public Optional<ScreeningSubmission> getActiveSubmission(Long citizenId, Long campaignId) {
        if (citizenId == null || campaignId == null) {
            throw new IllegalArgumentException("Citizen ID and Campaign ID are required");
        }
        return submissionRepository.findActiveSubmission(citizenId, campaignId, ACTIVE_SUBMISSION_STATUSES)
                .map(this::initializeResponseRelations);
    }

    /**
     * Creates and submits a citizen screening response.
     *
     * @param citizenId Citizen identifier
     * @param campaignId Vaccination campaign identifier
     * @param versionId QuestionnaireVersion ID (must be PUBLISHED)
     * @param consentNoticeVersionId Accepted ConsentNoticeVersion ID
     * @param answerMap Map of question ID -> answer value string
     * @return Completed ScreeningSubmission with calculated preliminary_result
     */
    public ScreeningSubmission submitScreening(
            Long citizenId,
            Long campaignId,
            Long versionId,
            Long consentNoticeVersionId,
            Map<Long, String> answerMap
    ) {
        if (citizenId == null) throw new IllegalArgumentException("Citizen ID is required");
        if (campaignId == null) throw new IllegalArgumentException("Campaign ID is required");
        if (versionId == null) throw new IllegalArgumentException("Questionnaire Version ID is required");
        if (consentNoticeVersionId == null) throw new IllegalArgumentException("Consent Notice Version ID is required");
        if (answerMap == null || answerMap.isEmpty()) throw new IllegalArgumentException("Screening answers are required");

        // 1. Guard: Check for existing active submission
        Optional<ScreeningSubmission> existingActive = submissionRepository.findActiveSubmission(citizenId, campaignId, ACTIVE_SUBMISSION_STATUSES);
        if (existingActive.isPresent()) {
            throw new IllegalStateException("An active submission already exists for this citizen and campaign");
        }

        // 2. Guard: Check assessment lifecycle eligibility under MF01-BR-13
        eligibilityReviewService.validateScreeningAllowed(citizenId, campaignId);

        // 3. Validate Questionnaire Version (must be PUBLISHED)
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire version not found with ID: " + versionId));

        if (version.getStatus() != QuestionnaireVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Submissions are only allowed for PUBLISHED questionnaire versions");
        }

        // 4. Validate that the submitted version is effective for the campaign today
        LocalDate effectiveDate = LocalDate.now(clock);
        CampaignQuestionnaireAssignment effectiveAssignment = assignmentService
                .getEffectiveAssignmentAt(campaignId, effectiveDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Campaign " + campaignId + " has no effective questionnaire assignment on " + effectiveDate
                ));

        if (!Objects.equals(effectiveAssignment.getQuestionnaireVersionId(), versionId)) {
            throw new IllegalStateException(
                    "Questionnaire version " + versionId + " is not assigned to campaign " + campaignId
                            + " on " + effectiveDate + "; effective version is "
                            + effectiveAssignment.getQuestionnaireVersionId()
            );
        }

        // 5. Validate Consent Notice
        ConsentNoticeVersion consentNotice = consentNoticeVersionRepository.findById(consentNoticeVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Consent notice version not found with ID: " + consentNoticeVersionId));

        if (consentNotice.getRetiredAt() != null) {
            throw new IllegalStateException("The accepted consent notice version is retired. Please accept the latest active notice.");
        }

        // 6. Validate all answers before persisting any submission data
        List<QuestionnaireItem> items = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        List<ScreeningAnswerValidator.ValidatedAnswer> validatedAnswers =
                answerValidator.validateAndNormalize(versionId, items, answerMap);

        // 7. Create Submission Entity
        Instant now = Instant.now();
        ScreeningSubmission submission = new ScreeningSubmission(citizenId, campaignId, version, consentNotice, now);
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        submission.setSubmittedAt(now);
        ScreeningSubmission savedSubmission = submissionRepository.save(submission);

        // 8. Persist Answers
        List<ScreeningAnswer> persistedAnswers = new ArrayList<>();
        for (ScreeningAnswerValidator.ValidatedAnswer validatedAnswer : validatedAnswers) {
            ScreeningAnswer answer = new ScreeningAnswer(
                    savedSubmission,
                    validatedAnswer.question(),
                    validatedAnswer.typedAnswer()
            );
            savedSubmission.addAnswer(answer);
            persistedAnswers.add(answerRepository.save(answer));
        }

        // 9. Evaluate Preliminary Result
        PreliminaryScreeningService.RuleEvaluationResult eval = preliminaryScreeningService.evaluateAnswers(versionId, persistedAnswers);
        savedSubmission.setPreliminaryResult(eval.overallResult());

        return initializeResponseRelations(submissionRepository.save(savedSubmission));
    }

    public ScreeningSubmission resubmitCorrections(
            Long submissionId,
            Map<Long, String> correctedAnswers
    ) {
        if (submissionId == null) throw new IllegalArgumentException("Submission ID is required");
        if (correctedAnswers == null || correctedAnswers.isEmpty()) {
            throw new IllegalArgumentException("Corrected screening answers are required");
        }

        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Screening submission not found with ID: " + submissionId
                ));

        if (submission.getStatus() != ScreeningSubmissionStatus.CORRECTIONS_REQUIRED) {
            throw new IllegalStateException(
                    "Corrections can only be resubmitted for submissions in CORRECTIONS_REQUIRED status"
            );
        }

        Long versionId = submission.getQuestionnaireVersion().getId();
        List<QuestionnaireItem> items = itemRepository
                .findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        List<ScreeningAnswerValidator.ValidatedAnswer> validatedAnswers =
                answerValidator.validateAndNormalize(versionId, items, correctedAnswers);

        Map<Long, ScreeningAnswer> existingAnswersByQuestionId = submission.getAnswers().stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        answer -> answer
                ));
        Set<Long> correctedQuestionIds = validatedAnswers.stream()
                .map(validatedAnswer -> validatedAnswer.question().getId())
                .collect(Collectors.toSet());

        new ArrayList<>(submission.getAnswers()).stream()
                .filter(answer -> !correctedQuestionIds.contains(answer.getQuestion().getId()))
                .forEach(submission::removeAnswer);

        List<ScreeningAnswer> persistedAnswers = new ArrayList<>();
        for (ScreeningAnswerValidator.ValidatedAnswer validatedAnswer : validatedAnswers) {
            Long questionId = validatedAnswer.question().getId();
            ScreeningAnswer answer = existingAnswersByQuestionId.get(questionId);
            if (answer == null) {
                answer = new ScreeningAnswer(
                        submission,
                        validatedAnswer.question(),
                        validatedAnswer.typedAnswer()
                );
                submission.addAnswer(answer);
            } else {
                answer.setTypedAnswer(validatedAnswer.typedAnswer());
            }
            persistedAnswers.add(answerRepository.save(answer));
        }

        PreliminaryScreeningService.RuleEvaluationResult eval =
                preliminaryScreeningService.evaluateAnswers(versionId, persistedAnswers);
        submission.setPreliminaryResult(eval.overallResult());
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);

        return initializeResponseRelations(submissionRepository.save(submission));
    }

    private ScreeningSubmission initializeResponseRelations(ScreeningSubmission submission) {
        submission.getAnswers().size();
        return submission;
    }
}
