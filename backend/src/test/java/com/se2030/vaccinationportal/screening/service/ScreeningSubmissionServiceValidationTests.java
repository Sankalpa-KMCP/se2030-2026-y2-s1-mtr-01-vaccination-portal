package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.ConsentNoticeVersionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningAnswerRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private ConsentNoticeVersionRepository consentRepository;
    @Mock
    private AnswerOptionRepository optionRepository;
    @Mock
    private PreliminaryScreeningService preliminaryScreeningService;
    @Mock
    private CampaignQuestionnaireAssignmentService assignmentService;

    private ScreeningSubmissionService service;
    private QuestionnaireVersion version;
    private ConsentNoticeVersion consent;
    private ScreeningQuestion question;

    @BeforeEach
    void setUp() {
        service = new ScreeningSubmissionService(
                submissionRepository,
                answerRepository,
                versionRepository,
                itemRepository,
                consentRepository,
                new ScreeningAnswerValidator(optionRepository),
                preliminaryScreeningService,
                assignmentService,
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
        when(submissionRepository.save(any(ScreeningSubmission.class))).thenAnswer(invocation -> {
            ScreeningSubmission submission = invocation.getArgument(0);
            submission.setId(50L);
            return submission;
        });
        when(answerRepository.save(any(ScreeningAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(any(), any())).thenReturn(
                new PreliminaryScreeningService.RuleEvaluationResult(
                        PreliminaryScreeningResult.PRELIMINARY_PASS,
                        List.of(),
                        List.of()
                )
        );

        ScreeningSubmission submission = service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "  valid details  "));

        assertThat(submission.getAnswers()).hasSize(1);
        assertThat(submission.getAnswers().getFirst().getTypedAnswer()).isEqualTo("valid details");
        assertThat(submission.getPreliminaryResult()).isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS);
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
    }

    private void stubSubmissionPrerequisites(List<QuestionnaireItem> items) {
        stubPublishedVersionPrerequisites();
        when(consentRepository.findById(30L)).thenReturn(Optional.of(consent));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L)).thenReturn(items);
    }

    private void stubPublishedVersionPrerequisites() {
        when(submissionRepository.findActiveSubmission(any(), any(), any())).thenReturn(Optional.empty());
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
    }

    private void stubEffectiveAssignment(Long questionnaireVersionId) {
        CampaignQuestionnaireAssignment assignment = mock(CampaignQuestionnaireAssignment.class);
        lenient().when(assignment.getQuestionnaireVersionId()).thenReturn(questionnaireVersionId);
        lenient().when(assignmentService.getEffectiveAssignmentAt(eq(2L), any(LocalDate.class)))
                .thenReturn(Optional.of(assignment));
    }

}
