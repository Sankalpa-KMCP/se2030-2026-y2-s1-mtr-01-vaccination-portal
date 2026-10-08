package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.repository.ConsentNoticeVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the canonical consent notice used by MF-01 screening. */
@Service
public class ScreeningConsentNoticeService {

    public static final String SCREENING_NOTICE_CODE = "PRE_VACCINATION_SCREENING";

    private final ConsentNoticeVersionRepository consentNoticeVersionRepository;

    public ScreeningConsentNoticeService(ConsentNoticeVersionRepository consentNoticeVersionRepository) {
        this.consentNoticeVersionRepository = consentNoticeVersionRepository;
    }

    @Transactional(readOnly = true)
    public ConsentNoticeVersion getCurrentScreeningConsentNotice() {
        return consentNoticeVersionRepository
                .findFirstByNoticeCodeAndRetiredAtIsNullOrderByVersionNumberDesc(SCREENING_NOTICE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No current consent notice exists for screening notice code " + SCREENING_NOTICE_CODE
                ));
    }
}
