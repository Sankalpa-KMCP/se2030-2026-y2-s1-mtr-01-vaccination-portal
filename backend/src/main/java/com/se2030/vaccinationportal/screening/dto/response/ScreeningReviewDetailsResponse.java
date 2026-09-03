package com.se2030.vaccinationportal.screening.dto.response;

import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;

import java.time.Instant;
import java.util.List;

public record ScreeningReviewDetailsResponse(
        Long submissionId,
        Long citizenId,
        Long campaignId,
        Long questionnaireVersionId,
        Long consentNoticeVersionId,
        ScreeningSubmissionStatus status,
        PreliminaryScreeningResult preliminaryResult,
        Instant consentAt,
        Instant submittedAt,
        String correctionInstructions,
        Instant correctionRequestedAt,
        List<ScreeningReviewAnswerResponse> answers,
        List<TriggeredRuleEvidenceResponse> triggeredRuleEvidence,
        EligibilityAssessmentResponse assessment
) {}
