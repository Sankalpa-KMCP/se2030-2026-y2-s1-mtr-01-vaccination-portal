package com.se2030.vaccinationportal.screening.submission.service;

import com.se2030.vaccinationportal.screening.eligibility.model.ScreeningTriggeredRuleEvidence;
import com.se2030.vaccinationportal.screening.eligibility.repository.ScreeningTriggeredRuleEvidenceRepository;
import com.se2030.vaccinationportal.screening.eligibility.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.service.ScreeningConsentNoticeService;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningAnswerRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;


import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
import com.se2030.vaccinationportal.campaign.api.screening.QuestionnaireAssignmentSnapshot;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerValidator;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final ScreeningConsentNoticeService consentNoticeService;
    private final ScreeningAnswerValidator answerValidator;
    private final PreliminaryScreeningService preliminaryScreeningService;
    private final EligibilityRuleRepository eligibilityRuleRepository;
    private final ScreeningTriggeredRuleEvidenceRepository evidenceRepository;
    private final CampaignScreeningReader campaignReader;
    private final EligibilityReviewService eligibilityReviewService;
    private final Clock clock;

    public ScreeningSubmissionService(
            ScreeningSubmissionRepository submissionRepository,
            ScreeningAnswerRepository answerRepository,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            ScreeningConsentNoticeService consentNoticeService,
            ScreeningAnswerValidator answerValidator,
            PreliminaryScreeningService preliminaryScreeningService,
            EligibilityRuleRepository eligibilityRuleRepository,
            ScreeningTriggeredRuleEvidenceRepository evidenceRepository,
            CampaignScreeningReader campaignReader,
            EligibilityReviewService eligibilityReviewService,
            Clock clock
    ) {
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.consentNoticeService = consentNoticeService;
        this.answerValidator = answerValidator;
        this.preliminaryScreeningService = preliminaryScreeningService;
        this.eligibilityRuleRepository = eligibilityRuleRepository;
        this.evidenceRepository = evidenceRepository;
        this.campaignReader = campaignReader;
        this.eligibilityReviewService = eligibilityReviewService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScreeningSubmission getSubmissionById(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening submission not found with ID: " + submissionId));
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

        // 1. Guard: Check for existing active submission under a pessimistic write lock (R05).
        // The locking read serializes concurrent submissions for the same citizen/campaign pair so
        // two requests cannot both observe "no active submission" and insert two active rows.
        Optional<ScreeningSubmission> existingActive = submissionRepository.findActiveSubmissionForUpdate(
                citizenId, campaignId, ACTIVE_SUBMISSION_STATUSES);
        if (existingActive.isPresent()) {
            throw new IllegalStateException("An active submission already exists for this citizen and campaign");
        }

        // 2. Guard: Check assessment lifecycle eligibility under MF01-BR-13
        eligibilityReviewService.validateScreeningAllowed(citizenId, campaignId);

        // 3. Validate Questionnaire Version (must be PUBLISHED)
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire version not found with ID: " + versionId));

        if (version.getStatus() != QuestionnaireVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Submissions are only allowed for PUBLISHED questionnaire versions");
        }

        // 4. Validate that the submitted version is effective for the campaign today
        LocalDate effectiveDate = LocalDate.now(clock);
        QuestionnaireAssignmentSnapshot effectiveAssignment = campaignReader
                .getEffectiveAssignmentAt(campaignId, effectiveDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Campaign " + campaignId + " has no effective questionnaire assignment on " + effectiveDate
                ));

        if (!Objects.equals(effectiveAssignment.questionnaireVersionId(), versionId)) {
            throw new IllegalStateException(
                    "Questionnaire version " + versionId + " is not assigned to campaign " + campaignId
                            + " on " + effectiveDate + "; effective version is "
                            + effectiveAssignment.questionnaireVersionId()
            );
        }

        // 5. Validate the exact current canonical MF-01 screening consent notice.
        ConsentNoticeVersion consentNotice = consentNoticeService.getCurrentScreeningConsentNotice();
        if (!Objects.equals(consentNotice.getId(), consentNoticeVersionId)) {
            throw new IllegalStateException(
                    "The accepted consent notice is not the current "
                            + ScreeningConsentNoticeService.SCREENING_NOTICE_CODE + " notice. "
                            + "Please reload the screening form and accept the current notice."
            );
        }

        // 6. Validate all answers before persisting any submission data
        List<QuestionnaireItem> items = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        List<ScreeningAnswerValidator.ValidatedAnswer> validatedAnswers =
                answerValidator.validateAndNormalize(versionId, items, answerMap);

        // 7. Create Submission Entity
        // The uk_screening_submission_active_citizen_campaign constraint is the database-level backstop
        // (R05): if a concurrent transaction inserts an active submission between the locking check and
        // this insert, the unique violation is translated into the same controlled rejection.
        Instant now = Instant.now();
        ScreeningSubmission submission = new ScreeningSubmission(citizenId, campaignId, version, consentNotice, now);
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        submission.setSubmittedAt(now);
        ScreeningSubmission savedSubmission;
        try {
            savedSubmission = submissionRepository.saveAndFlush(submission);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateActiveSubmissionViolation(exception)) {
                throw new IllegalStateException("An active submission already exists for this citizen and campaign", exception);
            }
            throw exception;
        }

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
        persistTriggeredRuleEvidence(savedSubmission, eval.triggeredRules());

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
                .orElseThrow(() -> new ResourceNotFoundException(
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
        replaceTriggeredRuleEvidence(submission, eval.triggeredRules());
        submission.setPreliminaryResult(eval.overallResult());
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);

        return initializeResponseRelations(submissionRepository.save(submission));
    }

    private void replaceTriggeredRuleEvidence(
            ScreeningSubmission submission,
            List<PreliminaryScreeningService.TriggeredRuleResult> triggeredRules
    ) {
        evidenceRepository.deleteBySubmissionId(submission.getId());
        evidenceRepository.flush();
        persistTriggeredRuleEvidence(submission, triggeredRules);
    }

    private void persistTriggeredRuleEvidence(
            ScreeningSubmission submission,
            List<PreliminaryScreeningService.TriggeredRuleResult> triggeredRules
    ) {
        if (triggeredRules.isEmpty()) {
            return;
        }

        List<ScreeningTriggeredRuleEvidence> evidenceRows = triggeredRules.stream()
                .map(triggeredRule -> new ScreeningTriggeredRuleEvidence(
                        submission,
                        eligibilityRuleRepository.getReferenceById(triggeredRule.sourceRuleId()),
                        triggeredRule.ruleCode(),
                        triggeredRule.message(),
                        triggeredRule.preliminaryResult(),
                        triggeredRule.priority(),
                        triggeredRule.evaluationOrder()
                ))
                .toList();
        evidenceRepository.saveAll(evidenceRows);
    }

    private ScreeningSubmission initializeResponseRelations(ScreeningSubmission submission) {
        submission.getAnswers().size();
        return submission;
    }

    private boolean isDuplicateActiveSubmissionViolation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.toLowerCase(java.util.Locale.ROOT).contains("uk_screening_submission_active_citizen_campaign")) {
                return true;
            }
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null && constraintName.toLowerCase(java.util.Locale.ROOT).contains("uk_screening_submission_active_citizen_campaign")) {
                    return true;
                }
            }
            if (current instanceof java.sql.SQLException sqlEx) {
                String sqlMsg = sqlEx.getMessage();
                if (sqlMsg != null && sqlMsg.toLowerCase(java.util.Locale.ROOT).contains("uk_screening_submission_active_citizen_campaign")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
