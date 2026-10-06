package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningAnswerValidatorTests {

    @Mock
    private AnswerOptionRepository optionRepository;

    private ScreeningAnswerValidator validator;
    private QuestionnaireVersion version;

    @BeforeEach
    void setUp() {
        validator = new ScreeningAnswerValidator(optionRepository);
        version = new QuestionnaireVersion();
        version.setId(50L);
    }

    @Test
    void acceptsAndTrimsTextAnswer() {
        ScreeningQuestion question = question(1L, QuestionType.TEXT, true);

        List<ScreeningAnswerValidator.ValidatedAnswer> result = validate(item(question, null), Map.of(1L, "  details  "));

        assertThat(result).extracting(ScreeningAnswerValidator.ValidatedAnswer::typedAnswer)
                .containsExactly("details");
    }

    @Test
    void rejectsBlankTextAnswer() {
        ScreeningQuestion question = question(1L, QuestionType.TEXT, false);

        assertThatThrownBy(() -> validate(item(question, null), Map.of(1L, "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Blank answer submitted for question 1");
    }

    @Test
    void acceptsAndNormalizesBooleanAnswer() {
        ScreeningQuestion question = question(2L, QuestionType.BOOLEAN, true);

        assertThat(validate(item(question, null), Map.of(2L, " TRUE ")).getFirst().typedAnswer())
                .isEqualTo("true");
    }

    @Test
    void rejectsInvalidBooleanAnswer() {
        ScreeningQuestion question = question(2L, QuestionType.BOOLEAN, true);

        assertThatThrownBy(() -> validate(item(question, null), Map.of(2L, "maybe")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid boolean answer for question 2");
    }

    @Test
    void acceptsNumericAnswer() {
        ScreeningQuestion question = question(3L, QuestionType.NUMERIC, true);

        assertThat(validate(item(question, null), Map.of(3L, "37.5")).getFirst().typedAnswer())
                .isEqualTo("37.5");
    }

    @Test
    void rejectsInvalidNumericAnswer() {
        ScreeningQuestion question = question(3L, QuestionType.NUMERIC, true);

        assertThatThrownBy(() -> validate(item(question, null), Map.of(3L, "thirty-seven")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid numeric answer for question 3");
    }

    @Test
    void acceptsIsoDateAnswer() {
        ScreeningQuestion question = question(4L, QuestionType.DATE, true);

        assertThat(validate(item(question, null), Map.of(4L, "2026-08-19")).getFirst().typedAnswer())
                .isEqualTo("2026-08-19");
    }

    @Test
    void rejectsInvalidDateAnswer() {
        ScreeningQuestion question = question(4L, QuestionType.DATE, true);

        assertThatThrownBy(() -> validate(item(question, null), Map.of(4L, "2026-02-30")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid date answer for question 4");
    }

    @Test
    void acceptsSingleChoiceOptionBelongingToQuestion() {
        ScreeningQuestion question = question(5L, QuestionType.SINGLE_CHOICE, true);
        AnswerOption yes = option(51L, question, "YES", "true");
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(5L)).thenReturn(List.of(yes));

        assertThat(validate(item(question, null), Map.of(5L, "YES")).getFirst().typedAnswer())
                .isEqualTo("true");
    }

    @Test
    void rejectsSingleChoiceOptionBelongingToAnotherQuestion() {
        ScreeningQuestion question = question(5L, QuestionType.SINGLE_CHOICE, true);
        ScreeningQuestion otherQuestion = question(99L, QuestionType.SINGLE_CHOICE, true);
        AnswerOption foreignOption = option(91L, otherQuestion, "FOREIGN", "foreign");
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(5L)).thenReturn(List.of(foreignOption));

        assertThatThrownBy(() -> validate(item(question, null), Map.of(5L, "FOREIGN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected option is not valid for question 5");
    }

    @Test
    void rejectsUnknownSingleChoiceOption() {
        ScreeningQuestion question = question(5L, QuestionType.SINGLE_CHOICE, true);
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(5L)).thenReturn(List.of());

        assertThatThrownBy(() -> validate(item(question, null), Map.of(5L, "UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected option is not valid for question 5");
    }

    @Test
    void acceptsAndNormalizesMultipleChoiceOptions() {
        ScreeningQuestion question = question(6L, QuestionType.MULTI_CHOICE, true);
        AnswerOption first = option(61L, question, "A", "alpha");
        AnswerOption second = option(62L, question, "B", "beta");
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(6L)).thenReturn(List.of(first, second));

        assertThat(validate(item(question, null), Map.of(6L, " A, B ")).getFirst().typedAnswer())
                .isEqualTo("alpha,beta");
    }

    @Test
    void rejectsUnknownMultipleChoiceOption() {
        ScreeningQuestion question = question(6L, QuestionType.MULTI_CHOICE, true);
        AnswerOption first = option(61L, question, "A", "alpha");
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(6L)).thenReturn(List.of(first));

        assertThatThrownBy(() -> validate(item(question, null), Map.of(6L, "A,UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected option is not valid for question 6");
    }

    @Test
    void rejectsDuplicateMultipleChoiceOptionEvenWhenCodeAndTypedValueDiffer() {
        ScreeningQuestion question = question(6L, QuestionType.MULTI_CHOICE, true);
        AnswerOption first = option(61L, question, "A", "alpha");
        when(optionRepository.findByQuestionIdOrderByDisplayOrderAsc(6L)).thenReturn(List.of(first));

        assertThatThrownBy(() -> validate(item(question, null), Map.of(6L, "A,alpha")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate option selected for question 6");
    }

    @Test
    void rejectsQuestionOutsideQuestionnaireVersion() {
        ScreeningQuestion included = question(1L, QuestionType.TEXT, true);

        assertThatThrownBy(() -> validate(item(included, null), Map.of(99L, "extra")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Question 99 is not part of questionnaire version 50");
    }

    @Test
    void rejectsMissingRequiredQuestion() {
        ScreeningQuestion required = question(1L, QuestionType.TEXT, true);

        assertThatThrownBy(() -> validate(item(required, null), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Answer required for question 1");
    }

    @Test
    void requiredOverrideIsHonoredAndOptionalQuestionMayBeOmitted() {
        ScreeningQuestion required = question(1L, QuestionType.TEXT, false);
        ScreeningQuestion optional = question(2L, QuestionType.TEXT, true);
        List<QuestionnaireItem> items = List.of(item(required, true), item(optional, false));

        List<ScreeningAnswerValidator.ValidatedAnswer> result =
                validator.validateAndNormalize(50L, items, Map.of(1L, "provided"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().question()).isSameAs(required);
    }

    @Test
    void rejectsBlankRequiredAnswer() {
        ScreeningQuestion required = question(1L, QuestionType.TEXT, true);
        Map<Long, String> answers = new LinkedHashMap<>();
        answers.put(1L, null);

        assertThatThrownBy(() -> validate(item(required, null), answers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Answer required for question 1");
    }

    private List<ScreeningAnswerValidator.ValidatedAnswer> validate(
            QuestionnaireItem item,
            Map<Long, String> answers
    ) {
        return validator.validateAndNormalize(50L, List.of(item), answers);
    }

    private ScreeningQuestion question(Long id, QuestionType type, boolean defaultRequired) {
        ScreeningQuestion question = new ScreeningQuestion("Q" + id, "Question " + id, type, defaultRequired, true);
        question.setId(id);
        return question;
    }

    private QuestionnaireItem item(ScreeningQuestion question, Boolean requiredOverride) {
        return new QuestionnaireItem(version, question, question.getId().intValue(), requiredOverride);
    }

    private AnswerOption option(Long id, ScreeningQuestion question, String code, String typedValue) {
        AnswerOption option = new AnswerOption(question, code, code, typedValue, id.intValue());
        option.setId(id);
        return option;
    }
}
