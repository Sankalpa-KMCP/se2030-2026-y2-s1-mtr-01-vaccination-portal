package com.se2030.vaccinationportal.screening.submission.mapper;

import com.se2030.vaccinationportal.screening.submission.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ScreeningSubmissionMapperTests {

    private final ScreeningSubmissionMapper mapper = new ScreeningSubmissionMapper();

    @Test
    void mapsCorrectionInstructionsAndRequestedAtToResponse() {
        Instant requestedAt = Instant.parse("2026-08-22T02:00:00Z");
        ScreeningSubmission submission = submissionWithRequiredReferences();
        submission.setStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        submission.setCorrectionInstructions("Please correct the allergy answer.");
        submission.setCorrectionRequestedAt(requestedAt);

        ScreeningSubmissionResponse response = mapper.toResponse(submission);

        assertAll(
                () -> assertThat(response.correctionInstructions())
                        .isEqualTo("Please correct the allergy answer."),
                () -> assertThat(response.correctionRequestedAt())
                        .isEqualTo(requestedAt)
        );
    }

    @Test
    void mapsNullCorrectionMetadataForSubmissionWithoutCorrectionRequest() {
        ScreeningSubmission submission = submissionWithRequiredReferences();
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);

        ScreeningSubmissionResponse response = mapper.toResponse(submission);

        assertAll(
                () -> assertThat(response.correctionInstructions()).isNull(),
                () -> assertThat(response.correctionRequestedAt()).isNull()
        );
    }

    private ScreeningSubmission submissionWithRequiredReferences() {
        QuestionnaireVersion questionnaireVersion = new QuestionnaireVersion();
        questionnaireVersion.setId(20L);
        ConsentNoticeVersion consentNoticeVersion = new ConsentNoticeVersion();
        consentNoticeVersion.setId(30L);

        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(10L);
        submission.setCitizenId(40L);
        submission.setCampaignId(50L);
        submission.setQuestionnaireVersion(questionnaireVersion);
        submission.setConsentNoticeVersion(consentNoticeVersion);
        return submission;
    }
}
