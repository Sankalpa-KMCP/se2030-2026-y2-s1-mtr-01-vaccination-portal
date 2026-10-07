package com.se2030.vaccinationportal.clinical;

import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.eligibility.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.screening.eligibility.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.questionnaire.model.*;
import com.se2030.vaccinationportal.screening.submission.model.*;
import com.se2030.vaccinationportal.screening.submission.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.*;
import com.se2030.vaccinationportal.vaccination.administration.model.*;
import com.se2030.vaccinationportal.vaccination.administration.repository.*;
import com.se2030.vaccinationportal.vaccination.administration.service.*;
import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.certificate.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the same forced lock-wait schedules on H2 and real MySQL through the tagged subclass. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(40)
public class ClinicalLifecycleConcurrencyIntegrationTests {
    @Autowired protected DataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    @Autowired ScreeningSubmissionRepository submissions;
    @Autowired EligibilityAssessmentRepository assessments;
    @Autowired EligibilityReviewService review;
    @Autowired VaccinationRecordRepository records;
    @Autowired VaccinationCorrectionRepository corrections;
    @Autowired VaccinationCorrectionService correctionService;
    @Autowired VaccinationCorrectionLifecycleService lifecycle;
    @Autowired DigitalCertificateService certificateService;
    @Autowired DigitalCertificateRepository certificates;
    @Autowired EffectiveVaccinationRecordService effectiveRecords;
    @Autowired MockMvc mvc;
    private final List<Long> recordIds = new ArrayList<>();
    private final List<Long> submissionIds = new ArrayList<>();

