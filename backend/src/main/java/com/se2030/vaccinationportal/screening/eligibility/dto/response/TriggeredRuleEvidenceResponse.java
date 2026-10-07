package com.se2030.vaccinationportal.screening.eligibility.dto.response;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;

import java.time.Instant;

public record TriggeredRuleEvidenceResponse(
        Long id,
        Long sourceRuleId,
        String ruleCode,
        String message,
        PreliminaryScreeningResult preliminaryResult,
        int priority,
        int evaluationOrder,
        Instant createdAt
) {}
