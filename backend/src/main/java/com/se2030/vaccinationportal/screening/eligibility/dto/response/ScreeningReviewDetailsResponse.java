package com.se2030.vaccinationportal.screening.eligibility.dto.response;

import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;

import java.time.Instant;
import java.util.List;

public record ScreeningReviewDetailsResponse(
        Long submissionId,
        Long citizenId,
        String citizenFullName,
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
