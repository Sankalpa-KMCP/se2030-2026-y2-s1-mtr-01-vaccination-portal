package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.AnswerOptionResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningQuestionResponse;
import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScreeningQuestionMapper {

    public ScreeningQuestionResponse toResponse(ScreeningQuestion question) {
        return new ScreeningQuestionResponse(
                question.getId(),
                question.getCode(),
                question.getText(),
                question.getDescription(),
                question.getQuestionType(),
                question.isDefaultRequired(),
                question.isActive(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    public List<ScreeningQuestionResponse> toQuestionResponses(List<ScreeningQuestion> questions) {
        return questions.stream().map(this::toResponse).toList();
    }

    public AnswerOptionResponse toResponse(AnswerOption option) {
        return new AnswerOptionResponse(
                option.getId(),
                option.getQuestion().getId(),
                option.getCode(),
                option.getLabel(),
                option.getTypedValue(),
                option.getDisplayOrder(),
                option.getCreatedAt(),
                option.getUpdatedAt()
        );
    }

    public List<AnswerOptionResponse> toOptionResponses(List<AnswerOption> options) {
        return options.stream().map(this::toResponse).toList();
    }
}
