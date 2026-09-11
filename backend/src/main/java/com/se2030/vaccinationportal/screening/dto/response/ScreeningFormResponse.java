package com.se2030.vaccinationportal.screening.dto.response;

import java.util.List;

public record ScreeningFormResponse(
        Long campaignId,
        Long questionnaireVersionId,
        Integer questionnaireVersionNumber,
        ScreeningFormConsentNoticeResponse consentNotice,
        List<ScreeningFormQuestionResponse> questions
) {}
