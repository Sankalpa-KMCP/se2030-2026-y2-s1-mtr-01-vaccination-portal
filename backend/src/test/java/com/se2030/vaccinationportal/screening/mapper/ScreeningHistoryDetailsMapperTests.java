package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.ScreeningHistoryAnswerResponse;
import com.se2030.vaccinationportal.screening.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScreeningHistoryDetailsMapperTests {
    private final ScreeningHistoryDetailsMapper mapper = new ScreeningHistoryDetailsMapper();

    @ParameterizedTest
    @CsvSource({
            "BOOLEAN,true,Yes", "BOOLEAN,false,No", "BOOLEAN,legacy,legacy",
            "NUMERIC,37.50,37.50", "DATE,2026-09-04,2026-09-04", "TEXT,Unchanged text,Unchanged text"
    })
    void displaysNonChoiceTypesWithoutChangingRawAnswer(QuestionType type, String raw, String display) {
        ScreeningHistoryAnswerResponse answer = map(type, raw, List.of());
        assertThat(answer.typedAnswer()).isEqualTo(raw);
        assertThat(answer.displayAnswer()).isEqualTo(display);
        assertThat(answer.selectedOptions()).isEmpty();
    }

    @Test
    void preservesTextWhitespaceAndMarkupAsData() {
        String raw = "  First line\n<not markup> & second line  ";
        assertThat(map(QuestionType.TEXT, raw, List.of()).displayAnswer()).isEqualTo(raw);
    }

    @Test
    void mapsSingleChoiceByTypedValueWithSelectedMetadata() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.SINGLE_CHOICE, "stored-value",
                List.of(option(1L, 10L, "CODE", "Readable label", "stored-value")));
        assertThat(answer.typedAnswer()).isEqualTo("stored-value");
        assertThat(answer.displayAnswer()).isEqualTo("Readable label");
        assertThat(answer.selectedOptions()).singleElement().satisfies(selected -> {
            assertThat(selected.id()).isEqualTo(1L);
            assertThat(selected.code()).isEqualTo("CODE");
            assertThat(selected.label()).isEqualTo("Readable label");
            assertThat(selected.value()).isEqualTo("stored-value");
        });
    }

    @Test
    void cannotMatchAnOptionBelongingToAnotherQuestion() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.SINGLE_CHOICE, "same-value",
                List.of(option(1L, 99L, "OTHER", "Unrelated label", "same-value")));
        assertThat(answer.displayAnswer()).isEqualTo("same-value");
        assertThat(answer.selectedOptions()).isEmpty();
    }

    @Test
    void ignoresOtherQuestionsWhenResolvingOwnMatchingOption() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.SINGLE_CHOICE, "same-value", List.of(
                option(1L, 99L, "OTHER", "Unrelated label", "same-value"),
                option(2L, 10L, "OWN", "Correct label", "same-value")));
        assertThat(answer.displayAnswer()).isEqualTo("Correct label");
        assertThat(answer.selectedOptions()).extracting(selected -> selected.id()).containsExactly(2L);
    }

    @Test
    void doesNotMistakeAnOptionCodeForStoredTypedValue() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.SINGLE_CHOICE, "LEGACY_CODE",
                List.of(option(1L, 10L, "LEGACY_CODE", "Do not guess", "different-value")));
        assertThat(answer.displayAnswer()).isEqualTo("LEGACY_CODE");
        assertThat(answer.selectedOptions()).isEmpty();
    }

    @Test
    void ambiguousTypedValuesKeepRawAnswerRatherThanGuessing() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.SINGLE_CHOICE, "duplicate", List.of(
                option(1L, 10L, "ONE", "First", "duplicate"),
                option(2L, 10L, "TWO", "Second", "duplicate")));
        assertThat(answer.displayAnswer()).isEqualTo("duplicate");
        assertThat(answer.selectedOptions()).isEmpty();
    }

    @Test
    void multiChoiceUsesStoredSelectionOrderNotOptionDisplayOrder() {
        List<AnswerOption> options = List.of(option(1L, 10L, "A", "Alpha", "a"),
                option(2L, 10L, "B", "Beta", "b"));
        ScreeningHistoryAnswerResponse answer = map(QuestionType.MULTI_CHOICE, "b,a", options);
        assertThat(answer.typedAnswer()).isEqualTo("b,a");
        assertThat(answer.displayAnswer()).isEqualTo("Beta, Alpha");
        assertThat(answer.selectedOptions()).extracting(selected -> selected.code()).containsExactly("B", "A");
        assertThat(map(QuestionType.MULTI_CHOICE, "b,a", options)).isEqualTo(answer);
    }

    @Test
    void unknownMultiChoiceValueKeepsWholeRawDisplayAndKnownMetadata() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.MULTI_CHOICE, "a,legacy,b", List.of(
                option(1L, 10L, "A", "Alpha", "a"), option(2L, 10L, "B", "Beta", "b")));
        assertThat(answer.typedAnswer()).isEqualTo("a,legacy,b");
        assertThat(answer.displayAnswer()).isEqualTo("a,legacy,b");
        assertThat(answer.selectedOptions()).extracting(selected -> selected.label()).containsExactly("Alpha", "Beta");
    }

    @Test
    void missingOptionMetadataKeepsSingleChoiceReadable() {
        ScreeningHistoryAnswerResponse answer = map(QuestionType.SINGLE_CHOICE, "legacy", List.of());
        assertThat(answer.displayAnswer()).isEqualTo("legacy");
        assertThat(answer.selectedOptions()).isEmpty();
    }

    @Test
    void nullLegacyChoiceDoesNotCrashMapping() {
        assertThat(map(QuestionType.MULTI_CHOICE, null, List.of()).displayAnswer()).isNull();
    }

    @Test
    void mapsStoredVersionOrderedMetadataCorrectionsAndLegacyAnswers() {
        ScreeningSubmission submission = submission();
        ScreeningQuestion first = question(10L, QuestionType.BOOLEAN);
        ScreeningQuestion second = question(11L, QuestionType.TEXT);
        ScreeningQuestion unanswered = question(12L, QuestionType.TEXT);
        ScreeningQuestion legacy = question(13L, QuestionType.TEXT);
        submission.addAnswer(answer(2L, submission, second, "second"));
        submission.addAnswer(answer(3L, submission, legacy, "retained legacy answer"));
        submission.addAnswer(answer(1L, submission, first, "false"));
        var result = mapper.toResponse(submission, List.of(
                item(submission, second, 20), item(submission, unanswered, 30), item(submission, first, 10)), Map.of());
        assertThat(result.questionnaireVersionId()).isEqualTo(70L);
        assertThat(result.questionnaireVersionNumber()).isEqualTo(3);
        assertThat(result.answers()).extracting(a -> a.questionId()).containsExactly(10L, 11L, 13L);
        assertThat(result.answers()).extracting(a -> a.displayOrder()).containsExactly(10, 20, null);
        assertThat(result.answers().get(0)).satisfies(a -> {
            assertThat(a.id()).isEqualTo(1L);
            assertThat(a.questionCode()).isEqualTo("QUESTION-10");
            assertThat(a.questionText()).isEqualTo("Historical question 10?");
            assertThat(a.questionType()).isEqualTo(QuestionType.BOOLEAN);
            assertThat(a.typedAnswer()).isEqualTo("false");
        });
        assertThat(result.answers().get(2).displayAnswer()).isEqualTo("retained legacy answer");
        assertThat(result.correctionInstructions()).isEqualTo("Clarify your answer");
        assertThat(result.correctionRequestedAt()).isEqualTo(Instant.parse("2026-09-03T10:00:00Z"));
    }

    private ScreeningHistoryAnswerResponse map(QuestionType type, String raw, List<AnswerOption> options) {
        ScreeningSubmission submission = submission();
        ScreeningQuestion question = question(10L, type);
        submission.addAnswer(answer(1L, submission, question, raw));
        return mapper.toResponse(submission, List.of(item(submission, question, 1)), Map.of(10L, options)).answers().get(0);
    }

    private ScreeningSubmission submission() {
        QuestionnaireVersion version = new QuestionnaireVersion(null, 3, QuestionnaireVersionStatus.RETIRED);
        version.setId(70L);
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setQuestionnaireVersion(version);
        submission.setCorrectionInstructions("Clarify your answer");
        submission.setCorrectionRequestedAt(Instant.parse("2026-09-03T10:00:00Z"));
        return submission;
    }

    private ScreeningQuestion question(Long id, QuestionType type) {
        ScreeningQuestion question = new ScreeningQuestion("QUESTION-" + id, "Historical question " + id + "?", type, true, true);
        question.setId(id);
        return question;
    }

    private ScreeningAnswer answer(Long id, ScreeningSubmission submission, ScreeningQuestion question, String raw) {
        ScreeningAnswer answer = new ScreeningAnswer(submission, question, raw);
        answer.setId(id);
        return answer;
    }

    private QuestionnaireItem item(ScreeningSubmission submission, ScreeningQuestion question, int order) {
        return new QuestionnaireItem(submission.getQuestionnaireVersion(), question, order, null);
    }

    private AnswerOption option(Long id, Long questionId, String code, String label, String value) {
        AnswerOption option = new AnswerOption(question(questionId, QuestionType.SINGLE_CHOICE), code, label, value, 1);
        option.setId(id);
        return option;
    }
}