    @AfterEach
    void removeOwnFixtures() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (Long id : recordIds) {
                corrections.deleteAll(corrections.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(id));
                certificates.deleteAll(certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(id));
                records.deleteById(id);
            }
            for (Long id : submissionIds) {
                var submission = submissions.findById(id).orElseThrow();
                var version = submission.getQuestionnaireVersion();
                var questionnaire = version.getQuestionnaire();
                var notice = submission.getConsentNoticeVersion();
                entityManager.remove(submission);
                entityManager.flush();
                entityManager.remove(version);
                entityManager.remove(questionnaire);
                entityManager.remove(notice);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void decisionAndCorrectionsCannotBothWin(boolean decisionWins) throws Exception {
        Long id = screening();
        Callable<?> decision = () -> review.recordDecision(id, 42L, EligibilityAssessmentStatus.ELIGIBLE,
                "Reviewed", null, Instant.now().plusSeconds(86400));
        Callable<?> correction = () -> review.requestCorrections(id, "Clarify answer");
        Throwable loser = compete(() -> submissions.findByIdForUpdate(id).orElseThrow(),
                decisionWins ? decision : correction, decisionWins ? correction : decision);
        assertInstanceOf(IllegalStateException.class, loser);
        assertTrue(loser.getMessage().contains("PENDING_REVIEW"));
        assertEquals(decisionWins ? ScreeningSubmissionStatus.REVIEWED : ScreeningSubmissionStatus.CORRECTIONS_REQUIRED,
                submissions.findById(id).orElseThrow().getStatus());
        assertEquals(decisionWins, assessments.findBySubmissionId(id).isPresent());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void applicationAndRejectionKeepEffectiveRecordAndCertificateConsistent(boolean applicationWins) throws Exception {
        Long recordId = record();
        Long originalId = certificateService.issueCertificateForVaccinationRecord(recordId).getId();
        Long correctionId = correctionService.createCitizenCorrection(recordId, request(), 41L).getId();
        correctionService.approveCorrection(recordId, correctionId, 42L, "Confirmed");
        Callable<?> application = () -> lifecycle.applyApprovedCorrection(recordId, correctionId, 43L);
        Callable<?> rejection = () -> correctionService.rejectCorrection(recordId, correctionId, 42L, "Rejected");
        Throwable loser = compete(() -> records.findByIdForUpdate(recordId).orElseThrow(),
                applicationWins ? application : rejection, applicationWins ? rejection : application);
        assertInstanceOf(InvalidVaccinationCorrectionException.class, loser);
        assertEquals(applicationWins ? VaccinationCorrectionStatus.APPLIED : VaccinationCorrectionStatus.REJECTED,
                corrections.findById(correctionId).orElseThrow().getStatus());
        int dose = applicationWins ? 2 : 1;
        assertEquals(dose, effectiveRecords.getEffectiveRecord(recordId).doseNumber());
        var history = certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(recordId);
        assertEquals(applicationWins ? 2 : 1, history.size());
        var valid = history.stream().filter(c -> c.getStatus() == CertificateStatus.VALID).toList();
        assertEquals(1, valid.size());
        assertEquals(dose, valid.get(0).getIssuedDoseNumber());
        assertEquals(applicationWins ? CertificateStatus.SUPERSEDED : CertificateStatus.VALID,
                certificates.findById(originalId).orElseThrow().getStatus());
    }

    @Test
    void concurrentCitizenCreationPersistsOnlyOneActiveRequest() throws Exception {
        Long id = record();
        Throwable loser = compete(() -> records.findByIdForUpdate(id).orElseThrow(),
                () -> correctionService.createCitizenCorrection(id, request(), 41L),
                () -> correctionService.createCitizenCorrection(id, request(), 41L));
        assertInstanceOf(DuplicateVaccinationCorrectionException.class, loser);
        var history = corrections.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(id);
        assertEquals(1, history.size());
        assertEquals(VaccinationCorrectionStatus.PENDING, history.get(0).getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void manualReissueAndApplicationShareRecordGuard(boolean applicationWins) throws Exception {
        Long id = record();
        certificateService.issueCertificateForVaccinationRecord(id);
        Long correctionId = correctionService.createCitizenCorrection(id, request(), 41L).getId();
        correctionService.approveCorrection(id, correctionId, 42L, "Confirmed");
        Callable<?> application = () -> lifecycle.applyApprovedCorrection(id, correctionId, 43L);
        Callable<?> reissue = () -> certificateService.reissueCertificate(id, "Manual replacement", 42L);
        // These operations may both succeed sequentially, but the second must see the first's state.
        runCompetition(() -> records.findByIdForUpdate(id).orElseThrow(),
                applicationWins ? application : reissue, applicationWins ? reissue : application, false);
        assertEquals(VaccinationCorrectionStatus.APPLIED, corrections.findById(correctionId).orElseThrow().getStatus());
        assertEquals(2, effectiveRecords.getEffectiveRecord(id).doseNumber());
        var valid = certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(id).stream()
                .filter(c -> c.getStatus() == CertificateStatus.VALID).toList();
        assertEquals(1, valid.size());
        assertEquals(2, valid.get(0).getIssuedDoseNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HEALTHCARE_OFFICER", "ADMIN"})
    void rescreeningPersistsAuthenticatedActorDespiteSpoofedBody(String role) throws Exception {
        Long id = screening();
        Long assessmentId = review.recordDecision(id, 42L, EligibilityAssessmentStatus.INELIGIBLE,
                "Review required", null, null).getId();
        mvc.perform(post("/api/v1/screening/assessments/{id}/rescreening", assessmentId)
                .with(authentication(actor(42L, role)))
                .contentType("application/json")
                .content("{\"authorizedByReviewerId\":999999,\"reason\":\"New clinical evidence\"}"))
                .andExpect(status().isOk());
        assertEquals(42L, assessments.findById(assessmentId).orElseThrow().getRescreeningAuthorizedBy());
    }

    @Test
    void citizenCannotAuthorizeRescreening() throws Exception {
        Long id = screening();
        Long assessmentId = review.recordDecision(id, 42L, EligibilityAssessmentStatus.INELIGIBLE,
                "Review required", null, null).getId();
        mvc.perform(post("/api/v1/screening/assessments/{id}/rescreening", assessmentId)
                .with(authentication(actor(41L, "CITIZEN")))
                .contentType("application/json")
                .content("{\"authorizedByReviewerId\":42,\"reason\":\"Spoof\"}"))
                .andExpect(status().isForbidden());
        assertNull(assessments.findById(assessmentId).orElseThrow().getRescreeningAuthorizedBy());
    }

    private UsernamePasswordAuthenticationToken actor(Long id, String role) {
        var principal = UserPrincipal.fromClaims(id, "clinical@example.test", List.of("ROLE_" + role));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private CreateVaccinationCorrectionRequest request() {
        return new CreateVaccinationCorrectionRequest("doseNumber", "2", "Correct dose", 41L);
    }

    private Long record() {
        Long id = records.saveAndFlush(new VaccinationRecord(Math.abs(UUID.randomUUID().getMostSignificantBits()),
                41L, 51L, 61L, 43L, 71L, 1, Instant.parse("2026-09-01T08:00:00Z"),
                null, "Original", Instant.parse("2026-09-01T08:05:00Z"))).getId();
        recordIds.add(id);
        return id;
    }

    private Long screening() {
        Long id = new TransactionTemplate(transactionManager).execute(status -> {
            var questionnaire = new Questionnaire("CL-" + UUID.randomUUID(), "Clinical", "Test");
            entityManager.persist(questionnaire);
            var version = new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.PUBLISHED);
            entityManager.persist(version);
            var notice = new ConsentNoticeVersion("CL-" + UUID.randomUUID(), 1, "Consent", "Test", "hash", Instant.now());
            entityManager.persist(notice);
            var submission = new ScreeningSubmission(Math.abs(UUID.randomUUID().getMostSignificantBits()), 51L,
                    version, notice, Instant.now());
            submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
            submission.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_PASS);
            return submissions.saveAndFlush(submission).getId();
        });
        submissionIds.add(id);
        return id;
    }

    /** Winner holds the production guard; loser must be observed waiting in the database before release. */
    private Throwable compete(Callable<?> guard, Callable<?> winner, Callable<?> loser) throws Exception {
        return runCompetition(guard, winner, loser, true);
    }

    private Throwable runCompetition(Callable<?> guard, Callable<?> winner, Callable<?> loser,
                                     boolean expectRejection) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                call(guard);
                locked.countDown();
                await(release);
                return call(winner);
            }));
            assertTrue(locked.await(10, TimeUnit.SECONDS), "Winner must hold guard");
            Future<Throwable> second = pool.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).execute(status -> {
                        started.countDown();
                        return call(loser);
                    });
                    return null;
                } catch (RuntimeException ex) { return ex; }
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));
            assertDatabaseLockWait();
            assertFalse(second.isDone(), "Contender must be blocked before winner transitions");
            release.countDown();
            first.get(15, TimeUnit.SECONDS);
            Throwable error = second.get(15, TimeUnit.SECONDS);
            if (expectRejection) assertNotNull(error, "Exactly one conflicting operation must succeed");
            else assertNull(error, "Compatible operation must succeed after revalidating winner's state");
            return error;
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    protected Connection observerConnection() throws Exception { return dataSource.getConnection(); }

    private void assertDatabaseLockWait() throws Exception {
        try (Connection connection = observerConnection(); var statement = connection.createStatement()) {
            String sql = connection.getMetaData().getDatabaseProductName().equals("MySQL")
                    ? "SELECT COUNT(*) FROM performance_schema.data_lock_waits"
                    : "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_STATE = 'BLOCKED'";
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            do {
                try (var result = statement.executeQuery(sql)) {
                    result.next();
                    if (result.getInt(1) > 0) return;
                }
                Thread.onSpinWait();
            } while (System.nanoTime() < deadline);
            fail("Competing service must actually wait for the database guard");
        }
    }

    private static Object call(Callable<?> action) {
        try { return action.call(); }
        catch (RuntimeException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(15, TimeUnit.SECONDS)); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
    }
}
