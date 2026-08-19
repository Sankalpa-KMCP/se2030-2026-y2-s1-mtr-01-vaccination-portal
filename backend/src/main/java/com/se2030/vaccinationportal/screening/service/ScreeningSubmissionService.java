package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

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
    private final ScreeningQuestionRepository questionRepository;
    private final PreliminaryScreeningService preliminaryScreeningService;

    public ScreeningSubmissionService(
            ScreeningSubmissionRepository submissionRepository,
            ScreeningAnswerRepository answerRepository,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            ConsentNoticeVersionRepository consentNoticeVersionRepository,
            ScreeningQuestionRepository questionRepository,
            PreliminaryScreeningService preliminaryScreeningService
    ) {
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.consentNoticeVersionRepository = consentNoticeVersionRepository;
        this.questionRepository = questionRepository;
        this.preliminaryScreeningService = preliminaryScreeningService;
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

        // 2. Validate Questionnaire Version (must be PUBLISHED)
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire version not found with ID: " + versionId));

        if (version.getStatus() != QuestionnaireVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Submissions are only allowed for PUBLISHED questionnaire versions");
        }

        // 3. Validate Consent Notice
        ConsentNoticeVersion consentNotice = consentNoticeVersionRepository.findById(consentNoticeVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Consent notice version not found with ID: " + consentNoticeVersionId));

        if (consentNotice.getRetiredAt() != null) {
            throw new IllegalStateException("The accepted consent notice version is retired. Please accept the latest active notice.");
        }

        // 4. Validate Required Questionnaire Items
        List<QuestionnaireItem> items = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        for (QuestionnaireItem item : items) {
            boolean isRequired = item.getRequiredOverride() != null
                    ? item.getRequiredOverride()
                    : item.getQuestion().isDefaultRequired();

            if (isRequired) {
                String ans = answerMap.get(item.getQuestion().getId());
                if (ans == null || ans.trim().isEmpty()) {
                    throw new IllegalArgumentException("Answer required for question: " + item.getQuestion().getText());
                }
            }
        }

        // 5. Create Submission Entity
        Instant now = Instant.now();
        ScreeningSubmission submission = new ScreeningSubmission(citizenId, campaignId, version, consentNotice, now);
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        submission.setSubmittedAt(now);
        ScreeningSubmission savedSubmission = submissionRepository.save(submission);

        // 6. Persist Answers
        List<ScreeningAnswer> persistedAnswers = new ArrayList<>();
        for (Map.Entry<Long, String> entry : answerMap.entrySet()) {
            ScreeningQuestion question = questionRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + entry.getKey()));

            ScreeningAnswer answer = new ScreeningAnswer(savedSubmission, question, entry.getValue().trim());
            savedSubmission.addAnswer(answer);
            persistedAnswers.add(answerRepository.save(answer));
        }

        // 7. Evaluate Preliminary Result
        PreliminaryScreeningService.RuleEvaluationResult eval = preliminaryScreeningService.evaluateAnswers(versionId, persistedAnswers);
        savedSubmission.setPreliminaryResult(eval.overallResult());

        return initializeResponseRelations(submissionRepository.save(savedSubmission));
    }

    private ScreeningSubmission initializeResponseRelations(ScreeningSubmission submission) {
        submission.getAnswers().size();
        return submission;
    }
}
