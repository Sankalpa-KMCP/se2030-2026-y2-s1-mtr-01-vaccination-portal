package com.se2030.vaccinationportal.screening.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.Questionnaire;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Database-level invariant tests for the one-active-screening-submission rule (R05).
 * The generated column {@code active_campaign_id} plus the unique constraint
 * {@code uk_screening_submission_active_citizen_campaign} guarantee at most one active submission
 * (DRAFT / PENDING_REVIEW / CORRECTIONS_REQUIRED) per citizen and campaign, while terminal
 * historical submissions (REVIEWED) still permit legitimate later rescreening.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScreeningSubmissionActiveUniquenessTests {

    @Autowired
    private ScreeningSubmissionRepository submissionRepository;

    @Autowired
    private QuestionnaireVersionRepository versionRepository;

    @Autowired
    private com.se2030.vaccinationportal.screening.repository.QuestionnaireRepository questionnaireRepository;

    @Autowired
    private com.se2030.vaccinationportal.screening.repository.ConsentNoticeVersionRepository consentNoticeVersionRepository;

    private QuestionnaireVersion publishedVersion;
    private ConsentNoticeVersion consent;

    private void seedReferenceData() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Questionnaire questionnaire = questionnaireRepository.save(
                new Questionnaire("R05-Q-" + suffix, "R05 Questionnaire", "R05 invariant fixture"));
        publishedVersion = versionRepository.save(
                new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.PUBLISHED));
        consent = consentNoticeVersionRepository.save(
                new ConsentNoticeVersion("R05_NOTICE-" + suffix, 1, "Consent", "Content", "hash-" + suffix, Instant.now()));
    }

    private ScreeningSubmission activeSubmission(Long citizenId, Long campaignId) {
        ScreeningSubmission submission = new ScreeningSubmission(
                citizenId, campaignId, publishedVersion, consent, Instant.now());
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        submission.setSubmittedAt(Instant.now());
        return submission;
    }

    @Test
    void firstActiveSubmission_succeeds() {
        seedReferenceData();
        ScreeningSubmission saved = submissionRepository.saveAndFlush(activeSubmission(7001L, 8001L));
        assertNotNull(saved.getId());
        assertEquals(ScreeningSubmissionStatus.PENDING_REVIEW, saved.getStatus());
    }

    @Test
    void duplicateActiveSubmission_isRejectedByDatabaseInvariant() {
        seedReferenceData();
        submissionRepository.saveAndFlush(activeSubmission(7002L, 8002L));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> submissionRepository.saveAndFlush(activeSubmission(7002L, 8002L)),
                "A second active submission for the same citizen/campaign must violate the unique invariant"
        );

        List<ScreeningSubmission> rows = submissionRepository.findByCitizenIdAndCampaignId(7002L, 8002L);
        assertEquals(1, rows.size(), "Exactly one active submission row may exist");
    }

    @Test
    void duplicateActiveSubmission_viaConcurrentInserts_producesExactlyOneActiveRow() throws Exception {
        seedReferenceData();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> insertTask = () -> {
                try {
                    submissionRepository.saveAndFlush(activeSubmission(7003L, 8003L));
                    return true;
                } catch (DataIntegrityViolationException exception) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(insertTask);
            Future<Boolean> second = executor.submit(insertTask);

            boolean firstResult = first.get(30, TimeUnit.SECONDS);
            boolean secondResult = second.get(30, TimeUnit.SECONDS);

            assertTrue(firstResult ^ secondResult || (firstResult && secondResult),
                    "At least one insert must complete without error");
            List<ScreeningSubmission> rows = submissionRepository.findByCitizenIdAndCampaignId(7003L, 8003L);
            assertEquals(1, rows.size(),
                    "The database invariant must keep exactly one active submission row after concurrent inserts");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void terminalHistoricalSubmission_doesNotPreventAuthorizedRescreening() {
        seedReferenceData();
        ScreeningSubmission first = submissionRepository.saveAndFlush(activeSubmission(7004L, 8004L));

        // Reviewer finalizes the historical submission (terminal REVIEWED status)
        first.setStatus(ScreeningSubmissionStatus.REVIEWED);
        submissionRepository.saveAndFlush(first);

        // Legitimate later rescreening for the same citizen/campaign must succeed
        ScreeningSubmission rescreened = submissionRepository.saveAndFlush(activeSubmission(7004L, 8004L));
        assertNotNull(rescreened.getId());

        List<ScreeningSubmission> rows = submissionRepository.findByCitizenIdAndCampaignId(7004L, 8004L);
        assertEquals(2, rows.size(), "Terminal row plus the new active row coexist");
        long activeCount = submissionRepository.findActiveSubmission(7004L, 8004L,
                List.of(ScreeningSubmissionStatus.DRAFT,
                        ScreeningSubmissionStatus.PENDING_REVIEW,
                        ScreeningSubmissionStatus.CORRECTIONS_REQUIRED)).isPresent() ? 1 : 0;
        assertEquals(1, activeCount, "Exactly one active submission after rescreening");
    }

    @Test
    void sameActiveSubmission_differentCampaign_isAllowed() {
        seedReferenceData();
        submissionRepository.saveAndFlush(activeSubmission(7005L, 8005L));
        ScreeningSubmission otherCampaign = submissionRepository.saveAndFlush(activeSubmission(7005L, 8006L));
        assertNotNull(otherCampaign.getId());
    }
}
