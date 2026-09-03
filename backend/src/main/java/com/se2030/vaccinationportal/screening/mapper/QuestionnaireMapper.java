package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.QuestionnaireResponse;
import com.se2030.vaccinationportal.screening.model.Questionnaire;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionnaireMapper {

    public QuestionnaireResponse toResponse(Questionnaire questionnaire) {
        return new QuestionnaireResponse(
                questionnaire.getId(),
                questionnaire.getCode(),
                questionnaire.getName(),
                questionnaire.getDescription(),
                questionnaire.getCreatedAt(),
                questionnaire.getUpdatedAt()
        );
    }

    public List<QuestionnaireResponse> toResponses(List<Questionnaire> questionnaires) {
        return questionnaires.stream().map(this::toResponse).toList();
    }
}
