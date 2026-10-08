package com.se2030.vaccinationportal.screening.submission.dto.response;

import com.se2030.vaccinationportal.screening.questionnaire.dto.response.ScreeningFormConsentNoticeResponse;


import java.util.List;

public record ScreeningFormResponse(
        Long campaignId,
        Long questionnaireVersionId,
        Integer questionnaireVersionNumber,
        ScreeningFormConsentNoticeResponse consentNotice,
        List<ScreeningFormQuestionResponse> questions
) {}
