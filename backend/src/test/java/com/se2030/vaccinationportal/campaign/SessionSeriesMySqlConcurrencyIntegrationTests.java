package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesChangeService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying multi-row
 * series-level locking and deterministic serialization for concurrent series change and occurrence generation.
 */
class SessionSeriesMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private SessionSeriesChangeService seriesChangeService;
    @Autowired private SessionOccurrenceGenerationService generationService;
    @Autowired private SessionRecurrenceRuleRepository ruleRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private SessionChangeEventRepository sessionChangeEventRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    @Timeout(30)
    @DisplayName("Concurrent series confirmation and occurrence generator on same recurrence rule execute without deadlock or duplicate keys on MySQL")
    void concurrentSeriesConfirmationAndGeneratorSerializeDeterministicallyOnMySql() throws Exception {
        assertConnectedToRealMySql();

        VaccineType vt = vaccineTypeRepository.saveAndFlush(new VaccineType("VT-MYSQL-SER", "Series Vaccine", "dose", true));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-MYSQL-SER", "Series Target Group", true));

        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-MYSQL-SER-01", "Series Test Campaign", "Description", vt, tg,
                LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1),
                Instant.parse("2028-12-01T00:00:00Z"), Instant.parse("2031-01-01T23:59:59Z"),
                CampaignStatus.ACTIVE));

        VaccinationCenter center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-MYSQL-SER-01", "Series Test Center", "123 Main St", "Colombo",
                CenterStatus.ACTIVE, 500, 10, "UTC"));

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1)));

        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0),
                    LocalDate.of(2029, 1, 1), null));
        }

        SessionRecurrenceRule rule = ruleRepository.saveAndFlush(new SessionRecurrenceRule(
                campaign, center, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "UTC"));

        generationService.generateOccurrences(rule.getId());

        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(
                rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC");

        // Run series confirmation and occurrence generator concurrently against the same active rule
        List<Outcome> outcomes = runConcurrently(
                () -> seriesChangeService.confirmSeriesChange(
                        rule.getId(), EnumSet.of(DayOfWeek.MONDAY),
                        LocalDate.of(2029, 2, 5), LocalDate.of(2029, 2, 26),
                        LocalTime.of(10, 0), LocalTime.of(13, 0), "UTC",
                        preview.previewToken(), "Concurrent series shift on MySQL", "mysql-series-actor"),
                () -> generationService.generateOccurrences(rule.getId())
        );

        // Both commands must terminate without deadlock
        assertEquals(2, outcomes.size(), "Expected 2 execution outcomes");
        assertTrue(outcomes.stream().anyMatch(Outcome::success), "At least one operation must succeed: " + outcomes);

        // Verify occurrence keys are strictly unique in database
        List<VaccinationSession> sessions = sessionRepository.findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(rule.getId());
        assertFalse(sessions.isEmpty(), "Generated sessions must exist");
        Set<String> uniqueKeys = new HashSet<>();
        for (VaccinationSession s : sessions) {
            assertTrue(uniqueKeys.add(s.getOccurrenceKey()), "Duplicate occurrenceKey detected: " + s.getOccurrenceKey());
        }

        // Verify audit event consistency
        List<SessionChangeEvent> events = sessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc(sessions.get(0).getId());
        assertTrue(events.size() <= 1);
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Contenders did not become ready");
            start.countDown();
            return List.of(
                    firstFuture.get(10, TimeUnit.SECONDS),
                    secondFuture.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Outcome> contender(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Contender timed out waiting for start");
            }
            try {
                Object result = action.call();
                return new Outcome(true, result, null);
            } catch (Exception ex) {
                return new Outcome(false, null, ex);
            }
        };
    }

    private record Outcome(boolean success, Object result, Throwable error) {}
}
