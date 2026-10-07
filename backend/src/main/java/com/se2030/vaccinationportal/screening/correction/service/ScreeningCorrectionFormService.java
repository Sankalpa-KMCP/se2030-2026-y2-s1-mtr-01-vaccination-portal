package com.se2030.vaccinationportal.screening.correction.service;

import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;


import com.se2030.vaccinationportal.screening.correction.dto.response.ScreeningCorrectionFormAnswerOptionResponse;
import com.se2030.vaccinationportal.screening.correction.dto.response.ScreeningCorrectionFormCurrentAnswerResponse;
import com.se2030.vaccinationportal.screening.correction.dto.response.ScreeningCorrectionFormQuestionResponse;
import com.se2030.vaccinationportal.screening.correction.dto.response.ScreeningCorrectionFormResponse;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds an editable correction form from the questionnaire version stored on a submission. */
@Service
public class ScreeningCorrectionFormService {

    private final ScreeningSubmissionRepository submissionRepository;
    private final QuestionnaireItemRepository itemRepository;
    private final AnswerOptionRepository optionRepository;
    private final CitizenScreeningAccessService accessService;

    public ScreeningCorrectionFormService(
            ScreeningSubmissionRepository submissionRepository,
            QuestionnaireItemRepository itemRepository,
            AnswerOptionRepository optionRepository,
            CitizenScreeningAccessService accessService
    ) {
        this.submissionRepository = submissionRepository;
        this.itemRepository = itemRepository;
        this.optionRepository = optionRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ScreeningCorrectionFormResponse getCorrectionForm(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }

        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screening submission not found with ID: " + submissionId
                ));
        accessService.requireCitizenIdAccess(
                submission.getCitizenId(),
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        if (submission.getStatus() != ScreeningSubmissionStatus.CORRECTIONS_REQUIRED) {
            throw new IllegalStateException(
                    "Correction form is only available for submissions in CORRECTIONS_REQUIRED status"
            );
        }

        Long versionId = submission.getQuestionnaireVersion().getId();
        List<QuestionnaireItem> items = itemRepository
                .findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId);
        Map<Long, ScreeningAnswer> answersByQuestionId = answersByQuestionId(submission);
        List<ScreeningCorrectionFormQuestionResponse> questions = items.stream()
                .map(item -> toQuestionResponse(item, answersByQuestionId.get(item.getQuestion().getId())))
                .toList();

        return new ScreeningCorrectionFormResponse(
                submission.getId(),
                submission.getCampaignId(),
                versionId,
                submission.getQuestionnaireVersion().getVersionNumber(),
                submission.getStatus(),
                submission.getCorrectionInstructions(),
                submission.getCorrectionRequestedAt(),
                questions
        );
    }

    private Map<Long, ScreeningAnswer> answersByQuestionId(ScreeningSubmission submission) {
        Map<Long, ScreeningAnswer> answers = new LinkedHashMap<>();
        for (ScreeningAnswer answer : submission.getAnswers()) {
            answers.putIfAbsent(answer.getQuestion().getId(), answer);
        }
        return answers;
    }

    private ScreeningCorrectionFormQuestionResponse toQuestionResponse(
            QuestionnaireItem item,
            ScreeningAnswer answer
    ) {
        ScreeningQuestion question = item.getQuestion();
        List<ScreeningCorrectionFormAnswerOptionResponse> options = optionRepository
                .findByQuestionIdOrderByDisplayOrderAsc(question.getId())
                .stream()
                .filter(option -> option.getQuestion() != null
                        && Objects.equals(question.getId(), option.getQuestion().getId()))
                .sorted(Comparator.comparingInt(AnswerOption::getDisplayOrder)
                        .thenComparing(AnswerOption::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toOptionResponse)
                .toList();
        boolean required = item.getRequiredOverride() != null
                ? item.getRequiredOverride()
                : question.isDefaultRequired();

        return new ScreeningCorrectionFormQuestionResponse(
                question.getId(),
                question.getCode(),
                question.getText(),
                question.getDescription(),
                question.getQuestionType(),
                required,
                item.getDisplayOrder(),
                options,
                answer == null ? null : toCurrentAnswer(question, answer.getTypedAnswer(), options)
        );
    }

    private ScreeningCorrectionFormCurrentAnswerResponse toCurrentAnswer(
            ScreeningQuestion question,
            String typedAnswer,
            List<ScreeningCorrectionFormAnswerOptionResponse> options
    ) {
        List<ScreeningCorrectionFormAnswerOptionResponse> selectedOptions = new ArrayList<>();
        if (typedAnswer != null && (question.getQuestionType() == QuestionType.SINGLE_CHOICE
                || question.getQuestionType() == QuestionType.MULTI_CHOICE)) {
            List<String> selectedValues = question.getQuestionType() == QuestionType.SINGLE_CHOICE
                    ? List.of(typedAnswer)
                    : Arrays.stream(typedAnswer.split(",", -1)).map(String::trim).toList();
            for (String selectedValue : selectedValues) {
                List<ScreeningCorrectionFormAnswerOptionResponse> matches = options.stream()
                        .filter(option -> Objects.equals(selectedValue, option.typedValue()))
                        .limit(2)
                        .toList();
                if (matches.size() == 1) {
                    selectedOptions.add(matches.get(0));
                }
            }
        }
        return new ScreeningCorrectionFormCurrentAnswerResponse(
                typedAnswer,
                List.copyOf(selectedOptions)
        );
    }

    private ScreeningCorrectionFormAnswerOptionResponse toOptionResponse(AnswerOption option) {
        return new ScreeningCorrectionFormAnswerOptionResponse(
                option.getId(),
                option.getCode(),
                option.getLabel(),
                option.getTypedValue(),
                option.getDisplayOrder()
        );
    }
}
