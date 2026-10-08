package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ScreeningQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningConfigurationServiceMutabilityTests {

    @Mock
    private ScreeningQuestionRepository questionRepository;

    @Mock
    private AnswerOptionRepository optionRepository;

    @Mock
    private QuestionnaireRepository questionnaireRepository;

    private ScreeningConfigurationService service;
    private ScreeningQuestion question;
    private AnswerOption option;

    @BeforeEach
    void setUp() {
        service = new ScreeningConfigurationService(
                questionRepository,
                optionRepository,
                questionnaireRepository,
                new QuestionnaireMutabilityValidator(questionRepository)
        );
        question = new ScreeningQuestion("ALLERGY", "Do you have allergies?", QuestionType.SINGLE_CHOICE, true, true);
        question.setId(10L);
        option = new AnswerOption(question, "YES", "Yes", "true", 1);
        option.setId(20L);
    }

    @Test
    void unusedQuestionCanBeUpdated() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);

        ScreeningQuestion updated = service.updateQuestion(10L, "Updated wording", "Updated description", false, true);

        assertThat(updated.getText()).isEqualTo("Updated wording");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.isDefaultRequired()).isFalse();
    }

    @Test
    void unusedQuestionCanBeDeleted() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        service.deleteQuestion(10L);

        verify(questionRepository).delete(question);
    }

    @Test
    void questionInProtectedVersionCannotBeUpdated() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.isReferencedInProtectedQuestionnaireVersion(10L, QuestionnaireVersionStatus.DRAFT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateQuestion(10L, "Changed meaning", null, true, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published or retired questionnaire version");

        verify(questionRepository, never()).save(any());
    }

    @Test
    void questionInProtectedVersionCannotBeDeleted() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.isReferencedInProtectedQuestionnaireVersion(10L, QuestionnaireVersionStatus.DRAFT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deleteQuestion(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published or retired questionnaire version");

        verify(questionRepository, never()).delete(any());
    }

    @Test
    void questionWithHistoricalAnswerCannotBeUpdated() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.isReferencedInScreeningAnswers(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateQuestion(10L, "Changed meaning", null, true, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("historical screening answers");

        verify(questionRepository, never()).save(any());
    }

    @Test
    void optionInProtectedVersionCannotBeUpdated() {
        when(optionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(questionRepository.isReferencedInProtectedQuestionnaireVersion(10L, QuestionnaireVersionStatus.DRAFT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateAnswerOption(20L, "NO", "No", "false", 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published or retired questionnaire version");

        verify(optionRepository, never()).save(any());
    }

    @Test
    void optionInProtectedVersionCannotBeDeleted() {
        when(optionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(questionRepository.isReferencedInProtectedQuestionnaireVersion(10L, QuestionnaireVersionStatus.DRAFT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.removeAnswerOption(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published or retired questionnaire version");

        verify(optionRepository, never()).delete(any());
    }

    @Test
    void optionWithHistoricalAnswerCannotBeDeleted() {
        when(optionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(questionRepository.isReferencedInScreeningAnswers(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.removeAnswerOption(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("historical screening answers");

        verify(optionRepository, never()).delete(any());
    }

    @Test
    void historicalOptionProtectionCannotBeBypassedThroughUpdate() {
        when(optionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(questionRepository.isReferencedInScreeningAnswers(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateAnswerOption(20L, "YES", "No", "false", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("historical screening answers");

        assertThat(option.getLabel()).isEqualTo("Yes");
        assertThat(option.getTypedValue()).isEqualTo("true");
        verify(optionRepository, never()).save(any());
    }

    @Test
    void optionCannotBeAddedToProtectedQuestion() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.isReferencedInProtectedQuestionnaireVersion(10L, QuestionnaireVersionStatus.DRAFT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addAnswerOption(10L, "NO", "No", "false", 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("published or retired questionnaire version");

        verify(optionRepository, never()).save(any());
    }
}
