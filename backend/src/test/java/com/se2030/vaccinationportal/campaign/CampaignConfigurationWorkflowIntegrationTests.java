package com.se2030.vaccinationportal.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignCenterAssignmentService;
import com.se2030.vaccinationportal.campaign.service.CampaignDoseProgrammeService;
import com.se2030.vaccinationportal.campaign.service.CampaignLocationTargetService;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignConfigurationWorkflowIntegrationTests {

    @TestConfiguration
    static class WorkflowTestConfig {
        @Bean
        @Primary
        public QuestionnaireVersionPublicationPort testPublicationPort() {
            return new TestQuestionnairePublicationPort();
        }
    }

    public static class TestQuestionnairePublicationPort implements QuestionnaireVersionPublicationPort {
        private final Set<Long> publishedIds = new HashSet<>();

        public void publish(Long id) {
            publishedIds.add(id);
        }

        @Override
        public boolean isPublished(Long questionnaireVersionId) {
            return publishedIds.contains(questionnaireVersionId);
        }
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private CampaignDoseProgrammeService doseProgrammeService;
    @Autowired private CampaignQuestionnaireAssignmentService questionnaireAssignmentService;
    @Autowired private CampaignCenterAssignmentService centerAssignmentService;
    @Autowired private CampaignLocationTargetService locationTargetService;
    @Autowired private VaccinationSessionService sessionService;

    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private QuestionnaireVersionPublicationPort publicationPort;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.save(new VaccineType("COVID-19 mRNA", "mRNA Vaccine"));
        targetGroup = targetGroupRepository.save(new TargetGroup("ADULTS-18-59", "Adult population 18-59"));

        center = new VaccinationCenter(
                "CC-001",
                "Colombo Central Vaccination Clinic",
                "100 Galle Road, Colombo",
                "colombo@vaccines.lk",
                CenterStatus.ACTIVE,
                50,
                100,
                "Asia/Colombo"
        );
        center = centerRepository.save(center);

        ((TestQuestionnairePublicationPort) publicationPort).publish(501L);
    }

    @Test
    @DisplayName("End-to-end Campaign Configuration workflow: configure dose, questionnaire, center, location -> schedule campaign -> verify session operational guard")
    void fullCampaignConfigurationWorkflow() {
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = startDate.plusMonths(3);
        Instant bookingOpen = startDate.minusDays(5).atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant bookingClose = endDate.minusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        // 1. Create Campaign in DRAFT status
        VaccinationCampaign campaign = campaignService.createCampaign(
                "COV-NAT-2026",
                "National Adult COVID Campaign 2026",
                "National rollout for adult population",
                vaccineType.getId(),
                targetGroup.getId(),
                startDate,
                endDate,
                bookingOpen,
                bookingClose
        );
        Long campaignId = campaign.getId();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);

        // Verify scheduling fails before configuration is established
        assertThatThrownBy(() -> campaignService.scheduleCampaign(campaignId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dose programme");

        // 2. Establish Initial Dose Programme
        CampaignDoseProgramme doseProg = doseProgrammeService.createInitialProgramme(
                campaignId,
                DoseCategory.PRIMARY,
                "1,2",
                28,
                false,
                15,
                startDate
        );
        assertThat(doseProg.getVersionNumber()).isEqualTo(1);

        // 3. Assign Published Questionnaire Version
        CampaignQuestionnaireAssignment questAssign = questionnaireAssignmentService.assignQuestionnaireVersion(
                campaignId,
                501L,
                startDate
        );
        assertThat(questAssign.getQuestionnaireVersionId()).isEqualTo(501L);

        // 4. Assign Active Center
        CampaignCenterAssignment centerAssign = centerAssignmentService.createAssignment(
                campaignId,
                center.getId(),
                startDate,
                endDate
        );
        assertThat(centerAssign.getCenter().getId()).isEqualTo(center.getId());

        // 5. Add Administrative Location Targets
        CampaignLocationTarget locTarget = locationTargetService.addTarget(
                campaignId,
                LocationTargetType.DISTRICT,
                "COLOMBO"
        );
        assertThat(locTarget.getTargetCode()).isEqualTo("COLOMBO");

        // 6. Verify Read-back of all configuration
        List<CampaignDoseProgramme> doseHistory = doseProgrammeService.getProgrammeHistory(campaignId);
        assertThat(doseHistory).hasSize(1);

        List<CampaignQuestionnaireAssignment> questHistory = questionnaireAssignmentService.getAssignmentHistory(campaignId);
        assertThat(questHistory).hasSize(1);

        List<CampaignCenterAssignment> centerAssignments = centerAssignmentService.getAssignmentsForCampaign(campaignId);
        assertThat(centerAssignments).hasSize(1);

        List<CampaignLocationTarget> locTargets = locationTargetService.getTargetsForCampaign(campaignId);
        assertThat(locTargets).hasSize(1);

        // 7. Satisfy Campaign Scheduling Prerequisites -> Campaign schedules successfully
        VaccinationCampaign scheduledCampaign = campaignService.scheduleCampaign(campaignId);
        assertThat(scheduledCampaign.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);

        // 8. Create a DRAFT Session under this configured campaign
        Instant sessionStart = startDate.plusDays(2).atTime(9, 0).atZone(ZoneId.of("Asia/Colombo")).toInstant();
        Instant sessionEnd = startDate.plusDays(2).atTime(17, 0).atZone(ZoneId.of("Asia/Colombo")).toInstant();
        Instant sessionBookingClose = sessionStart.minusSeconds(3600);

        VaccinationSession draftSession = new VaccinationSession(
                scheduledCampaign,
                center,
                null,
                null,
                null,
                sessionStart,
                sessionEnd,
                sessionBookingClose,
                "Asia/Colombo"
        );
        draftSession = sessionRepository.save(draftSession);
        assertThat(draftSession.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.DRAFT);

        // 9. Downstream Session Scheduling Guard: verify scheduling rejection for invalid / unconfigured session
        Long sessionId = draftSession.getId();
        assertThatThrownBy(() -> sessionService.scheduleSession(sessionId))
                .isInstanceOf(RuntimeException.class);

        VaccinationSession reloadedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(reloadedSession.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.DRAFT);
    }
}
