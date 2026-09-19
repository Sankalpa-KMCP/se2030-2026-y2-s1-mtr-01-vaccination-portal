package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignSchedulingIntegrationTests {

    @TestConfiguration
    static class SchedulingTestConfig {
        @Bean
        @Primary
        public QuestionnaireVersionPublicationPort testPublicationPort() {
            return new TestQuestionnairePublicationPort();
        }
    }

    public static class TestQuestionnairePublicationPort implements QuestionnaireVersionPublicationPort {
        private final Set<Long> publishedIds = new HashSet<>();

        public void publishVersion(Long id) {
            publishedIds.add(id);
        }

        public void unpublishVersion(Long id) {
            publishedIds.remove(id);
        }

        @Override
        public boolean isPublished(Long questionnaireVersionId) {
            return publishedIds.contains(questionnaireVersionId);
        }

        @Override
        public boolean hasCampaignSubmissions(Long campaignId) {
            return false;
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    @Autowired private CampaignCenterAssignmentRepository centerAssignmentRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private QuestionnaireVersionPublicationPort publicationPort;
    @Autowired private Clock clock;

    private VaccineType activeVaccineType;
    private VaccineType inactiveVaccineType;
    private TargetGroup activeTargetGroup;
    private TargetGroup inactiveTargetGroup;
    private VaccinationCenter activeCenterA;
    private VaccinationCenter activeCenterB;
    private VaccinationCenter inactiveCenter;
    private TestQuestionnairePublicationPort testPort;

    @BeforeEach
    void setUp() {
        testPort = (TestQuestionnairePublicationPort) publicationPort;
        testPort.publishVersion(501L);

        activeVaccineType = vaccineTypeRepository.findByCode("VAX-SCHED-01")
                .orElseGet(() -> vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SCHED-01", "Sched Active Vaccine", "dose", true)));

        inactiveVaccineType = vaccineTypeRepository.findByCode("VAX-SCHED-INACT")
                .orElseGet(() -> vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SCHED-INACT", "Sched Inactive Vaccine", "dose", false)));

        activeTargetGroup = targetGroupRepository.findByCode("TG-SCHED-01")
                .orElseGet(() -> targetGroupRepository.saveAndFlush(new TargetGroup("TG-SCHED-01", "Active Target Group Description", true)));

        inactiveTargetGroup = targetGroupRepository.findByCode("TG-SCHED-INACT")
                .orElseGet(() -> targetGroupRepository.saveAndFlush(new TargetGroup("TG-SCHED-INACT", "Inactive Target Group Description", false)));

        activeCenterA = centerRepository.findByCode("CTR-SCHED-01")
                .orElseGet(() -> centerRepository.saveAndFlush(new VaccinationCenter(
                        "CTR-SCHED-01", "Center Alpha", "123 Main St", "contact@alpha.org",
                        CenterStatus.ACTIVE, 100, null, "UTC"
                )));

        activeCenterB = centerRepository.findByCode("CTR-SCHED-02")
                .orElseGet(() -> centerRepository.saveAndFlush(new VaccinationCenter(
                        "CTR-SCHED-02", "Center Beta", "456 Oak St", "contact@beta.org",
                        CenterStatus.ACTIVE, 150, null, "UTC"
                )));

        inactiveCenter = centerRepository.findByCode("CTR-SCHED-INACT")
                .orElseGet(() -> centerRepository.saveAndFlush(new VaccinationCenter(
                        "CTR-SCHED-INACT", "Center Inactive", "789 Pine St", "contact@inact.org",
                        CenterStatus.INACTIVE, 50, null, "UTC"
                )));
    }

    private VaccinationCampaign createBaseCampaign(LocalDate serviceStartDate, LocalDate serviceEndDate, CampaignStatus status, VaccineType vax, TargetGroup tg) {
        Instant bookingOpen = serviceStartDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant bookingClose = serviceEndDate.atStartOfDay(ZoneId.of("UTC")).toInstant();

        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-SCHED-" + System.nanoTime(), "Test Sched Campaign", "Desc",
                vax != null ? vax : activeVaccineType,
                tg != null ? tg : activeTargetGroup,
                serviceStartDate, serviceEndDate,
                bookingOpen, bookingClose,
                status
        ));
    }

    private void addDoseProgramme(VaccinationCampaign campaign, LocalDate activeFrom, LocalDate activeTo) {
        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1,2", 21, false, 10,
                activeFrom, activeTo
        ));
    }

    private void addQuestionnaireAssignment(VaccinationCampaign campaign, Long versionId, LocalDate activeFrom, LocalDate activeTo) {
        if (versionId != null) {
            testPort.publishVersion(versionId);
        }
        questionnaireAssignmentRepository.saveAndFlush(new CampaignQuestionnaireAssignment(
                campaign, versionId, activeFrom, activeTo
        ));
    }

    private VaccinationCampaign createValidDraftCampaign(int startDaysInFuture, int durationDays) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(startDaysInFuture);
        LocalDate end = start.plusDays(durationDays);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);

        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, activeCenterA, start, null
        ));

        return campaign;
    }

    @Test
    @DisplayName("Valid DRAFT campaign with all prerequisites successfully transitions to SCHEDULED")
    void validDraftCampaignSucceeds() {
        VaccinationCampaign campaign = createValidDraftCampaign(7, 30);

        VaccinationCampaign result = campaignService.scheduleCampaign(campaign.getId());

        assertNotNull(result);
        assertEquals(CampaignStatus.SCHEDULED, result.getStatus());

        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.SCHEDULED, persisted.getStatus());
    }

    @Test
    @DisplayName("scheduleCampaign rejects non-DRAFT campaigns")
    void nonDraftCampaignRejects() {
        VaccinationCampaign campaign = createValidDraftCampaign(7, 30);
        CampaignTestFixtures.forceStatus(campaign, CampaignStatus.PUBLISHED);
        campaignRepository.saveAndFlush(campaign);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot schedule campaign in PUBLISHED status"));
        assertTrue(ex.getMessage().contains("Only DRAFT campaigns can be scheduled"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects same-day service start date (serviceStartDate == today UTC)")
    void sameDayServiceStartRejects() {
        LocalDate today = LocalDate.now(clock);
        VaccinationCampaign campaign = createBaseCampaign(today, today.plusDays(30), CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);

        addDoseProgramme(campaign, today, null);
        addQuestionnaireAssignment(campaign, 501L, today, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, today, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("must be strictly in the future for scheduling approval"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects past service start date")
    void pastServiceStartRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate pastDate = today.minusDays(5);
        VaccinationCampaign campaign = createBaseCampaign(pastDate, today.plusDays(30), CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);

        addDoseProgramme(campaign, pastDate, null);
        addQuestionnaireAssignment(campaign, 501L, pastDate, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, pastDate, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("must be strictly in the future for scheduling approval"));
    }

    @Test
    @DisplayName("scheduleCampaign succeeds when service start date is tomorrow (strictly in future)")
    void tomorrowServiceStartSucceeds() {
        VaccinationCampaign campaign = createValidDraftCampaign(1, 30);

        VaccinationCampaign result = campaignService.scheduleCampaign(campaign.getId());
        assertEquals(CampaignStatus.SCHEDULED, result.getStatus());
    }

    @Test
    @DisplayName("scheduleCampaign rejects inactive VaccineType")
    void inactiveVaccineTypeRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, inactiveVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires an active VaccineType"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects inactive TargetGroup")
    void inactiveTargetGroupRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, inactiveTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires an active TargetGroup"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when no effective CampaignDoseProgramme exists at serviceStartDate")
    void missingDoseProgrammeRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        // Dose programme effective only in the future after service start
        addDoseProgramme(campaign, start.plusDays(10), null);
        addQuestionnaireAssignment(campaign, 501L, start, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires exactly one effective dose programme for scheduling approval; found 0"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when multiple effective CampaignDoseProgrammes exist at serviceStartDate")
    void multipleEffectiveDoseProgrammesRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(campaign, 1, DoseCategory.PRIMARY, "1,2", 28, false, 10, start.minusDays(5), null));
        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(campaign, 2, DoseCategory.BOOSTER, "3", 0, false, 10, start.minusDays(2), null));
        addQuestionnaireAssignment(campaign, 501L, start, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires exactly one effective dose programme for scheduling approval; found 2"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when no effective CampaignQuestionnaireAssignment exists at serviceStartDate")
    void missingQuestionnaireAssignmentRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        // Assignment effective after service start
        addQuestionnaireAssignment(campaign, 501L, start.plusDays(5), null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires exactly one effective questionnaire assignment for scheduling approval; found 0"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when assigned QuestionnaireVersion is NOT published in MF-01")
    void unpublishedQuestionnaireVersionRejectsScheduling() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 999L, start, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, null));

        // Ensure version 999L is NOT published in MF-01 port
        testPort.unpublishVersion(999L);
        assertFalse(publicationPort.isPublished(999L));

        // 1. Scheduling fails deterministically
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("is not in PUBLISHED status in MF-01"));

        // 2. Campaign remains in DRAFT status
        VaccinationCampaign unchanged = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.DRAFT, unchanged.getStatus());

        // 3. Once published in MF-01, scheduling succeeds
        testPort.publishVersion(999L);
        VaccinationCampaign scheduled = campaignService.scheduleCampaign(campaign.getId());
        assertEquals(CampaignStatus.SCHEDULED, scheduled.getStatus());
    }

    @Test
    @DisplayName("scheduleCampaign rejects when campaign has 0 center assignments")
    void zeroCenterAssignmentsRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires at least one active center assignment overlapping its service period"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when all center assignments are CANCELLED")
    void allCancelledCenterAssignmentsRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        CampaignCenterAssignment assignment = new CampaignCenterAssignment(campaign, activeCenterA, start, null);
        assignment.cancel();
        centerAssignmentRepository.saveAndFlush(assignment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires at least one active center assignment overlapping its service period"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when center assignment active period is entirely before campaign service period")
    void assignmentEntirelyBeforeServicePeriodRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        // Assignment ends exactly on start: activeTo = start (half-open [start-5, start) does not overlap [start, end])
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start.minusDays(5), start));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires at least one active center assignment overlapping its service period"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects when center assignment active period is entirely after campaign service period")
    void assignmentEntirelyAfterServicePeriodRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        // Assignment starts after campaign ends: activeFrom = end.plusDays(1)
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, end.plusDays(1), null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("requires at least one active center assignment overlapping its service period"));
    }

    @Test
    @DisplayName("scheduleCampaign succeeds when assignment starts exactly on campaign service end date")
    void assignmentStartingOnServiceEndDateSucceeds() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        // activeFrom == end overlaps the last service day
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, end, null));

        VaccinationCampaign result = campaignService.scheduleCampaign(campaign.getId());
        assertEquals(CampaignStatus.SCHEDULED, result.getStatus());
    }

    @Test
    @DisplayName("scheduleCampaign rejects when relevant overlapping assignment references an INACTIVE center")
    void relevantAssignmentWithInactiveCenterRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, inactiveCenter, start, null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("references an INACTIVE center"));
    }

    @Test
    @DisplayName("scheduleCampaign rejects in mixed case when one relevant assignment has an INACTIVE center")
    void mixedRelevantAssignmentsWithOneInactiveCenterRejects() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        // Assignment 1 to Active Center A
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, start.plusDays(10)));
        // Assignment 2 to Inactive Center
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, inactiveCenter, start.plusDays(10), null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(ex.getMessage().contains("references an INACTIVE center"));
    }

    @Test
    @DisplayName("Non-overlapping inactive center assignment does NOT block scheduling if relevant assignment is valid")
    void nonOverlappingInactiveCenterAssignmentDoesNotBlockScheduling() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(7);
        LocalDate end = start.plusDays(30);

        VaccinationCampaign campaign = createBaseCampaign(start, end, CampaignStatus.DRAFT, activeVaccineType, activeTargetGroup);
        addDoseProgramme(campaign, start, null);
        addQuestionnaireAssignment(campaign, 501L, start, null);

        // Overlapping valid assignment to active center
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, activeCenterA, start, end.plusDays(5)));

        // Non-overlapping assignment to inactive center (ended before campaign started)
        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, inactiveCenter, start.minusDays(20), start.minusDays(5)));

        VaccinationCampaign result = campaignService.scheduleCampaign(campaign.getId());
        assertEquals(CampaignStatus.SCHEDULED, result.getStatus());
    }
}
