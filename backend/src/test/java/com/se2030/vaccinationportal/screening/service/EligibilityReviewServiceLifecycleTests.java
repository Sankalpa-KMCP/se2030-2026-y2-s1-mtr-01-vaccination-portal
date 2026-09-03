package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityReviewServiceLifecycleTests {

    private static final Long CITIZEN_ID = 10L;
    private static final Long CAMPAIGN_ID = 20L;
    private static final Long SUBMISSION_ID = 30L;
    private static final Long REVIEWER_ID = 40L;
    private static final Long ASSESSMENT_ID = 50L;
    private static final Instant REVIEWED_AT = Instant.parse("2026-08-24T12:00:00Z");

    @Mock
    private EligibilityAssessmentRepository assessmentRepository;

    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    @Mock
    private VaccinationCampaignService campaignService;

    @Spy
    private Clock clock = Clock.fixed(REVIEWED_AT, ZoneOffset.UTC);

    @InjectMocks
    private EligibilityReviewService service;

    @Test
    void recordDecisionRejectsCorrectionsRequiredSubmission() {
        ScreeningSubmission submission = submissionWithStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.INELIGIBLE,
                "Clinical review found a contraindication",
                null,
                null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_REVIEW");

        assertThat(submission.getStatus()).isEqualTo(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        verify(submissionRepository, never()).save(any(ScreeningSubmission.class));
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void recordEligibleDecisionRejectsMissingValidUntil() {
        ScreeningSubmission submission = submissionWithStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validUntil");

        assertThat(submission.getStatus()).isEqualTo(ScreeningSubmissionStatus.PENDING_REVIEW);
        verify(submissionRepository, never()).save(any(ScreeningSubmission.class));
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void recordEligibleDecisionRejectsValidUntilNotAfterReviewedAt() {
        ScreeningSubmission beforeReview = submissionWithStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        ScreeningSubmission equalToReview = submissionWithStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        when(submissionRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(beforeReview))
                .thenReturn(Optional.of(equalToReview));

        assertThatThrownBy(() -> service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                null,
                REVIEWED_AT.minusSeconds(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validUntil");

        assertThatThrownBy(() -> service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                null,
                REVIEWED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validUntil");

        assertAll(
                () -> assertThat(beforeReview.getStatus()).isEqualTo(ScreeningSubmissionStatus.PENDING_REVIEW),
                () -> assertThat(equalToReview.getStatus()).isEqualTo(ScreeningSubmissionStatus.PENDING_REVIEW)
        );
        verify(submissionRepository, never()).save(any(ScreeningSubmission.class));
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void recordEligibleDecisionUsesInjectedClock() {
        ScreeningSubmission submission = submissionWithStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        stubSuccessfulDecision(submission);

        EligibilityAssessment result = service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                "Citizen is clinically eligible",
                "No clinical concerns",
                REVIEWED_AT.plusSeconds(86_400)
        );

        assertAll(
                () -> assertThat(result.getReviewedAt()).isEqualTo(REVIEWED_AT),
                () -> assertThat(result.getValidUntil()).isEqualTo(REVIEWED_AT.plusSeconds(86_400)),
                () -> assertThat(submission.getStatus()).isEqualTo(ScreeningSubmissionStatus.REVIEWED)
        );
        verify(submissionRepository).save(same(submission));
        verify(assessmentRepository).save(same(result));
    }

    @Test
    void recordIneligibleDecisionAllowsNullValidUntil() {
        ScreeningSubmission submission = submissionWithStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        stubSuccessfulDecision(submission);

        EligibilityAssessment result = service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.INELIGIBLE,
                "Clinical review found a contraindication",
                null,
                null
        );

        assertAll(
                () -> assertThat(result.getStatus()).isEqualTo(EligibilityAssessmentStatus.INELIGIBLE),
                () -> assertThat(result.getValidUntil()).isNull(),
                () -> assertThat(submission.getStatus()).isEqualTo(ScreeningSubmissionStatus.REVIEWED)
        );
        verify(submissionRepository).save(same(submission));
        verify(assessmentRepository).save(same(result));
    }

    @Test
    void recordDecisionRejectsSecondSequentialDecisionWithoutAdditionalPersistence() {
        ScreeningSubmission reviewedSubmission = submissionWithStatus(ScreeningSubmissionStatus.REVIEWED);
        EligibilityAssessment originalAssessment = new EligibilityAssessment(
                reviewedSubmission,
                CITIZEN_ID,
                CAMPAIGN_ID,
                REVIEWER_ID,
                EligibilityAssessmentStatus.ELIGIBLE,
                PreliminaryScreeningResult.PRELIMINARY_PASS,
                "Original final decision",
                REVIEWED_AT.minusSeconds(3_600),
                REVIEWED_AT.plusSeconds(86_400)
        );
        reviewedSubmission.setAssessment(originalAssessment);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(reviewedSubmission));

        assertThatThrownBy(() -> service.recordDecision(
                SUBMISSION_ID,
                REVIEWER_ID + 1,
                EligibilityAssessmentStatus.INELIGIBLE,
                "Attempted second final decision",
                "Should not be stored",
                null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_REVIEW");

        assertAll(
                () -> assertThat(reviewedSubmission.getStatus()).isEqualTo(ScreeningSubmissionStatus.REVIEWED),
                () -> assertThat(reviewedSubmission.getAssessment()).isSameAs(originalAssessment),
                () -> assertThat(originalAssessment.getStatus()).isEqualTo(EligibilityAssessmentStatus.ELIGIBLE),
                () -> assertThat(originalAssessment.getReason()).isEqualTo("Original final decision")
        );
        verify(submissionRepository, never()).save(any(ScreeningSubmission.class));
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void revokeEligibleAssessmentPreservesDecisionMetadataAndPersistsTrimmedReason() {
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.ELIGIBLE);
        assessment.setCitizenId(CITIZEN_ID);
        assessment.setCampaignId(CAMPAIGN_ID);
        assessment.setReviewerId(REVIEWER_ID);
        assessment.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_PASS);
        assessment.setReason("Original clinical decision");
        assessment.setClinicalNotes("Original clinical notes");
        assessment.setReviewedAt(REVIEWED_AT.minusSeconds(3_600));
        assessment.setValidUntil(REVIEWED_AT.plusSeconds(86_400));
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(same(assessment))).thenReturn(assessment);

        EligibilityAssessment result = service.revokeAssessment(
                ASSESSMENT_ID,
                REVIEWER_ID + 1,
                "  Eligibility withdrawn after clinical review  "
        );

        assertAll(
                () -> assertThat(result).isSameAs(assessment),
                () -> assertThat(assessment.getStatus()).isEqualTo(EligibilityAssessmentStatus.REVOKED),
                () -> assertThat(assessment.getReviewerId()).isEqualTo(REVIEWER_ID),
                () -> assertThat(assessment.getPreliminaryResult())
                        .isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS),
                () -> assertThat(assessment.getReason()).isEqualTo("Original clinical decision"),
                () -> assertThat(assessment.getClinicalNotes()).isEqualTo("Original clinical notes"),
                () -> assertThat(assessment.getReviewedAt()).isEqualTo(REVIEWED_AT.minusSeconds(3_600)),
                () -> assertThat(assessment.getValidUntil()).isEqualTo(REVIEWED_AT.plusSeconds(86_400)),
                () -> assertThat(assessment.getRevocationReason())
                        .isEqualTo("Eligibility withdrawn after clinical review"),
                () -> assertThat(assessment.getRevokedAt()).isNotNull()
        );
        verify(assessmentRepository).save(same(assessment));
    }

    @Test
    void revokeAssessmentRejectsRepeatedRevocationWithoutMutationOrSave() {
        Instant originalRevokedAt = REVIEWED_AT.minusSeconds(1_800);
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.REVOKED);
        assessment.setReviewerId(REVIEWER_ID);
        assessment.setReason("Original clinical decision");
        assessment.setReviewedAt(REVIEWED_AT.minusSeconds(3_600));
        assessment.setValidUntil(REVIEWED_AT.plusSeconds(86_400));
        assessment.setRevokedAt(originalRevokedAt);
        assessment.setRevocationReason("Original revocation reason");
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.revokeAssessment(
                ASSESSMENT_ID,
                REVIEWER_ID + 1,
                "Replacement revocation reason"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ELIGIBLE");

        assertAll(
                () -> assertThat(assessment.getStatus()).isEqualTo(EligibilityAssessmentStatus.REVOKED),
                () -> assertThat(assessment.getReviewerId()).isEqualTo(REVIEWER_ID),
                () -> assertThat(assessment.getReason()).isEqualTo("Original clinical decision"),
                () -> assertThat(assessment.getReviewedAt()).isEqualTo(REVIEWED_AT.minusSeconds(3_600)),
                () -> assertThat(assessment.getValidUntil()).isEqualTo(REVIEWED_AT.plusSeconds(86_400)),
                () -> assertThat(assessment.getRevokedAt()).isEqualTo(originalRevokedAt),
                () -> assertThat(assessment.getRevocationReason()).isEqualTo("Original revocation reason")
        );
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void authorizeRescreeningRejectsEligibleAssessment() {
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.ELIGIBLE);
        assessment.setValidUntil(REVIEWED_AT.plusSeconds(86_400));
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.authorizeRescreening(
                ASSESSMENT_ID,
                REVIEWER_ID,
                "Authorize another clinical screening"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ELIGIBLE");

        assertAll(
                () -> assertThat(assessment.getRescreeningAuthorizedAt()).isNull(),
                () -> assertThat(assessment.getRescreeningAuthorizedBy()).isNull(),
                () -> assertThat(assessment.getRescreeningReason()).isNull()
        );
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void authorizeRescreeningRejectsExpiredAssessment() {
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.EXPIRED);
        assessment.setValidUntil(REVIEWED_AT.minusSeconds(1));
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.authorizeRescreening(
                ASSESSMENT_ID,
                REVIEWER_ID,
                "Authorize another clinical screening"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXPIRED");

        assertAll(
                () -> assertThat(assessment.getRescreeningAuthorizedAt()).isNull(),
                () -> assertThat(assessment.getRescreeningAuthorizedBy()).isNull(),
                () -> assertThat(assessment.getRescreeningReason()).isNull()
        );
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    @Test
    void authorizeRescreeningAllowsIneligibleAssessment() {
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.INELIGIBLE);
        stubSuccessfulRescreeningAuthorization(assessment);

        EligibilityAssessment result = service.authorizeRescreening(
                ASSESSMENT_ID,
                REVIEWER_ID,
                "Clinical circumstances have changed"
        );

        assertAll(
                () -> assertThat(result).isSameAs(assessment),
                () -> assertThat(assessment.getStatus()).isEqualTo(EligibilityAssessmentStatus.INELIGIBLE),
                () -> assertThat(assessment.getRescreeningAuthorizedAt()).isNotNull(),
                () -> assertThat(assessment.getRescreeningAuthorizedBy()).isEqualTo(REVIEWER_ID),
                () -> assertThat(assessment.getRescreeningReason()).isEqualTo("Clinical circumstances have changed")
        );
        verify(assessmentRepository).save(same(assessment));
    }

    @Test
    void authorizeRescreeningAllowsRevokedAssessment() {
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.REVOKED);
        Instant revokedAt = REVIEWED_AT.minusSeconds(3_600);
        assessment.setRevokedAt(revokedAt);
        assessment.setRevocationReason("Original eligibility was revoked");
        stubSuccessfulRescreeningAuthorization(assessment);

        EligibilityAssessment result = service.authorizeRescreening(
                ASSESSMENT_ID,
                REVIEWER_ID,
                "Citizen may undergo a new clinical screening"
        );

        assertAll(
                () -> assertThat(result).isSameAs(assessment),
                () -> assertThat(assessment.getStatus()).isEqualTo(EligibilityAssessmentStatus.REVOKED),
                () -> assertThat(assessment.getRevokedAt()).isEqualTo(revokedAt),
                () -> assertThat(assessment.getRevocationReason()).isEqualTo("Original eligibility was revoked"),
                () -> assertThat(assessment.getRescreeningAuthorizedAt()).isNotNull(),
                () -> assertThat(assessment.getRescreeningAuthorizedBy()).isEqualTo(REVIEWER_ID),
                () -> assertThat(assessment.getRescreeningReason())
                        .isEqualTo("Citizen may undergo a new clinical screening")
        );
        verify(assessmentRepository).save(same(assessment));
    }

    @Test
    void authorizeRescreeningUsesInjectedClock() {
        EligibilityAssessment assessment = assessmentWithStatus(EligibilityAssessmentStatus.INELIGIBLE);
        stubSuccessfulRescreeningAuthorization(assessment);

        EligibilityAssessment result = service.authorizeRescreening(
                ASSESSMENT_ID,
                REVIEWER_ID,
                "Clinical circumstances have changed"
        );

        assertThat(result.getRescreeningAuthorizedAt()).isEqualTo(REVIEWED_AT);
        verify(assessmentRepository).save(same(assessment));
    }

    @Test
    void currentEligibilityUsesLatestCampaignAssessment() {
        EligibilityAssessment olderEligible = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(7_200),
                REVIEWED_AT.plusSeconds(86_400)
        );
        EligibilityAssessment latestIneligible = campaignAssessment(
                EligibilityAssessmentStatus.INELIGIBLE,
                REVIEWED_AT.minusSeconds(3_600),
                null
        );
        EligibilityAssessment olderIneligible = campaignAssessment(
                EligibilityAssessmentStatus.INELIGIBLE,
                REVIEWED_AT.minusSeconds(7_200),
                null
        );
        EligibilityAssessment latestEligible = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(3_600),
                REVIEWED_AT.plusSeconds(86_400)
        );

        lenient().when(assessmentRepository.findByCitizenIdAndCampaignId(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(List.of(latestIneligible, olderEligible))
                .thenReturn(List.of(latestEligible, olderIneligible));
        lenient().when(assessmentRepository.findByCitizenIdAndCampaignIdAndStatus(
                        CITIZEN_ID,
                        CAMPAIGN_ID,
                        EligibilityAssessmentStatus.ELIGIBLE
                ))
                .thenReturn(Optional.of(olderEligible))
                .thenReturn(Optional.empty());
        stubCampaignEndingOn(LocalDate.of(2026, 8, 31));

        assertAll(
                () -> assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isFalse(),
                () -> assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isTrue()
        );
    }

    @Test
    void currentEligibilityUsesInjectedClockForValidityExpiry() {
        EligibilityAssessment expiredBeforeFixedTime = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(7_200),
                REVIEWED_AT.minusSeconds(60)
        );
        EligibilityAssessment validAfterFixedTime = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(3_600),
                REVIEWED_AT.plusSeconds(60)
        );

        lenient().when(assessmentRepository.findByCitizenIdAndCampaignId(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(List.of(expiredBeforeFixedTime))
                .thenReturn(List.of(validAfterFixedTime));
        lenient().when(assessmentRepository.findByCitizenIdAndCampaignIdAndStatus(
                        CITIZEN_ID,
                        CAMPAIGN_ID,
                        EligibilityAssessmentStatus.ELIGIBLE
                ))
                .thenReturn(Optional.of(expiredBeforeFixedTime))
                .thenReturn(Optional.of(validAfterFixedTime));
        stubCampaignEndingOn(LocalDate.of(2026, 8, 31));

        assertAll(
                () -> assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isFalse(),
                () -> assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isTrue()
        );
    }

    @Test
    void currentEligibilityEndsAfterCampaignServiceEndDate() {
        EligibilityAssessment eligible = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(3_600),
                Instant.parse("2030-01-01T00:00:00Z")
        );
        stubCurrentEligibilityAssessment(eligible);
        stubCampaignEndingOn(LocalDate.of(2026, 8, 23));

        assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isFalse();
    }

    @Test
    void currentEligibilityRemainsValidOnCampaignServiceEndDate() {
        EligibilityAssessment eligible = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(3_600),
                REVIEWED_AT.plusSeconds(86_400)
        );
        stubCurrentEligibilityAssessment(eligible);
        stubCampaignEndingOn(LocalDate.of(2026, 8, 24));

        assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isTrue();
        verify(campaignService).getRequiredCampaignById(CAMPAIGN_ID);
    }

    @Test
    void currentEligibilityRejectsRevokedEligibleAssessment() {
        EligibilityAssessment revoked = campaignAssessment(
                EligibilityAssessmentStatus.ELIGIBLE,
                REVIEWED_AT.minusSeconds(3_600),
                REVIEWED_AT.plusSeconds(86_400)
        );
        revoked.setRevokedAt(REVIEWED_AT.minusSeconds(1_800));
        stubCurrentEligibilityAssessment(revoked);
        stubCampaignEndingOn(LocalDate.of(2026, 8, 31));

        assertThat(service.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID)).isFalse();
        verify(assessmentRepository, never()).save(any(EligibilityAssessment.class));
    }

    private ScreeningSubmission submissionWithStatus(ScreeningSubmissionStatus status) {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(SUBMISSION_ID);
        submission.setCitizenId(CITIZEN_ID);
        submission.setCampaignId(CAMPAIGN_ID);
        submission.setStatus(status);
        submission.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_PASS);
        return submission;
    }

    private void stubSuccessfulDecision(ScreeningSubmission submission) {
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(same(submission))).thenReturn(submission);
        when(assessmentRepository.save(any(EligibilityAssessment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EligibilityAssessment assessmentWithStatus(EligibilityAssessmentStatus status) {
        EligibilityAssessment assessment = new EligibilityAssessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setStatus(status);
        return assessment;
    }

    private void stubSuccessfulRescreeningAuthorization(EligibilityAssessment assessment) {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(same(assessment))).thenReturn(assessment);
    }

    private EligibilityAssessment campaignAssessment(
            EligibilityAssessmentStatus status,
            Instant reviewedAt,
            Instant validUntil
    ) {
        EligibilityAssessment assessment = assessmentWithStatus(status);
        assessment.setCitizenId(CITIZEN_ID);
        assessment.setCampaignId(CAMPAIGN_ID);
        assessment.setReviewedAt(reviewedAt);
        assessment.setValidUntil(validUntil);
        return assessment;
    }

    private void stubCurrentEligibilityAssessment(EligibilityAssessment assessment) {
        lenient().when(assessmentRepository.findByCitizenIdAndCampaignId(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(List.of(assessment));
        lenient().when(assessmentRepository.findByCitizenIdAndCampaignIdAndStatus(
                        CITIZEN_ID,
                        CAMPAIGN_ID,
                        EligibilityAssessmentStatus.ELIGIBLE
                ))
                .thenReturn(Optional.of(assessment));
    }

    private void stubCampaignEndingOn(LocalDate serviceEndDate) {
        VaccinationCampaign campaign = new VaccinationCampaign(
                "CAMPAIGN-20",
                "Campaign 20",
                null,
                null,
                null,
                serviceEndDate.minusDays(30),
                serviceEndDate,
                REVIEWED_AT.minusSeconds(86_400),
                REVIEWED_AT.plusSeconds(86_400)
        );
        lenient().when(campaignService.getRequiredCampaignById(CAMPAIGN_ID)).thenReturn(campaign);
    }
}
