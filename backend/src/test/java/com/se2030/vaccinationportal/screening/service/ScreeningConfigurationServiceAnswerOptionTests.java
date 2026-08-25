package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningConfigurationServiceAnswerOptionTests {

    @Mock
    private ScreeningQuestionRepository questionRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private QuestionnaireRepository questionnaireRepository;

    private ScreeningConfigurationService service;
    private ScreeningQuestion question;
    private AnswerOption option;

    @BeforeEach
    void setUp() {
        service = new ScreeningConfigurationService(
                questionRepository,
                answerOptionRepository,
                questionnaireRepository,
                new QuestionnaireMutabilityValidator(questionRepository)
        );

        question = new ScreeningQuestion("ALLERGY", "Do you have allergies?", QuestionType.SINGLE_CHOICE, true, true);
        question.setId(10L);
        option = new AnswerOption(question, "YES", "Yes", "true", 1);
        option.setId(20L);
    }

    @Test
    void createsAnswerOptionForChoiceQuestion() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerOptionRepository.existsByQuestionIdAndCode(10L, "NO")).thenReturn(false);
        when(answerOptionRepository.save(any(AnswerOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnswerOption created = service.addAnswerOption(10L, " NO ", " No ", " false ", 2);

        assertThat(created.getQuestion()).isSameAs(question);
        assertThat(created.getCode()).isEqualTo("NO");
        assertThat(created.getLabel()).isEqualTo("No");
        assertThat(created.getTypedValue()).isEqualTo("false");
        assertThat(created.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void readsAnswerOptionById() {
        when(answerOptionRepository.findById(20L)).thenReturn(Optional.of(option));

        assertThat(service.getAnswerOptionById(20L)).isSameAs(option);
    }

    @Test
    void listsAnswerOptionsForExistingQuestion() {
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(option));

        assertThat(service.getOptionsForQuestion(10L)).containsExactly(option);
    }

    @Test
    void updatesBusinessFieldsWithoutChangingOwningQuestion() {
        when(answerOptionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(answerOptionRepository.existsByQuestionIdAndCode(10L, "AFFIRMATIVE")).thenReturn(false);
        when(answerOptionRepository.save(option)).thenReturn(option);

        AnswerOption updated = service.updateAnswerOption(
                20L,
                " AFFIRMATIVE ",
                " Affirmative ",
                " yes ",
                3
        );

        assertThat(updated.getQuestion()).isSameAs(question);
        assertThat(updated.getCode()).isEqualTo("AFFIRMATIVE");
        assertThat(updated.getLabel()).isEqualTo("Affirmative");
        assertThat(updated.getTypedValue()).isEqualTo("yes");
        assertThat(updated.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void rejectsDuplicateAnswerOptionCodeWithinQuestion() {
        when(answerOptionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(answerOptionRepository.existsByQuestionIdAndCode(10L, "NO")).thenReturn(true);

        assertThatThrownBy(() -> service.updateAnswerOption(20L, "NO", "No", "false", 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists for this question");

        verify(answerOptionRepository, never()).save(any());
    }

    @Test
    void deletesUnreferencedAnswerOption() {
        when(answerOptionRepository.findById(20L)).thenReturn(Optional.of(option));
        when(questionRepository.isReferencedInScreeningAnswers(10L)).thenReturn(false);

        service.removeAnswerOption(20L);

        verify(answerOptionRepository).delete(option);
    }

    @Test
    void rejectsNonexistentAnswerOptionId() {
        when(answerOptionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAnswerOptionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Answer option not found with ID: 999");
    }
}
