package com.se2030.vaccinationportal.screening.submission.service;

import com.se2030.vaccinationportal.screening.eligibility.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.questionnaire.service.ScreeningConsentNoticeService;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.campaign.api.screening.QuestionnaireAssignmentSnapshot;
import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.EligibilityRuleRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningAnswerRepository;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.eligibility.repository.ScreeningTriggeredRuleEvidenceRepository;
import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerValidator;
import com.se2030.vaccinationportal.screening.validation.TextAnswerValidationStrategy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Service-level tests for the transactional serialization of concurrent screening submissions (R05):
 * submission must consult the pessimistic-locking active-submission query, and a database-level
 * uniqueness violation must be translated into the same controlled rejection.
 */
@ExtendWith(MockitoExtension.class)
class ScreeningSubmissionConcurrencyGuardTests {

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
    private PreliminaryScreeningService preliminaryScreeningService;
    @Mock
    private EligibilityRuleRepository eligibilityRuleRepository;
    @Mock
    private ScreeningTriggeredRuleEvidenceRepository evidenceRepository;
    @Mock
    private CampaignScreeningReader assignmentService;
    @Mock
    private EligibilityReviewService eligibilityReviewService;

    private ScreeningSubmissionService service;
    private QuestionnaireVersion version;
    private ConsentNoticeVersion consent;
    private ScreeningQuestion question;

    @BeforeEach
    void setUp() {
        service = new ScreeningSubmissionService(
                submissionRepository,
                answerRepository,
                versionRepository,
                itemRepository,
                consentNoticeService,
                new ScreeningAnswerValidator(List.of(new TextAnswerValidationStrategy())),
                preliminaryScreeningService,
                eligibilityRuleRepository,
                evidenceRepository,
                assignmentService,
                eligibilityReviewService,
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC)
        );
        version = new QuestionnaireVersion();
        version.setId(20L);
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
        consent = new ConsentNoticeVersion("PRE_VACCINATION_SCREENING", 1, "Consent", "Content", "hash", Instant.now());
        consent.setId(30L);
        question = new ScreeningQuestion("DETAILS", "Provide details", QuestionType.TEXT, true, true);
        question.setId(40L);
    }

    private void stubSubmissionPrerequisites() {
        when(submissionRepository.findActiveSubmissionForUpdate(any(), any(), anyList())).thenReturn(Optional.empty());
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(consentNoticeService.getCurrentScreeningConsentNotice()).thenReturn(consent);
        lenient().when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(new QuestionnaireItem(version, question, 1, null)));
        QuestionnaireAssignmentSnapshot assignment = new QuestionnaireAssignmentSnapshot(1L, 2L, 20L);
        lenient().when(assignmentService.getEffectiveAssignmentAt(eq(2L), any(LocalDate.class)))
                .thenReturn(Optional.of(assignment));
    }

    @Test
    void submitScreening_usesPessimisticLockingActiveCheckForSerialization() {
        stubSubmissionPrerequisites();
        when(answerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(submissionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(preliminaryScreeningService.evaluateAnswers(any(), anyList()))
                .thenReturn(new PreliminaryScreeningService.RuleEvaluationResult(
                        PreliminaryScreeningResult.PRELIMINARY_PASS, List.of()));

        service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details"));

        // The submission guard must be the pessimistic-locking variant so concurrent submissions serialize
        verify(submissionRepository).findActiveSubmissionForUpdate(eq(1L), eq(2L), anyList());
    }

    @Test
    void submitScreening_activeSubmissionFound_rejectsBeforeAnyPersistence() {
        when(submissionRepository.findActiveSubmissionForUpdate(any(), any(), anyList()))
                .thenReturn(Optional.of(new com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission()));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An active submission already exists for this citizen and campaign");

        verify(answerRepository, never()).save(any());
        verify(preliminaryScreeningService, never()).evaluateAnswers(any(), anyList());
        verify(evidenceRepository, never()).saveAll(any());
    }

    @Test
    void submitScreening_concurrentConstraintViolation_translatesToControlledRejection() {
        stubSubmissionPrerequisites();
        when(submissionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_screening_submission_active_citizen_campaign'"));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An active submission already exists for this citizen and campaign");

        verify(answerRepository, never()).save(any());
        verify(submissionRepository, never()).findActiveSubmission(any(), any(), any());
    }

    @Test
    void submitScreening_hibernateConstraintViolation_translatesToControlledRejection() {
        stubSubmissionPrerequisites();
        java.sql.SQLException sqlEx = new java.sql.SQLException("Unique index violation", "23505");
        org.hibernate.exception.ConstraintViolationException cve =
                new org.hibernate.exception.ConstraintViolationException(
                        "could not execute statement", sqlEx, "uk_screening_submission_active_citizen_campaign");
        when(submissionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("could not execute statement", cve));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An active submission already exists for this citizen and campaign");

        verify(submissionRepository, never()).findActiveSubmission(any(), any(), any());
    }

    @Test
    void submitScreening_h2ConstraintViolationMessage_translatesToControlledRejection() {
        stubSubmissionPrerequisites();
        when(submissionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "Unique index or primary key violation: \"PUBLIC.UK_SCREENING_SUBMISSION_ACTIVE_CITIZEN_CAMPAIGN_INDEX_1 ON PUBLIC.SCREENING_SUBMISSIONS\""));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An active submission already exists for this citizen and campaign");

        verify(submissionRepository, never()).findActiveSubmission(any(), any(), any());
    }

    @Test
    void submitScreening_sqlExceptionCause_translatesToControlledRejection() {
        stubSubmissionPrerequisites();
        java.sql.SQLException sqlEx = new java.sql.SQLException("Duplicate entry '1-2' for key 'uk_screening_submission_active_citizen_campaign'");
        when(submissionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Database error", sqlEx));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An active submission already exists for this citizen and campaign");

        verify(submissionRepository, never()).findActiveSubmission(any(), any(), any());
    }

    @Test
    void submitScreening_unrelatedConstraintViolation_noActiveRow_isRethrownWithoutMisclassification() {
        stubSubmissionPrerequisites();
        when(submissionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails (`fk_screening_submission_citizen`)"));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_screening_submission_citizen");

        verify(answerRepository, never()).save(any());
        verify(submissionRepository, never()).findActiveSubmission(any(), any(), any());
    }

    @Test
    void submitScreening_unrelatedConstraintViolation_withActiveRowPresent_stillRethrownWithoutMisclassification() {
        stubSubmissionPrerequisites();
        // Even if an active row exists in the repository, an unrelated integrity failure must NOT be classified as duplicate-active!
        lenient().when(submissionRepository.findActiveSubmission(any(), any(), any()))
                .thenReturn(Optional.of(new com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission()));

        when(submissionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails (`fk_screening_submission_citizen`)"));

        assertThatThrownBy(() -> service.submitScreening(1L, 2L, 20L, 30L, Map.of(40L, "valid details")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_screening_submission_citizen");

        verify(answerRepository, never()).save(any());
        // Crucial F6 invariant: no repository query is executed after failed flush solely for classification
        verify(submissionRepository, never()).findActiveSubmission(any(), any(), any());
    }
}
