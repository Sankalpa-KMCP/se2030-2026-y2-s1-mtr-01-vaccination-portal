package com.se2030.vaccinationportal.screening.eligibility.service;

import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
import com.se2030.vaccinationportal.shared.auth.api.AccountDisplayReader;
import com.se2030.vaccinationportal.screening.eligibility.dto.response.ScreeningReviewDetailsResponse;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.eligibility.mapper.EligibilityAssessmentMapper;
import com.se2030.vaccinationportal.screening.eligibility.mapper.ScreeningReviewDetailsMapper;
import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.EligibilityRule;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.eligibility.model.ScreeningTriggeredRuleEvidence;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.eligibility.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.eligibility.repository.ScreeningTriggeredRuleEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityReviewServiceReviewDetailsTests {

    private static final Long SUBMISSION_ID = 30L;
    private static final Long VERSION_ID = 70L;
    private static final Instant NOW = Instant.parse("2026-08-28T06:00:00Z");

    @Mock
    private EligibilityAssessmentRepository assessmentRepository;

    @Mock
    private ScreeningSubmissionRepository submissionRepository;

    @Mock
    private QuestionnaireItemRepository questionnaireItemRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private ScreeningTriggeredRuleEvidenceRepository evidenceRepository;

    @Mock
    private CampaignScreeningReader campaignService;

    @Mock
    private AccountDisplayReader accountDisplayReader;

    private EligibilityReviewService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityReviewService(
                assessmentRepository,
                submissionRepository,
                questionnaireItemRepository,
                answerOptionRepository,
                evidenceRepository,
                new ScreeningReviewDetailsMapper(new EligibilityAssessmentMapper()),
                campaignService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                accountDisplayReader
        );
    }

    @Test
    void getReviewDetailsAssemblesOrderedPersistedAnswersSnapshotsCorrectionsAndAssessment() {
        ScreeningQuestion unanswered = question(10L, "UNANSWERED", "Unanswered question", QuestionType.TEXT, true);
        ScreeningQuestion single = question(11L, "ALLERGY", "Do you have an allergy?", QuestionType.SINGLE_CHOICE, true);
        ScreeningQuestion multi = question(12L, "SYMPTOMS", "Select current symptoms", QuestionType.MULTI_CHOICE, false);

        ScreeningSubmission submission = submission();
        ScreeningAnswer multiAnswer = answer(302L, submission, multi, "fever,cough", NOW.minusSeconds(60));
        ScreeningAnswer singleAnswer = answer(301L, submission, single, "yes", NOW.minusSeconds(120));
        submission.addAnswer(multiAnswer);
        submission.addAnswer(singleAnswer);

        QuestionnaireItem unansweredItem = item(100L, submission.getQuestionnaireVersion(), unanswered, 1, null);
        QuestionnaireItem singleItem = item(101L, submission.getQuestionnaireVersion(), single, 2, false);
        QuestionnaireItem multiItem = item(102L, submission.getQuestionnaireVersion(), multi, 3, null);

        AnswerOption no = option(201L, single, "NO", "No", "no", 1);
        AnswerOption yes = option(202L, single, "YES", "Yes", "yes", 2);
        AnswerOption cough = option(203L, multi, "COUGH", "Cough", "cough", 1);
        AnswerOption fever = option(204L, multi, "FEVER", "Fever", "fever", 2);

        EligibilityRule liveRule = new EligibilityRule();
        liveRule.setId(401L);
        liveRule.setRuleCode("LIVE_CODE_AFTER_CHANGE");
        liveRule.setMessage("Live message after change");
        ScreeningTriggeredRuleEvidence secondEvidence = evidence(
                502L,
                submission,
                liveRule,
                "SNAPSHOT_SECOND",
                "Historical second message",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                20,
                2
        );
        ScreeningTriggeredRuleEvidence firstEvidence = evidence(
                501L,
                submission,
                liveRule,
                "SNAPSHOT_FIRST",
                "Historical first message",
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                10,
                1
        );

        EligibilityAssessment assessment = assessment(submission);
        when(accountDisplayReader.findFullName(10L)).thenReturn(Optional.of("Kawya Arusha"));

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(questionnaireItemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(unansweredItem, singleItem, multiItem));
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(11L))
                .thenReturn(List.of(no, yes));
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(12L))
                .thenReturn(List.of(cough, fever));
        when(evidenceRepository.findBySubmissionIdOrderByEvaluationOrderAsc(SUBMISSION_ID))
                .thenReturn(List.of(secondEvidence, firstEvidence));
        when(assessmentRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.of(assessment));

        ScreeningReviewDetailsResponse result = service.getReviewDetails(SUBMISSION_ID);

        assertAll(
                () -> assertThat(result.submissionId()).isEqualTo(SUBMISSION_ID),
                () -> assertThat(result.citizenId()).isEqualTo(10L),
                () -> assertThat(result.citizenFullName()).isEqualTo("Kawya Arusha"),
                () -> assertThat(result.campaignId()).isEqualTo(20L),
                () -> assertThat(result.questionnaireVersionId()).isEqualTo(VERSION_ID),
                () -> assertThat(result.consentNoticeVersionId()).isEqualTo(80L),
                () -> assertThat(result.status()).isEqualTo(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED),
                () -> assertThat(result.preliminaryResult())
                        .isEqualTo(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED),
                () -> assertThat(result.consentAt()).isEqualTo(NOW.minusSeconds(3_600)),
                () -> assertThat(result.submittedAt()).isEqualTo(NOW.minusSeconds(1_800)),
                () -> assertThat(result.correctionInstructions()).isEqualTo("Clarify the allergy response"),
                () -> assertThat(result.correctionRequestedAt()).isEqualTo(NOW.minusSeconds(900)),
                () -> assertThat(result.answers()).extracting(answer -> answer.questionId())
                        .containsExactly(11L, 12L),
                () -> assertThat(result.answers()).extracting(answer -> answer.displayOrder())
                        .containsExactly(2, 3),
                () -> assertThat(result.answers().get(0).questionnaireItemId()).isEqualTo(101L),
                () -> assertThat(result.answers().get(0).questionCode()).isEqualTo("ALLERGY"),
                () -> assertThat(result.answers().get(0).questionText()).isEqualTo("Do you have an allergy?"),
                () -> assertThat(result.answers().get(0).questionType()).isEqualTo(QuestionType.SINGLE_CHOICE),
                () -> assertThat(result.answers().get(0).required()).isFalse(),
                () -> assertThat(result.answers().get(0).typedAnswer()).isEqualTo("yes"),
                () -> assertThat(result.answers().get(0).selectedOptions())
                        .extracting(option -> option.id(), option -> option.code(), option -> option.label(), option -> option.value())
                        .containsExactly(org.assertj.core.groups.Tuple.tuple(202L, "YES", "Yes", "yes")),
                () -> assertThat(result.answers().get(1).required()).isFalse(),
                () -> assertThat(result.answers().get(1).typedAnswer()).isEqualTo("fever,cough"),
                () -> assertThat(result.answers().get(1).selectedOptions())
                        .extracting(option -> option.code())
                        .containsExactly("FEVER", "COUGH"),
                () -> assertThat(result.triggeredRuleEvidence())
                        .extracting(evidence -> evidence.evaluationOrder())
                        .containsExactly(1, 2),
                () -> assertThat(result.triggeredRuleEvidence().get(0).ruleCode()).isEqualTo("SNAPSHOT_FIRST"),
                () -> assertThat(result.triggeredRuleEvidence().get(0).message())
                        .isEqualTo("Historical first message"),
                () -> assertThat(result.triggeredRuleEvidence().get(0).ruleCode())
                        .isNotEqualTo(liveRule.getRuleCode()),
                () -> assertThat(result.assessment()).isNotNull(),
                () -> assertThat(result.assessment().id()).isEqualTo(601L),
                () -> assertThat(result.assessment().status()).isEqualTo(EligibilityAssessmentStatus.INELIGIBLE)
        );

        verify(evidenceRepository).findBySubmissionIdOrderByEvaluationOrderAsc(SUBMISSION_ID);
    }

    @Test
    void getReviewDetailsKeepsTypedAnswerWhenOptionalChoiceMetadataCannotBeResolved() {
        ScreeningQuestion question = question(11L, "ALLERGY", "Do you have an allergy?", QuestionType.SINGLE_CHOICE, true);
        ScreeningSubmission submission = submission();
        submission.addAnswer(answer(301L, submission, question, "historical-value", NOW.minusSeconds(60)));
        QuestionnaireItem item = item(101L, submission.getQuestionnaireVersion(), question, 1, null);

        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(submission));
        when(questionnaireItemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(item));
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(11L)).thenReturn(List.of());
        when(evidenceRepository.findBySubmissionIdOrderByEvaluationOrderAsc(SUBMISSION_ID)).thenReturn(List.of());
        when(assessmentRepository.findBySubmissionId(SUBMISSION_ID)).thenReturn(Optional.empty());

        ScreeningReviewDetailsResponse result = service.getReviewDetails(SUBMISSION_ID);

        assertAll(
                () -> assertThat(result.answers()).hasSize(1),
                () -> assertThat(result.citizenFullName()).isNull(),
                () -> assertThat(result.answers().get(0).typedAnswer()).isEqualTo("historical-value"),
                () -> assertThat(result.answers().get(0).selectedOptions()).isEmpty(),
                () -> assertThat(result.assessment()).isNull()
        );
    }

    @Test
    void getReviewDetailsRejectsMissingSubmissionBeforeLoadingReviewRelations() {
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReviewDetails(SUBMISSION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(SUBMISSION_ID.toString());

        verifyNoInteractions(questionnaireItemRepository, answerOptionRepository, evidenceRepository);
        verify(assessmentRepository, never()).findBySubmissionId(SUBMISSION_ID);
    }

    private ScreeningSubmission submission() {
        QuestionnaireVersion version = new QuestionnaireVersion();
        version.setId(VERSION_ID);
        ConsentNoticeVersion consent = new ConsentNoticeVersion();
        consent.setId(80L);

        ScreeningSubmission submission = new ScreeningSubmission(
                10L,
                20L,
                version,
                consent,
                NOW.minusSeconds(3_600)
        );
        submission.setId(SUBMISSION_ID);
        submission.setStatus(ScreeningSubmissionStatus.CORRECTIONS_REQUIRED);
        submission.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED);
        submission.setSubmittedAt(NOW.minusSeconds(1_800));
        submission.setCorrectionInstructions("Clarify the allergy response");
        submission.setCorrectionRequestedAt(NOW.minusSeconds(900));
        return submission;
    }

    private ScreeningQuestion question(
            Long id,
            String code,
            String text,
            QuestionType type,
            boolean defaultRequired
    ) {
        ScreeningQuestion question = new ScreeningQuestion(code, text, type, defaultRequired, true);
        question.setId(id);
        return question;
    }

    private QuestionnaireItem item(
            Long id,
            QuestionnaireVersion version,
            ScreeningQuestion question,
            int displayOrder,
            Boolean requiredOverride
    ) {
        QuestionnaireItem item = new QuestionnaireItem(version, question, displayOrder, requiredOverride);
        item.setId(id);
        return item;
    }

    private ScreeningAnswer answer(
            Long id,
            ScreeningSubmission submission,
            ScreeningQuestion question,
            String typedAnswer,
            Instant createdAt
    ) {
        ScreeningAnswer answer = new ScreeningAnswer(submission, question, typedAnswer);
        answer.setId(id);
        answer.setCreatedAt(createdAt);
        return answer;
    }

    private AnswerOption option(
            Long id,
            ScreeningQuestion question,
            String code,
            String label,
            String value,
            int displayOrder
    ) {
        AnswerOption option = new AnswerOption(question, code, label, value, displayOrder);
        option.setId(id);
        return option;
    }

    private ScreeningTriggeredRuleEvidence evidence(
            Long id,
            ScreeningSubmission submission,
            EligibilityRule sourceRule,
            String snapshotCode,
            String snapshotMessage,
            PreliminaryScreeningResult result,
            int priority,
            int evaluationOrder
    ) {
        ScreeningTriggeredRuleEvidence evidence = new ScreeningTriggeredRuleEvidence(
                submission,
                sourceRule,
                snapshotCode,
                snapshotMessage,
                result,
                priority,
                evaluationOrder
        );
        evidence.setId(id);
        evidence.setCreatedAt(NOW.minusSeconds(300));
        return evidence;
    }

    private EligibilityAssessment assessment(ScreeningSubmission submission) {
        EligibilityAssessment assessment = new EligibilityAssessment(
                submission,
                10L,
                20L,
                90L,
                EligibilityAssessmentStatus.INELIGIBLE,
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED,
                "Clinical review completed",
                NOW.minusSeconds(300),
                null
        );
        assessment.setId(601L);
        assessment.setCreatedAt(NOW.minusSeconds(300));
        assessment.setUpdatedAt(NOW.minusSeconds(300));
        return assessment;
    }
}
