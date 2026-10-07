package com.se2030.vaccinationportal.screening.questionnaire.mapper;

import com.se2030.vaccinationportal.screening.questionnaire.dto.response.QuestionnaireResponse;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
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
