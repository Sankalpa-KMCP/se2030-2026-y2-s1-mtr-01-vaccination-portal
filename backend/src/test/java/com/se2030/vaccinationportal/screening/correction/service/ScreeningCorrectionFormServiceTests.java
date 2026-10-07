package com.se2030.vaccinationportal.screening.correction.service;

import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;


import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningCorrectionFormServiceTests {

    private static final long SUBMISSION_ID = 7L;
    private static final long CITIZEN_ID = 6L;
    private static final long VERSION_ID = 70L;

    @Mock private ScreeningSubmissionRepository submissions;
    @Mock private QuestionnaireItemRepository items;
    @Mock private AnswerOptionRepository options;
    @Mock private CitizenScreeningAccessService access;

    private ScreeningCorrectionFormService service;

    @BeforeEach
    void setUp() {
        service = new ScreeningCorrectionFormService(submissions, items, options, access);
    }

    @Test
    void buildsCompleteStoredVersionFormWithOrderedMetadataOptionsAndPrefills() {
        QuestionnaireVersion storedVersion = version();
        ScreeningSubmission submission = submission(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);

        ScreeningQuestion bool = question(11L, "BOOL", QuestionType.BOOLEAN, false);
        ScreeningQuestion single = question(12L, "SINGLE", QuestionType.SINGLE_CHOICE, true);
        ScreeningQuestion multi = question(13L, "MULTI", QuestionType.MULTI_CHOICE, true);
        ScreeningQuestion numeric = question(14L, "NUMBER", QuestionType.NUMERIC, true);
        ScreeningQuestion date = question(15L, "DATE", QuestionType.DATE, true);
        ScreeningQuestion text = question(16L, "TEXT", QuestionType.TEXT, true);
        ScreeningQuestion optional = question(17L, "OPTIONAL", QuestionType.TEXT, false);

        submission.addAnswer(new ScreeningAnswer(submission, bool, "false"));
        submission.addAnswer(new ScreeningAnswer(submission, single, "single-b"));
        submission.addAnswer(new ScreeningAnswer(submission, multi, "green,red,legacy"));
        submission.addAnswer(new ScreeningAnswer(submission, numeric, "37.5"));
        submission.addAnswer(new ScreeningAnswer(submission, date, "2026-09-05"));
        submission.addAnswer(new ScreeningAnswer(submission, text, "Current notes"));

        List<QuestionnaireItem> storedItems = List.of(
                new QuestionnaireItem(storedVersion, bool, 1, true),
                new QuestionnaireItem(storedVersion, single, 2, false),
                new QuestionnaireItem(storedVersion, multi, 3, null),
                new QuestionnaireItem(storedVersion, numeric, 4, null),
                new QuestionnaireItem(storedVersion, date, 5, null),
                new QuestionnaireItem(storedVersion, text, 6, null),
                new QuestionnaireItem(storedVersion, optional, 7, null)
        );
        when(submissions.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(items.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID)).thenReturn(storedItems);

        ScreeningQuestion foreignQuestion = question(99L, "FOREIGN", QuestionType.SINGLE_CHOICE, false);
        AnswerOption singleA = option(single, 121L, "A", "First", "single-a", 1);
        AnswerOption singleB = option(single, 122L, "B", "Second", "single-b", 2);
        AnswerOption foreign = option(foreignQuestion, 991L, "FOREIGN", "Foreign", "single-b", 0);
        when(options.findByQuestionIdOrderByDisplayOrderAsc(11L)).thenReturn(List.of());
        when(options.findByQuestionIdOrderByDisplayOrderAsc(12L))
                .thenReturn(List.of(singleB, foreign, singleA));
        AnswerOption red = option(multi, 131L, "RED", "Red", "red", 1);
        AnswerOption green = option(multi, 132L, "GREEN", "Green", "green", 2);
        when(options.findByQuestionIdOrderByDisplayOrderAsc(13L)).thenReturn(List.of(green, red));
        when(options.findByQuestionIdOrderByDisplayOrderAsc(14L)).thenReturn(List.of());
        when(options.findByQuestionIdOrderByDisplayOrderAsc(15L)).thenReturn(List.of());
        when(options.findByQuestionIdOrderByDisplayOrderAsc(16L)).thenReturn(List.of());
        when(options.findByQuestionIdOrderByDisplayOrderAsc(17L)).thenReturn(List.of());

        var result = service.getCorrectionForm(SUBMISSION_ID);

        assertThat(result.submissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(result.campaignId()).isEqualTo(3L);
        assertThat(result.questionnaireVersionId()).isEqualTo(VERSION_ID);
        assertThat(result.questionnaireVersionNumber()).isEqualTo(4);
        assertThat(result.status()).isEqualTo(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        assertThat(result.correctionInstructions()).isEqualTo("Correct the requested answers");
        assertThat(result.correctionRequestedAt()).isEqualTo(Instant.parse("2026-09-05T02:00:00Z"));
        assertThat(result.questions()).extracting(question -> question.id())
                .containsExactly(11L, 12L, 13L, 14L, 15L, 16L, 17L);
        assertThat(result.questions()).extracting(question -> question.displayOrder())
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(result.questions()).extracting(question -> question.required())
                .containsExactly(true, false, true, true, true, true, false);
        assertThat(result.questions().get(0).currentAnswer().typedAnswer()).isEqualTo("false");
        assertThat(result.questions().get(1).options()).extracting(option -> option.code())
                .containsExactly("A", "B");
        assertThat(result.questions().get(1).currentAnswer().selectedOptions())
                .extracting(option -> option.code()).containsExactly("B");
        assertThat(result.questions().get(2).currentAnswer().typedAnswer()).isEqualTo("green,red,legacy");
        assertThat(result.questions().get(2).currentAnswer().selectedOptions())
                .extracting(option -> option.code()).containsExactly("GREEN", "RED");
        assertThat(result.questions().get(3).currentAnswer().typedAnswer()).isEqualTo("37.5");
        assertThat(result.questions().get(4).currentAnswer().typedAnswer()).isEqualTo("2026-09-05");
        assertThat(result.questions().get(5).currentAnswer().typedAnswer()).isEqualTo("Current notes");
        assertThat(result.questions().get(6).currentAnswer()).isNull();

        verify(access).requireCitizenIdAccess(CITIZEN_ID, "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR");
        verify(items).findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID);
        verify(submissions, never()).save(submission);
    }

    @ParameterizedTest
    @EnumSource(value = ScreeningSubmissionStatus.class, names = {
            "DRAFT", "PENDING_REVIEW", "REVIEWED"
    })
    void rejectsSubmissionOutsideCorrectionsRequired(ScreeningSubmissionStatus status) {
        when(submissions.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission(status)));

        assertThatThrownBy(() -> service.getCorrectionForm(SUBMISSION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORRECTIONS_REQUIRED");

        verifyNoInteractions(items, options);
    }

    @Test
    void ownershipFailureStopsBeforeHistoricalFormHydration() {
        ScreeningSubmission submission = submission(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        when(submissions.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        doThrow(new AccessDeniedException("Not the owner")).when(access).requireCitizenIdAccess(
                CITIZEN_ID, "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR");

        assertThatThrownBy(() -> service.getCorrectionForm(SUBMISSION_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(items, options);
    }

    @Test
    void missingSubmissionStopsBeforeOwnershipAndHydration() {
        when(submissions.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCorrectionForm(SUBMISSION_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(access, items, options);
    }

    @Test
    void nullSubmissionIdIsRejectedBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getCorrectionForm(null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(submissions, access, items, options);
    }

    private ScreeningSubmission submission(ScreeningSubmissionStatus status) {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(SUBMISSION_ID);
        submission.setCitizenId(CITIZEN_ID);
        submission.setCampaignId(3L);
        submission.setQuestionnaireVersion(version());
        submission.setStatus(status);
        submission.setCorrectionInstructions("Correct the requested answers");
        submission.setCorrectionRequestedAt(Instant.parse("2026-09-05T02:00:00Z"));
        return submission;
    }

    private QuestionnaireVersion version() {
        QuestionnaireVersion version = new QuestionnaireVersion(null, 4, QuestionnaireVersionStatus.RETIRED);
        version.setId(VERSION_ID);
        return version;
    }

    private ScreeningQuestion question(Long id, String code, QuestionType type, boolean defaultRequired) {
        ScreeningQuestion question = new ScreeningQuestion(
                code, "Question " + code, type, defaultRequired, false
        );
        question.setId(id);
        question.setDescription("Description " + code);
        return question;
    }

    private AnswerOption option(
            ScreeningQuestion question,
            Long id,
            String code,
            String label,
            String typedValue,
            int displayOrder
    ) {
        AnswerOption option = new AnswerOption(question, code, label, typedValue, displayOrder);
        option.setId(id);
        return option;
    }
}
