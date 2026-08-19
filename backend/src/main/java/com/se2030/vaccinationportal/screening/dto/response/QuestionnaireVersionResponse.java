package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;

import java.time.Instant;
import java.util.List;

public record QuestionnaireVersionResponse(
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
) {}
