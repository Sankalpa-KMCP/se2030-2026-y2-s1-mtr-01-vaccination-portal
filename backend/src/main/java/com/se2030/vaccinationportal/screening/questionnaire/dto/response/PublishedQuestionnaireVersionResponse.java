package com.se2030.vaccinationportal.screening.questionnaire.dto.response;

import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;

import java.time.Instant;

/**
 * Response DTO representing published QuestionnaireVersion summaries for Campaign assignment.
 */
public record PublishedQuestionnaireVersionResponse(
        Long id,
        Long questionnaireId,
        String questionnaireCode,
        String questionnaireName,
        Integer versionNumber,
        QuestionnaireVersionStatus status,
        Instant publishedAt
) {}
