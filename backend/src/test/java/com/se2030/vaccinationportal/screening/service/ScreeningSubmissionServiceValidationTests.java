package com.se2030.vaccinationportal.screening.service;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                preliminaryScreeningService
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
    void invalidSubmissionDoesNotPersistSubmissionOrPartialAnswers() {
        QuestionnaireItem item = new QuestionnaireItem(version, question, 1, null);
        stubSubmissionPrerequisites(List.of(item));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(99L, "unrelated")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Question 99 is not part of questionnaire version 20");

        verify(submissionRepository, never()).save(any());
        verify(answerRepository, never()).save(any());
        verify(preliminaryScreeningService, never()).evaluateAnswers(any(), any());
    }

    private void stubSubmissionPrerequisites(List<QuestionnaireItem> items) {
        when(submissionRepository.findActiveSubmission(any(), any(), any())).thenReturn(Optional.empty());
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(consentRepository.findById(30L)).thenReturn(Optional.of(consent));
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L)).thenReturn(items);
    }
}
