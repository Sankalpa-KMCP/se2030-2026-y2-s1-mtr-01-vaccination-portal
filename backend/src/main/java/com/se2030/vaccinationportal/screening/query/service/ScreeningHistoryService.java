package com.se2030.vaccinationportal.screening.query.service;

import com.se2030.vaccinationportal.screening.submission.service.CitizenScreeningAccessService;


import com.se2030.vaccinationportal.screening.query.dto.response.ScreeningHistoryDetailsResponse;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.query.mapper.ScreeningHistoryDetailsMapper;
import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScreeningHistoryService {
    private final ScreeningSubmissionRepository submissionRepository;
    private final QuestionnaireItemRepository itemRepository;
    private final AnswerOptionRepository optionRepository;
    private final CitizenScreeningAccessService accessService;
    private final ScreeningHistoryDetailsMapper mapper;

    public ScreeningHistoryService(
            ScreeningSubmissionRepository submissionRepository,
            QuestionnaireItemRepository itemRepository,
            AnswerOptionRepository optionRepository,
            CitizenScreeningAccessService accessService,
            ScreeningHistoryDetailsMapper mapper
    ) {
        this.submissionRepository = submissionRepository;
        this.itemRepository = itemRepository;
        this.optionRepository = optionRepository;
        this.accessService = accessService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ScreeningHistoryDetailsResponse getHistoryDetails(Long submissionId) {
        if (submissionId == null) throw new IllegalArgumentException("Submission ID cannot be null");
        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screening submission not found with ID: " + submissionId));
        accessService.requireCitizenIdAccess(submission.getCitizenId(),
                "ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR");

        // Hydrate within this read-only transaction, independent of current campaign assignments/windows.
        List<QuestionnaireItem> items = itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(
                submission.getQuestionnaireVersion().getId());
        Set<Long> answeredQuestions = submission.getAnswers().stream()
                .map(answer -> answer.getQuestion().getId()).collect(Collectors.toSet());
        Map<Long, List<AnswerOption>> options = new HashMap<>();
        for (QuestionnaireItem item : items) {
            Long questionId = item.getQuestion().getId();
            QuestionType type = item.getQuestion().getQuestionType();
            if (answeredQuestions.contains(questionId)
                    && (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTI_CHOICE)) {
                options.put(questionId, optionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId));
            }
        }
        return mapper.toResponse(submission, items, options);
    }
}
