package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.ScreeningHistoryAnswerOptionResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningHistoryAnswerResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningHistoryDetailsResponse;
import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScreeningHistoryDetailsMapper {

    public ScreeningHistoryDetailsResponse toResponse(
            ScreeningSubmission submission,
            List<QuestionnaireItem> questionnaireItems,
            Map<Long, List<AnswerOption>> optionsByQuestionId
    ) {
        Map<Long, QuestionnaireItem> itemsByQuestionId = questionnaireItems.stream()
                .collect(Collectors.toMap(item -> item.getQuestion().getId(), Function.identity()));
        List<ScreeningHistoryAnswerResponse> answers = submission.getAnswers().stream()
                .map(answer -> mapAnswer(answer, itemsByQuestionId.get(answer.getQuestion().getId()),
                        optionsByQuestionId.getOrDefault(answer.getQuestion().getId(), List.of())))
                .sorted(Comparator.comparing(ScreeningHistoryAnswerResponse::displayOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ScreeningHistoryAnswerResponse::questionId))
                .toList();

        return new ScreeningHistoryDetailsResponse(
                submission.getId(), submission.getCitizenId(), submission.getCampaignId(),
                submission.getQuestionnaireVersion().getId(),
                submission.getQuestionnaireVersion().getVersionNumber(),
                submission.getStatus(), submission.getPreliminaryResult(), submission.getSubmittedAt(),
                submission.getCreatedAt(), submission.getUpdatedAt(),
                submission.getCorrectionInstructions(), submission.getCorrectionRequestedAt(), answers
        );
    }

    private ScreeningHistoryAnswerResponse mapAnswer(
            ScreeningAnswer answer, QuestionnaireItem item, List<AnswerOption> options
    ) {
        // Keep anomalous legacy answers visible without inventing a questionnaire position.
        ScreeningQuestion question = item == null ? answer.getQuestion() : item.getQuestion();
        String raw = answer.getTypedAnswer();
        String display = raw;
        List<ScreeningHistoryAnswerOptionResponse> selectedOptions = new ArrayList<>();
        if (question.getQuestionType() == QuestionType.BOOLEAN) {
            if ("true".equals(raw)) display = "Yes";
            else if ("false".equals(raw)) display = "No";
        } else if (raw != null && item != null && (question.getQuestionType() == QuestionType.SINGLE_CHOICE
                || question.getQuestionType() == QuestionType.MULTI_CHOICE)) {
            List<String> values = question.getQuestionType() == QuestionType.SINGLE_CHOICE
                    ? List.of(raw) : Arrays.stream(raw.split(",", -1)).map(String::trim).toList();
            for (String value : values) {
                // Persisted answers contain typedValue, not code. Ambiguous values must not guess a label.
                List<AnswerOption> matches = options.stream()
                        .filter(option -> option.getQuestion() != null
                                && Objects.equals(question.getId(), option.getQuestion().getId())
                                && Objects.equals(value, option.getTypedValue()))
                        .limit(2).toList();
                if (matches.size() == 1) {
                    AnswerOption option = matches.get(0);
                    selectedOptions.add(new ScreeningHistoryAnswerOptionResponse(
                            option.getId(), option.getCode(), option.getLabel(), option.getTypedValue()));
                }
            }
            // Do not silently drop unknown selections from a partially resolved multi-choice answer.
            if (selectedOptions.size() == values.size()) {
                display = selectedOptions.stream().map(ScreeningHistoryAnswerOptionResponse::label)
                        .collect(Collectors.joining(", "));
            }
        }
        return new ScreeningHistoryAnswerResponse(
                answer.getId(), question.getId(), question.getCode(), question.getText(),
                question.getQuestionType(), item == null ? null : item.getDisplayOrder(),
                raw, display, List.copyOf(selectedOptions)
        );
    }
}
