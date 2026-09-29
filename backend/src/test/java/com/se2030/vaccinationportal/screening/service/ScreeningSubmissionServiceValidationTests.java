package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.api.screening.QuestionnaireAssignmentSnapshot;
import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
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
import com.se2030.vaccinationportal.screening.model.ScreeningTriggeredRuleEvidence;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionServiceValidationTests {

    @Mock
    private ScreeningSubmissionRepository submissionRepository;
    @Mock
    private ScreeningAnswerRepository answerRepository;
    @Mock
    private QuestionnaireVersionRepository versionRepository;
    @Mock
    private QuestionnaireItemRepository itemRepository;
    @Mock
    private ScreeningConsentNoticeService consentNoticeService;
    @Mock
    private AnswerOptionRepository optionRepository;
    @Mock
    private PreliminaryScreeningService preliminaryScreeningService;
    @Mock
    private EligibilityRuleRepository eligibilityRuleRepository;
    @Mock
    private ScreeningTriggeredRuleEvidenceRepository evidenceRepository;
    @Mock
    private CampaignScreeningReader assignmentService;
    @Mock
    private EligibilityReviewService eligibilityReviewService;

    private ScreeningSubmissionService service;
    private ScreeningAnswerValidator answerValidator;
    private QuestionnaireVersion version;
    private ConsentNoticeVersion consent;
    private ScreeningQuestion question;

    @BeforeEach
    void setUp() {
        answerValidator = spy(new ScreeningAnswerValidator(optionRepository));
        service = new ScreeningSubmissionService(
                submissionRepository,
                answerRepository,
                versionRepository,
                itemRepository,
                consentNoticeService,
                answerValidator,
                preliminaryScreeningService,
                eligibilityRuleRepository,
                evidenceRepository,
                assignmentService,
                eligibilityReviewService,
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC)
        );
        version = new QuestionnaireVersion();
        version.setId(20L);
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        consent = new ConsentNoticeVersion("NOTICE", 1, "Consent", "Content", "hash", Instant.now());
        consent.setId(30L);
        question = new ScreeningQuestion("DETAILS", "Provide details", QuestionType.TEXT, true, true);
        question.setId(40L);
    }

    @Test
    void validCompleteSubmissionSucceedsAndUsesValidatedAnswer() {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, null);
        stubSubmissionPrerequisites(List.of(item));
        stubEffectiveAssignment(20L);
        when(submissionRepository.saveAndFlush(any(ScreeningSubmission.class))).thenAnswer(invocation -> {
            ScreeningSubmission submission = invocation.getArgument(0);
            submission.setId(50L);
            return submission;
        });
        when(submissionRepository.save(any(ScreeningSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.save(any(ScreeningAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(any(), any())).thenReturn(
                new PreliminaryScreeningService.RuleEvaluationResult(
                        PreliminaryScreeningResult.PRELIMINARY_PASS,
                        List.of()
                )
        );

        ScreeningSubmission submission = service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "  valid details  "));

        assertThat(submission.getAnswers()).hasSize(1);
        assertThat(submission.getAnswers().getFirst().getTypedAnswer()).isEqualTo("valid details");
        assertThat(submission.getPreliminaryResult()).isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS);
        verify(evidenceRepository, never()).saveAll(any());
    }

    @Test
    void triggeredEvidenceIsPersistedWithSnapshotFieldsAndEvaluationOrder() {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, null);
        stubSubmissionPrerequisites(List.of(item));
        stubEffectiveAssignment(20L);
        when(submissionRepository.saveAndFlush(any(ScreeningSubmission.class))).thenAnswer(invocation -> {
            ScreeningSubmission submission = invocation.getArgument(0);
            submission.setId(50L);
            return submission;
        });
        when(submissionRepository.save(any(ScreeningSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.save(any(ScreeningAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EligibilityRule sourceRule = new EligibilityRule();
        sourceRule.setId(60L);
        EligibilityRule secondSourceRule = new EligibilityRule();
        secondSourceRule.setId(61L);
        when(eligibilityRuleRepository.getReferenceById(60L)).thenReturn(sourceRule);
        when(eligibilityRuleRepository.getReferenceById(61L)).thenReturn(secondSourceRule);
        when(preliminaryScreeningService.evaluateAnswers(any(), any())).thenReturn(
                new PreliminaryScreeningService.RuleEvaluationResult(
                        PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                        List.of(
                                new PreliminaryScreeningService.TriggeredRuleResult(
                                        60L,
                                        "RECORD-DETAILS",
                                        "Details were recorded",
                                        PreliminaryScreeningResult.PRELIMINARY_PASS,
                                        10,
                                        1
                                ),
                                new PreliminaryScreeningService.TriggeredRuleResult(
                                        61L,
                                        "REVIEW-DETAILS",
                                        "Details require clinical review",
                                        PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                                        25,
                                        2
                                )
                        )
                )
        );

        ScreeningSubmission submission = service.submitScreening(
                1L,
                2L,
                20L,
                30L,
                Map.of(40L, "valid details")
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScreeningTriggeredRuleEvidence>> evidenceCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(evidenceRepository).saveAll(evidenceCaptor.capture());
        assertThat(evidenceCaptor.getValue()).satisfiesExactly(
                evidence -> {
                    assertThat(evidence.getSubmission()).isSameAs(submission);
                    assertThat(evidence.getSourceRule()).isSameAs(sourceRule);
                    assertThat(evidence.getRuleCode()).isEqualTo("RECORD-DETAILS");
                    assertThat(evidence.getMessage()).isEqualTo("Details were recorded");
                    assertThat(evidence.getPreliminaryResult())
                            .isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS);
                    assertThat(evidence.getPriority()).isEqualTo(10);
                    assertThat(evidence.getEvaluationOrder()).isEqualTo(1);
                },
                evidence -> {
                    assertThat(evidence.getSubmission()).isSameAs(submission);
                    assertThat(evidence.getSourceRule()).isSameAs(secondSourceRule);
                    assertThat(evidence.getRuleCode()).isEqualTo("REVIEW-DETAILS");
                    assertThat(evidence.getMessage()).isEqualTo("Details require clinical review");
                    assertThat(evidence.getPreliminaryResult())
                            .isEqualTo(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED);
                    assertThat(evidence.getPriority()).isEqualTo(25);
                    assertThat(evidence.getEvaluationOrder()).isEqualTo(2);
                }
        );
        assertThat(submission.getPreliminaryResult())
                .isEqualTo(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED);
    }

    @Test
    void evaluationFailureDoesNotPersistPartialEvidence() {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, null);
        stubSubmissionPrerequisites(List.of(item));
        stubEffectiveAssignment(20L);
        when(submissionRepository.saveAndFlush(any(ScreeningSubmission.class))).thenAnswer(invocation -> {
            ScreeningSubmission submission = invocation.getArgument(0);
            submission.setId(50L);
            return submission;
        });
        lenient().when(submissionRepository.save(any(ScreeningSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.save(any(ScreeningAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(any(), any()))
                .thenThrow(new IllegalStateException("evaluation failed"));

        assertThatThrownBy(() -> service.submitScreening(
                1L,
                2L,
                20L,
                30L,
                Map.of(40L, "valid details")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("evaluation failed");

        verify(evidenceRepository, never()).saveAll(any());
        verify(eligibilityRuleRepository, never()).getReferenceById(any());
    }

    @Test
    void submitScreeningRejectsQuestionnaireVersionNotAssignedToCampaign() {
        stubPublishedVersionPrerequisites();
        stubEffectiveAssignment(21L);

        Throwable failure = catchThrowable(() ->
                service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details"))
        );

        assertAll(
                () -> assertThat(failure)
                        .as("a questionnaire version that is not assigned to the campaign must be rejected")
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("not assigned"),
                () -> verify(submissionRepository, never()).save(any()),
                () -> verify(answerRepository, never()).save(any()),
                () -> verify(preliminaryScreeningService, never()).evaluateAnswers(any(), any())
        );
    }

    @Test
    void submitScreeningRejectsCampaignWithoutEffectiveQuestionnaireAssignment() {
        stubPublishedVersionPrerequisites();
        lenient().when(assignmentService.getEffectiveAssignmentAt(eq(2L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Throwable failure = catchThrowable(() ->
                service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details"))
        );

        assertAll(
                () -> assertThat(failure)
                        .as("a campaign without an effective questionnaire assignment must be rejected")
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("effective questionnaire assignment"),
                () -> verify(submissionRepository, never()).save(any()),
                () -> verify(answerRepository, never()).save(any())
        );
    }

    @Test
    void invalidSubmissionDoesNotPersistSubmissionOrPartialAnswers() {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, null);
        stubSubmissionPrerequisites(List.of(item));
        stubEffectiveAssignment(20L);

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(99L, "unrelated")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Question 99 is not part of questionnaire version 20");

        verify(submissionRepository, never()).save(any());
        verify(answerRepository, never()).save(any());
        verify(preliminaryScreeningService, never()).evaluateAnswers(any(), any());
        verify(evidenceRepository, never()).saveAll(any());
    }

    @Test
    void submitScreeningRejectsUnrelatedActiveConsentNoticeId() {
        stubSubmissionPrerequisites(List.of(new QuestionnaireItem(version, question, 1, null)));
        stubEffectiveAssignment(20L);

        assertThatThrownBy(() ->
                service.submitScreening(1L, 2L, 20L, 901L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("current PRE_VACCINATION_SCREENING notice");

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitScreeningRejectsOlderNonRetiredCanonicalConsentWhenNewerVersionIsCurrent() {
        consent.setVersionNumber(2);
        stubSubmissionPrerequisites(List.of(new QuestionnaireItem(version, question, 1, null)));
        stubEffectiveAssignment(20L);

        assertThatThrownBy(() ->
                service.submitScreening(1L, 2L, 20L, 29L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reload the screening form");

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitScreeningRejectsRetiredCanonicalConsentId() {
        stubSubmissionPrerequisites(List.of(new QuestionnaireItem(version, question, 1, null)));
        stubEffectiveAssignment(20L);

        assertThatThrownBy(() ->
                service.submitScreening(1L, 2L, 20L, 28L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("current PRE_VACCINATION_SCREENING notice");

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitScreeningReportsMissingCurrentCanonicalConsent() {
        stubPublishedVersionPrerequisites();
        stubEffectiveAssignment(20L);
        when(consentNoticeService.getCurrentScreeningConsentNotice())
                .thenThrow(new ResourceNotFoundException(
                        "No current consent notice exists for screening notice code PRE_VACCINATION_SCREENING"
                ));

        assertThatThrownBy(() ->
                service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PRE_VACCINATION_SCREENING");

        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitScreeningRejectsExistingActiveSubmissionBeforeDownstreamWorkOrPersistence() {
        ScreeningSubmission activeSubmission = new ScreeningSubmission();
        activeSubmission.setId(50L);
        when(submissionRepository.findActiveSubmissionForUpdate(any(), any(), any()))
                .thenReturn(Optional.of(activeSubmission));

        assertThatThrownBy(() ->
                service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active submission");

        verify(eligibilityReviewService, never()).validateScreeningAllowed(any(), any());
        verify(versionRepository, never()).findById(any());
        verify(assignmentService, never()).getEffectiveAssignmentAt(any(), any());
        verify(consentNoticeService, never()).getCurrentScreeningConsentNotice();
        verify(itemRepository, never()).findByQuestionnaireVersionIdOrderByDisplayOrderAsc(any());
        verify(answerValidator, never()).validateAndNormalize(any(), anyList(), anyMap());
        verify(preliminaryScreeningService, never()).evaluateAnswers(any(), any());
        verify(submissionRepository, never()).save(any());
        verify(answerRepository, never()).save(any());
        verify(evidenceRepository, never()).saveAll(any());
    }

    private void stubSubmissionPrerequisites(List<QuestionnaireItem> items) {
        stubPublishedVersionPrerequisites();
        when(consentNoticeService.getCurrentScreeningConsentNotice()).thenReturn(consent);
        lenient().when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L)).thenReturn(items);
    }

    private void stubPublishedVersionPrerequisites() {
        when(submissionRepository.findActiveSubmissionForUpdate(any(), any(), any())).thenReturn(Optional.empty());
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
    }

    private void stubEffectiveAssignment(Long questionnaireVersionId) {
        QuestionnaireAssignmentSnapshot assignment = new QuestionnaireAssignmentSnapshot(1L, 2L, questionnaireVersionId);
        lenient().when(assignmentService.getEffectiveAssignmentAt(eq(2L), any(LocalDate.class)))
                .thenReturn(Optional.of(assignment));
    }

}
