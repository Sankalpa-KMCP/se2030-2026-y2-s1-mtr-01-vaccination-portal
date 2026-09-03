package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.mapper.ScreeningReviewDetailsMapper;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningAnswerRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningTriggeredRuleEvidenceRepository;
import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionEligibilityLifecycleTests {

    private static final Long CITIZEN_ID = 1L;
    private static final Long CAMPAIGN_ID = 2L;
    private static final Long VERSION_ID = 20L;
    private static final Long CONSENT_ID = 30L;
    private static final Long QUESTION_ID = 40L;
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    @Mock
    private ScreeningAnswerRepository answerRepository;

    @Mock
    private QuestionnaireVersionRepository versionRepository;

    @Mock
    private QuestionnaireItemRepository itemRepository;

    @Mock
    private ScreeningConsentNoticeService consentNoticeService;

    @Mock
    private ScreeningAnswerValidator answerValidator;

    @Mock
    private PreliminaryScreeningService preliminaryScreeningService;

    @Mock
    private EligibilityRuleRepository eligibilityRuleRepository;

    @Mock
    private ScreeningTriggeredRuleEvidenceRepository evidenceRepository;

    @Mock
    private CampaignQuestionnaireAssignmentService assignmentService;

    @Mock
    private EligibilityAssessmentRepository assessmentRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private ScreeningReviewDetailsMapper reviewDetailsMapper;

    @Mock
    private VaccinationCampaignService campaignService;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private EligibilityReviewService eligibilityReviewService;
    private ScreeningSubmissionService service;

    @BeforeEach
    void setUp() {
        eligibilityReviewService = new EligibilityReviewService(
                assessmentRepository,
                submissionRepository,
                itemRepository,
                answerOptionRepository,
                evidenceRepository,
                reviewDetailsMapper,
                campaignService,
                clock
        );
        service = new ScreeningSubmissionService(
                submissionRepository,
                answerRepository,
                versionRepository,
                itemRepository,
                consentNoticeService,
                answerValidator,
                preliminaryScreeningService,
                eligibilityRuleRepository,
                evidenceRepository,
                assignmentService,
                eligibilityReviewService,
                clock
        );
    }

    @Test
    void submitScreeningRejectsCurrentEligibleAssessment() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.ELIGIBLE);
        assessment.setValidUntil(NOW.plusSeconds(86_400));
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 31));
        stubSuccessfulSubmissionFlow();

        assertSubmissionRejected(EligibilityAssessmentStatus.ELIGIBLE);
    }

    @Test
    void submitScreeningRejectsIneligibleAssessmentWithoutAuthorization() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.INELIGIBLE);
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 31));
        stubSuccessfulSubmissionFlow();

        assertSubmissionRejected(EligibilityAssessmentStatus.INELIGIBLE);
    }

    @Test
    void submitScreeningRejectsRevokedAssessmentWithoutAuthorization() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.REVOKED);
        assessment.setRevokedAt(NOW.minusSeconds(3_600));
        assessment.setRevocationReason("Original eligibility was revoked");
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 31));
        stubSuccessfulSubmissionFlow();

        assertSubmissionRejected(EligibilityAssessmentStatus.REVOKED);
    }

    @Test
    void submitScreeningAllowsExpiredAssessment() {
        stubLatestAssessment(
                assessment(EligibilityAssessmentStatus.EXPIRED),
                LocalDate.of(2026, 8, 31)
        );
        stubSuccessfulSubmissionFlow();

        assertSubmissionAllowed();
    }

    @Test
    void submitScreeningAllowsAuthorizedIneligibleAssessment() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.INELIGIBLE);
        authorizeRescreening(assessment);
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 31));
        stubSuccessfulSubmissionFlow();

        assertSubmissionAllowed();
    }

    @Test
    void submitScreeningAllowsAuthorizedRevokedAssessment() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.REVOKED);
        assessment.setRevokedAt(NOW.minusSeconds(7_200));
        assessment.setRevocationReason("Original eligibility was revoked");
        authorizeRescreening(assessment);
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 31));
        stubSuccessfulSubmissionFlow();

        assertSubmissionAllowed();
        assertAll(
                () -> assertThat(assessment.getStatus()).isEqualTo(EligibilityAssessmentStatus.REVOKED),
                () -> assertThat(assessment.getRevokedAt()).isEqualTo(NOW.minusSeconds(7_200)),
                () -> assertThat(assessment.getRevocationReason()).isEqualTo("Original eligibility was revoked")
        );
    }

    @Test
    void submitScreeningAllowsPreviouslyEligibleAssessmentAfterEffectiveExpiry() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.ELIGIBLE);
        assessment.setValidUntil(NOW.minusSeconds(1));
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 31));
        stubSuccessfulSubmissionFlow();

        assertSubmissionAllowed();
    }

    @Test
    void submitScreeningAllowsPreviouslyEligibleAssessmentAfterCampaignEnd() {
        EligibilityAssessment assessment = assessment(EligibilityAssessmentStatus.ELIGIBLE);
        assessment.setValidUntil(NOW.plusSeconds(86_400));
        stubLatestAssessment(assessment, LocalDate.of(2026, 8, 23));
        stubSuccessfulSubmissionFlow();

        assertSubmissionAllowed();
    }

    @Test
    void submitScreeningAllowsCitizenWithNoPreviousAssessment() {
        lenient().when(assessmentRepository.findByCitizenIdAndCampaignId(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(List.of());
        stubSuccessfulSubmissionFlow();

        assertSubmissionAllowed();
    }

    private void assertSubmissionRejected(EligibilityAssessmentStatus status) {
        Throwable failure = catchThrowable(this::submitScreening);

        assertAll(
                () -> assertThat(failure)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(status.name()),
                () -> verify(submissionRepository, never()).save(any(ScreeningSubmission.class)),
                () -> verify(answerRepository, never()).save(any(ScreeningAnswer.class)),
                () -> verify(preliminaryScreeningService, never()).evaluateAnswers(any(), anyList())
        );
    }

    private void assertSubmissionAllowed() {
        ScreeningSubmission result = submitScreening();

        assertAll(
                () -> assertThat(result.getStatus()).isEqualTo(ScreeningSubmissionStatus.PENDING_REVIEW),
                () -> assertThat(result.getPreliminaryResult()).isEqualTo(PreliminaryScreeningResult.PRELIMINARY_PASS)
        );
        verify(submissionRepository, atLeastOnce()).save(any(ScreeningSubmission.class));
        verify(answerRepository).save(any(ScreeningAnswer.class));
        verify(preliminaryScreeningService).evaluateAnswers(eq(VERSION_ID), anyList());
    }

    private ScreeningSubmission submitScreening() {
        return service.submitScreening(
                CITIZEN_ID,
                CAMPAIGN_ID,
                VERSION_ID,
                CONSENT_ID,
                Map.of(QUESTION_ID, "No current contraindications")
        );
    }

    private void stubSuccessfulSubmissionFlow() {
        QuestionnaireVersion version = new QuestionnaireVersion();
        version.setId(VERSION_ID);
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);

        ConsentNoticeVersion consent = new ConsentNoticeVersion();
        consent.setId(CONSENT_ID);

        ScreeningQuestion question = new ScreeningQuestion(
                "HEALTH_DETAILS",
                "Provide current health details",
                QuestionType.TEXT,
                true,
                true
        );
        question.setId(QUESTION_ID);

        CampaignQuestionnaireAssignment assignment = mock(CampaignQuestionnaireAssignment.class);
        lenient().when(assignment.getQuestionnaireVersionId()).thenReturn(VERSION_ID);
        lenient().when(submissionRepository.findActiveSubmission(any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        lenient().when(assignmentService.getEffectiveAssignmentAt(eq(CAMPAIGN_ID), any(LocalDate.class)))
                .thenReturn(Optional.of(assignment));
        lenient().when(consentNoticeService.getCurrentScreeningConsentNotice()).thenReturn(consent);
        lenient().when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of());
        lenient().when(answerValidator.validateAndNormalize(eq(VERSION_ID), anyList(), any()))
                .thenReturn(List.of(new ScreeningAnswerValidator.ValidatedAnswer(
                        question,
                        "No current contraindications"
                )));
        lenient().when(submissionRepository.save(any(ScreeningSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(answerRepository.save(any(ScreeningAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(preliminaryScreeningService.evaluateAnswers(eq(VERSION_ID), anyList()))
                .thenReturn(new PreliminaryScreeningService.RuleEvaluationResult(
                        PreliminaryScreeningResult.PRELIMINARY_PASS,
                        List.of()
                ));
    }

    private EligibilityAssessment assessment(EligibilityAssessmentStatus status) {
        EligibilityAssessment assessment = new EligibilityAssessment();
        assessment.setCitizenId(CITIZEN_ID);
        assessment.setCampaignId(CAMPAIGN_ID);
        assessment.setStatus(status);
        assessment.setReviewedAt(NOW.minusSeconds(3_600));
        return assessment;
    }

    private void authorizeRescreening(EligibilityAssessment assessment) {
        assessment.setRescreeningAuthorizedAt(NOW.minusSeconds(1_800));
        assessment.setRescreeningAuthorizedBy(99L);
        assessment.setRescreeningReason("Clinical circumstances changed");
    }

    private void stubLatestAssessment(EligibilityAssessment assessment, LocalDate serviceEndDate) {
        lenient().when(assessmentRepository.findByCitizenIdAndCampaignId(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(List.of(assessment));
        VaccinationCampaign campaign = new VaccinationCampaign(
                "CAMPAIGN-2",
                "Campaign 2",
                null,
                null,
                null,
                serviceEndDate.minusDays(30),
                serviceEndDate,
                NOW.minusSeconds(86_400),
                NOW.plusSeconds(86_400)
        );
        lenient().when(campaignService.getRequiredCampaignById(CAMPAIGN_ID)).thenReturn(campaign);
    }
}
