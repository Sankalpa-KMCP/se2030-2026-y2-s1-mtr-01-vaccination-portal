package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewAnswerOptionResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewAnswerResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewDetailsResponse;
import com.se2030.vaccinationportal.screening.dto.response.TriggeredRuleEvidenceResponse;
import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningTriggeredRuleEvidence;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScreeningReviewDetailsMapper {

    private final EligibilityAssessmentMapper assessmentMapper;

    public ScreeningReviewDetailsMapper(EligibilityAssessmentMapper assessmentMapper) {
        this.assessmentMapper = assessmentMapper;
    }

    public ScreeningReviewDetailsResponse toResponse(
            ScreeningSubmission submission,
            String citizenFullName,
            List<QuestionnaireItem> questionnaireItems,
            Map<Long, List<AnswerOption>> optionsByQuestionId,
            List<ScreeningTriggeredRuleEvidence> evidence,
            EligibilityAssessment assessment
    ) {
        Map<Long, ScreeningAnswer> answersByQuestionId = submission.getAnswers().stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        Function.identity()
                ));

        List<ScreeningReviewAnswerResponse> answers = questionnaireItems.stream()
                .sorted(Comparator.comparingInt(QuestionnaireItem::getDisplayOrder))
                .filter(item -> answersByQuestionId.containsKey(item.getQuestion().getId()))
                .map(item -> mapAnswer(
                        item,
                        answersByQuestionId.get(item.getQuestion().getId()),
                        optionsByQuestionId.getOrDefault(item.getQuestion().getId(), List.of())
                ))
                .toList();

        List<TriggeredRuleEvidenceResponse> triggeredRuleEvidence = evidence.stream()
                .sorted(Comparator.comparingInt(ScreeningTriggeredRuleEvidence::getEvaluationOrder))
                .map(this::mapEvidence)
                .toList();

        return new ScreeningReviewDetailsResponse(
                submission.getId(),
                submission.getCitizenId(),
                citizenFullName,
                submission.getCampaignId(),
                submission.getQuestionnaireVersion().getId(),
                submission.getConsentNoticeVersion().getId(),
                submission.getStatus(),
                submission.getPreliminaryResult(),
                submission.getConsentAt(),
                submission.getSubmittedAt(),
                submission.getCorrectionInstructions(),
                submission.getCorrectionRequestedAt(),
                answers,
                triggeredRuleEvidence,
                assessment == null ? null : assessmentMapper.toResponse(assessment)
        );
    }

    private ScreeningReviewAnswerResponse mapAnswer(
            QuestionnaireItem item,
            ScreeningAnswer answer,
            List<AnswerOption> options
    ) {
        boolean required = item.getRequiredOverride() != null
                ? item.getRequiredOverride()
                : item.getQuestion().isDefaultRequired();

        return new ScreeningReviewAnswerResponse(
                answer.getId(),
                item.getId(),
                item.getQuestion().getId(),
                item.getQuestion().getCode(),
                item.getQuestion().getText(),
                item.getQuestion().getQuestionType(),
                item.getDisplayOrder(),
                required,
                answer.getTypedAnswer(),
                answer.getCreatedAt(),
                resolveSelectedOptions(item.getQuestion().getQuestionType(), answer.getTypedAnswer(), options)
        );
    }

    private List<ScreeningReviewAnswerOptionResponse> resolveSelectedOptions(
            QuestionType questionType,
            String typedAnswer,
            List<AnswerOption> options
    ) {
        if (questionType == QuestionType.SINGLE_CHOICE) {
            return resolveUniqueOption(typedAnswer, options).stream().map(this::mapOption).toList();
        }
        if (questionType == QuestionType.MULTI_CHOICE) {
            return Arrays.stream(typedAnswer.split(",", -1))
                    .map(String::trim)
                    .map(value -> resolveUniqueOption(value, options))
                    .flatMap(java.util.Optional::stream)
                    .map(this::mapOption)
                    .toList();
        }
        return List.of();
    }

    private java.util.Optional<AnswerOption> resolveUniqueOption(String value, List<AnswerOption> options) {
        List<AnswerOption> matches = options.stream()
                .filter(option -> Objects.equals(value, option.getTypedValue())
                        || Objects.equals(value, option.getCode()))
                .limit(2)
                .toList();
        return matches.size() == 1 ? java.util.Optional.of(matches.get(0)) : java.util.Optional.empty();
    }

    private ScreeningReviewAnswerOptionResponse mapOption(AnswerOption option) {
        return new ScreeningReviewAnswerOptionResponse(
                option.getId(),
                option.getCode(),
                option.getLabel(),
                option.getTypedValue()
        );
    }

    private TriggeredRuleEvidenceResponse mapEvidence(ScreeningTriggeredRuleEvidence evidence) {
        return new TriggeredRuleEvidenceResponse(
                evidence.getId(),
                evidence.getSourceRule().getId(),
                evidence.getRuleCode(),
                evidence.getMessage(),
                evidence.getPreliminaryResult(),
                evidence.getPriority(),
                evidence.getEvaluationOrder(),
                evidence.getCreatedAt()
        );
    }
}
