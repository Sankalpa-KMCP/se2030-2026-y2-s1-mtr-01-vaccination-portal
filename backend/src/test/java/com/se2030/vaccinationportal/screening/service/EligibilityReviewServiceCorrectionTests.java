package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityReviewServiceCorrectionTests {

    private static final Instant CORRECTION_REQUESTED_AT = Instant.parse("2026-08-22T00:30:00Z");

    @Mock
    private EligibilityAssessmentRepository assessmentRepository;

    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    private EligibilityReviewService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityReviewService(
                assessmentRepository,
                submissionRepository,
                Clock.fixed(CORRECTION_REQUESTED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void requestCorrectionsPersistsTrimmedInstructionsAndTransitionsPendingSubmission() {
        ScreeningSubmission submission = pendingSubmission();
        when(submissionRepository.findById(30L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(same(submission))).thenReturn(submission);

        ScreeningSubmission result = service.requestCorrections(
                30L,
                "  Please correct the allergy and medication answers.  "
        );

        assertAll(
                () -> assertThat(result).isSameAs(submission),
                () -> assertThat(submission.getStatus()).isEqualTo(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED),
                () -> assertThat(submission.getCorrectionInstructions())
                        .isEqualTo("Please correct the allergy and medication answers."),
                () -> assertThat(submission.getCorrectionRequestedAt())
                        .isEqualTo(CORRECTION_REQUESTED_AT)
        );
        verify(submissionRepository).save(same(submission));
    }

    @Test
    void requestCorrectionsPreservesSubmissionIdentityAndOwnership() {
        QuestionnaireVersion questionnaireVersion = new QuestionnaireVersion();
        questionnaireVersion.setId(40L);
        ConsentNoticeVersion consentVersion = new ConsentNoticeVersion();
        consentVersion.setId(50L);

        ScreeningSubmission submission = new ScreeningSubmission(
                10L,
                20L,
                questionnaireVersion,
                consentVersion,
                Instant.parse("2026-08-20T08:00:00Z")
        );
        submission.setId(30L);
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);

        ScreeningQuestion question = new ScreeningQuestion(
                "ALLERGY_DETAILS",
                "Provide allergy details",
                QuestionType.TEXT,
                true,
                true
        );
        question.setId(60L);
        ScreeningAnswer answer = new ScreeningAnswer(submission, question, "Original allergy details");
        answer.setId(70L);
        submission.addAnswer(answer);

        when(submissionRepository.findById(30L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(same(submission))).thenReturn(submission);

        ScreeningSubmission result = service.requestCorrections(30L, "Correct the allergy answer");

        assertAll(
                () -> assertThat(result).isSameAs(submission),
                () -> assertThat(submission.getId()).isEqualTo(30L),
                () -> assertThat(submission.getCitizenId()).isEqualTo(10L),
                () -> assertThat(submission.getCampaignId()).isEqualTo(20L),
                () -> assertThat(submission.getQuestionnaireVersion()).isSameAs(questionnaireVersion),
                () -> assertThat(submission.getConsentNoticeVersion()).isSameAs(consentVersion),
                () -> assertThat(submission.getAnswers()).containsExactly(answer),
                () -> assertThat(answer.getSubmission()).isSameAs(submission)
        );
        verify(submissionRepository).save(same(submission));
    }

    @Test
    void requestCorrectionsRejectsSubmissionNotPendingReview() {
        ScreeningSubmission submission = pendingSubmission();
        submission.setStatus(ScreeningSubmissionStatus.REVIEWED);
        when(submissionRepository.findById(30L)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.requestCorrections(30L, "Correct the allergy answer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_REVIEW");

        assertThat(submission.getStatus()).isEqualTo(ScreeningSubmissionStatus.REVIEWED);
        verify(submissionRepository, never()).save(submission);
    }

    private ScreeningSubmission pendingSubmission() {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(30L);
        submission.setCitizenId(10L);
        submission.setCampaignId(20L);
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        return submission;
    }
}
