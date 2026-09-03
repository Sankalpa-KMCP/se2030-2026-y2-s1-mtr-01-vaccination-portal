package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningTriggeredRuleEvidence;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.ConsentNoticeVersionRepository;
import com.se2030.vaccinationportal.screening.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningAnswerRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningTriggeredRuleEvidenceRepository;
import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionResubmissionTests {

    private static final long SUBMISSION_ID = 50L;
    private static final long VERSION_ID = 20L;
    private static final long REQUIRED_QUESTION_ID = 40L;
    private static final long OPTIONAL_QUESTION_ID = 41L;

    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    @Mock
    private ScreeningAnswerRepository answerRepository;

    @Mock
    private QuestionnaireVersionRepository versionRepository;

    @Mock
    private QuestionnaireItemRepository itemRepository;

    @Mock
    private ConsentNoticeVersionRepository consentNoticeVersionRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private PreliminaryScreeningService preliminaryScreeningService;

    @Mock
    private EligibilityRuleRepository eligibilityRuleRepository;

    @Mock
    private ScreeningTriggeredRuleEvidenceRepository evidenceRepository;

    @Mock
    private CampaignQuestionnaireAssignmentService campaignQuestionnaireAssignmentService;

    @Mock
    private EligibilityReviewService eligibilityReviewService;

    private ScreeningSubmissionService service;
    private QuestionnaireVersion questionnaireVersion;
    private ConsentNoticeVersion consentNoticeVersion;
    private ScreeningQuestion requiredQuestion;
    private ScreeningQuestion optionalQuestion;

    @BeforeEach
    void setUp() {
        ScreeningAnswerValidator answerValidator = new ScreeningAnswerValidator(answerOptionRepository);
        Clock clock = Clock.fixed(Instant.parse("2026-08-22T08:30:00Z"), ZoneOffset.UTC);
        service = new ScreeningSubmissionService(
                submissionRepository,
                answerRepository,
                versionRepository,
                itemRepository,
                consentNoticeVersionRepository,
                answerValidator,
                preliminaryScreeningService,
                eligibilityRuleRepository,
                evidenceRepository,
                campaignQuestionnaireAssignmentService,
                eligibilityReviewService,
                clock);

        questionnaireVersion = new QuestionnaireVersion();
        questionnaireVersion.setId(VERSION_ID);
        questionnaireVersion.setStatus(QuestionnaireVersionStatus.PUBLISHED);

        consentNoticeVersion = new ConsentNoticeVersion();
        consentNoticeVersion.setId(30L);

        requiredQuestion = question(REQUIRED_QUESTION_ID, "DETAILS", true);
        optionalQuestion = question(OPTIONAL_QUESTION_ID, "OPTIONAL_DETAILS", false);
    }

    @Test
    void resubmitCorrectionsReplacesAnswersRecalculatesPreliminaryResultAndReturnsToPendingReview() {
        ScreeningSubmission submission = correctionsRequiredSubmission();
        ScreeningAnswer originalAnswer = new ScreeningAnswer(submission, requiredQuestion, "original details");
        submission.addAnswer(originalAnswer);

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(java.util.Optional.of(submission));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(item(requiredQuestion, 1)));
        when(answerRepository.save(any(ScreeningAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(eq(VERSION_ID), anyList()))
                .thenReturn(passResult());
        when(submissionRepository.save(submission)).thenReturn(submission);

        ScreeningSubmission result = service.resubmitCorrections(
                SUBMISSION_ID,
                Map.of(REQUIRED_QUESTION_ID, "  corrected details  "));

        assertSame(submission, result);
        assertEquals(SUBMISSION_ID, result.getId());
        assertEquals(1L, result.getCitizenId());
        assertEquals(2L, result.getCampaignId());
        assertSame(questionnaireVersion, result.getQuestionnaireVersion());
        assertSame(consentNoticeVersion, result.getConsentNoticeVersion());
        assertEquals(ScreeningSubmissionStatus.PENDING_REVIEW, result.getStatus());
        assertEquals(PreliminaryScreeningResult.PRELIMINARY_PASS, result.getPreliminaryResult());
        assertEquals(1, result.getAnswers().size());

        ScreeningAnswer correctedAnswer = result.getAnswers().getFirst();
        assertSame(requiredQuestion, correctedAnswer.getQuestion());
        assertEquals("corrected details", correctedAnswer.getTypedAnswer());

        verify(preliminaryScreeningService).evaluateAnswers(eq(VERSION_ID), anyList());
        verify(evidenceRepository).deleteBySubmissionId(SUBMISSION_ID);
        verify(evidenceRepository).flush();
        verify(evidenceRepository, never()).saveAll(any());
        verify(answerRepository).save(any(ScreeningAnswer.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    void resubmitCorrectionsRejectsInvalidAnswersWithoutMutatingSubmission() {
        ScreeningSubmission submission = correctionsRequiredSubmission();
        ScreeningAnswer originalAnswer = new ScreeningAnswer(submission, requiredQuestion, "original details");
        submission.addAnswer(originalAnswer);

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(java.util.Optional.of(submission));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(item(requiredQuestion, 1)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.resubmitCorrections(SUBMISSION_ID, Map.of(999L, "not in questionnaire")));

        assertTrue(exception.getMessage().contains("questionnaire"));
        assertEquals(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED, submission.getStatus());
        assertEquals(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED, submission.getPreliminaryResult());
        assertEquals(1, submission.getAnswers().size());
        assertSame(originalAnswer, submission.getAnswers().getFirst());
        assertEquals("original details", originalAnswer.getTypedAnswer());

        verify(answerRepository, never()).save(any(ScreeningAnswer.class));
        verify(submissionRepository, never()).save(any(ScreeningSubmission.class));
        verify(preliminaryScreeningService, never()).evaluateAnswers(any(Long.class), anyList());
        verify(evidenceRepository, never()).deleteBySubmissionId(any(Long.class));
        verify(evidenceRepository, never()).saveAll(any());
    }

    @Test
    void resubmitCorrectionsRejectsSubmissionNotInCorrectionsRequired() {
        ScreeningSubmission submission = correctionsRequiredSubmission();
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        ScreeningAnswer originalAnswer = new ScreeningAnswer(submission, requiredQuestion, "original details");
        submission.addAnswer(originalAnswer);

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(java.util.Optional.of(submission));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.resubmitCorrections(
                        SUBMISSION_ID,
                        Map.of(REQUIRED_QUESTION_ID, "corrected details")));

        assertTrue(exception.getMessage().contains("CORRECTIONS_REQUIRED"));
        assertEquals(ScreeningSubmissionStatus.PENDING_REVIEW, submission.getStatus());
        assertEquals(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED, submission.getPreliminaryResult());
        assertEquals(1, submission.getAnswers().size());
        assertSame(originalAnswer, submission.getAnswers().getFirst());

        verify(itemRepository, never())
                .findByQuestionnaireVersionIdOrderByDisplayOrderAsc(any(Long.class));
        verify(answerRepository, never()).save(any(ScreeningAnswer.class));
        verify(submissionRepository, never()).save(any(ScreeningSubmission.class));
        verify(preliminaryScreeningService, never()).evaluateAnswers(any(Long.class), anyList());
        verify(evidenceRepository, never()).deleteBySubmissionId(any(Long.class));
        verify(evidenceRepository, never()).saveAll(any());
    }

    @Test
    void resubmitCorrectionsRemovesPreviouslyAnsweredOptionalQuestionWhenOmitted() {
        ScreeningSubmission submission = correctionsRequiredSubmission();
        submission.addAnswer(new ScreeningAnswer(submission, requiredQuestion, "original details"));
        submission.addAnswer(new ScreeningAnswer(submission, optionalQuestion, "old optional answer"));

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(java.util.Optional.of(submission));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(item(requiredQuestion, 1), item(optionalQuestion, 2)));
        when(answerRepository.save(any(ScreeningAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(eq(VERSION_ID), anyList()))
                .thenReturn(passResult());
        when(submissionRepository.save(submission)).thenReturn(submission);

        ScreeningSubmission result = service.resubmitCorrections(
                SUBMISSION_ID,
                Map.of(REQUIRED_QUESTION_ID, "replacement required answer"));

        assertEquals(1, result.getAnswers().size());
        assertEquals(REQUIRED_QUESTION_ID, result.getAnswers().getFirst().getQuestion().getId());
        assertEquals("replacement required answer", result.getAnswers().getFirst().getTypedAnswer());
        assertFalse(result.getAnswers().stream()
                .anyMatch(answer -> OPTIONAL_QUESTION_ID == answer.getQuestion().getId()));
    }

    @Test
    void resubmitCorrectionsReplacesOldEvidenceWithNewTriggeredRule() {
        ScreeningSubmission submission = correctionsRequiredSubmission();
        submission.addAnswer(new ScreeningAnswer(submission, requiredQuestion, "original details"));

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(java.util.Optional.of(submission));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(item(requiredQuestion, 1)));
        when(answerRepository.save(any(ScreeningAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        EligibilityRule sourceRule = new EligibilityRule();
        sourceRule.setId(61L);
        when(eligibilityRuleRepository.getReferenceById(61L)).thenReturn(sourceRule);
        when(preliminaryScreeningService.evaluateAnswers(eq(VERSION_ID), anyList()))
                .thenReturn(new PreliminaryScreeningService.RuleEvaluationResult(
                        PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                        List.of(new PreliminaryScreeningService.TriggeredRuleResult(
                                61L,
                                "REVIEW-CORRECTED",
                                "Corrected answer requires review",
                                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                                15,
                                1
                        ))
                ));
        when(submissionRepository.save(submission)).thenReturn(submission);

        ScreeningSubmission result = service.resubmitCorrections(
                SUBMISSION_ID,
                Map.of(REQUIRED_QUESTION_ID, "corrected details")
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScreeningTriggeredRuleEvidence>> evidenceCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(evidenceRepository).deleteBySubmissionId(SUBMISSION_ID);
        verify(evidenceRepository).flush();
        verify(evidenceRepository).saveAll(evidenceCaptor.capture());
        assertThatEvidenceMatches(evidenceCaptor.getValue(), submission, sourceRule);
        assertSame(submission, result);
        assertEquals(SUBMISSION_ID, result.getId());
    }

    @Test
    void evaluationFailureDoesNotReplaceExistingEvidence() {
        ScreeningSubmission submission = correctionsRequiredSubmission();
        submission.addAnswer(new ScreeningAnswer(submission, requiredQuestion, "original details"));

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(java.util.Optional.of(submission));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(item(requiredQuestion, 1)));
        when(answerRepository.save(any(ScreeningAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(eq(VERSION_ID), anyList()))
                .thenThrow(new IllegalStateException("evaluation failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.resubmitCorrections(
                        SUBMISSION_ID,
                        Map.of(REQUIRED_QUESTION_ID, "corrected details")
                )
        );

        assertEquals("evaluation failed", exception.getMessage());
        verify(evidenceRepository, never()).deleteBySubmissionId(any(Long.class));
        verify(evidenceRepository, never()).saveAll(any());
    }

    private ScreeningSubmission correctionsRequiredSubmission() {
        ScreeningSubmission submission = new ScreeningSubmission(
                1L,
                2L,
                questionnaireVersion,
                consentNoticeVersion,
                Instant.parse("2026-08-20T10:15:30Z"));
        submission.setId(SUBMISSION_ID);
        submission.setStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        submission.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED);
        return submission;
    }

    private QuestionnaireItem item(ScreeningQuestion question, int displayOrder) {
        return new QuestionnaireItem(questionnaireVersion, question, displayOrder, null);
    }

    private ScreeningQuestion question(long id, String code, boolean required) {
        ScreeningQuestion question = new ScreeningQuestion(code, code, QuestionType.TEXT, required, true);
        question.setId(id);
        return question;
    }

    private PreliminaryScreeningService.RuleEvaluationResult passResult() {
        return new PreliminaryScreeningService.RuleEvaluationResult(
                PreliminaryScreeningResult.PRELIMINARY_PASS,
                List.of());
    }

    private void assertThatEvidenceMatches(
            Iterable<ScreeningTriggeredRuleEvidence> evidenceRows,
            ScreeningSubmission submission,
            EligibilityRule sourceRule
    ) {
        org.assertj.core.api.Assertions.assertThat(evidenceRows).singleElement().satisfies(evidence -> {
            assertSame(submission, evidence.getSubmission());
            assertSame(sourceRule, evidence.getSourceRule());
            assertEquals("REVIEW-CORRECTED", evidence.getRuleCode());
            assertEquals("Corrected answer requires review", evidence.getMessage());
            assertEquals(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED, evidence.getPreliminaryResult());
            assertEquals(15, evidence.getPriority());
            assertEquals(1, evidence.getEvaluationOrder());
        });
    }
}
