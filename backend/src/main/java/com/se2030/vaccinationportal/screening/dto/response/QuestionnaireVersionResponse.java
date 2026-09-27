package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;

import java.time.Instant;
import java.util.List;

public record QuestionnaireVersionResponse(
        Long id,
        Long questionnaireId,
        String questionnaireCode,
        String questionnaireName,
        Integer versionNumber,
        QuestionnaireVersionStatus status,
        Instant publishedAt,
        Instant retiredAt,
        List<QuestionnaireItemResponse> items,
        List<EligibilityRuleResponse> rules,
        Instant createdAt,
        Instant updatedAt
) {
    public QuestionnaireVersionResponse(
            Long id,
            Long questionnaireId,
            Integer versionNumber,
            QuestionnaireVersionStatus status,
            Instant publishedAt,
            Instant retiredAt,
            List<QuestionnaireItemResponse> items,
            List<EligibilityRuleResponse> rules,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, questionnaireId, null, null, versionNumber, status, publishedAt, retiredAt, items, rules, createdAt, updatedAt);
    }
}
