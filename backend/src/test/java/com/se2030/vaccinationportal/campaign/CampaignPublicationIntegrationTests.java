package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;

import java.time.LocalDate;
import java.time.Instant;
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
class CampaignPublicationIntegrationTests {

    @TestConfiguration
    static class PublicationTestConfig {
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
    }

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private CampaignQuestionnaireAssignmentRepository questionnaireAssignmentRepository;
    @Autowired private QuestionnaireVersionPublicationPort publicationPort;

    private VaccineType activeVaccineType;
    private TargetGroup validTargetGroup;
    private TestQuestionnairePublicationPort testPort;

    @BeforeEach
    void setUp() {
        testPort = (TestQuestionnairePublicationPort) publicationPort;

        activeVaccineType = vaccineTypeRepository.findByCode("VAX-PUB-01")
                .orElseGet(() -> vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-PUB-01", "Publication Vaccine", "dose", true)));

        validTargetGroup = targetGroupRepository.findByCode("TG-PUB-01")
                .orElseGet(() -> targetGroupRepository.saveAndFlush(new TargetGroup("TG-PUB-01", "Valid Adult Target Group Description", true)));
    }

    private VaccinationCampaign createBaseCampaign(CampaignStatus status) {
        LocalDate today = LocalDate.now();
        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-PUB-" + System.nanoTime(), "Test Pub Campaign", "Desc",
                activeVaccineType, validTargetGroup,
                today, today.plusDays(30),
                today.atStartOfDay(ZoneId.of("UTC")).toInstant(),
                today.plusDays(30).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                status
        ));
        return campaign;
    }

    private void addDoseProgramme(VaccinationCampaign campaign) {
        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1,2", 21, false, 10,
                campaign.getServiceStartDate(), null
        ));
    }

    private void addQuestionnaireAssignment(VaccinationCampaign campaign, Long versionId) {
        testPort.publishVersion(versionId);
        questionnaireAssignmentRepository.saveAndFlush(new CampaignQuestionnaireAssignment(
                campaign, versionId, campaign.getServiceStartDate(), null
        ));
    }

    @Test
    @DisplayName("Valid DRAFT publication succeeds")
    void validDraftPublication_succeeds() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 101L);

        VaccinationCampaign published = campaignService.publishCampaign(campaign.getId());
        assertEquals(CampaignStatus.PUBLISHED, published.getStatus());
    }

    @Test
    @DisplayName("Valid SCHEDULED publication succeeds")
    void validScheduledPublication_succeeds() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.SCHEDULED);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 102L);

        VaccinationCampaign published = campaignService.publishCampaign(campaign.getId());
        assertEquals(CampaignStatus.PUBLISHED, published.getStatus());
    }

    @Test
    @DisplayName("Already PUBLISHED campaign publication rejected")
    void alreadyPublished_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.PUBLISHED);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 103L);

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
        assertEquals(CampaignStatus.PUBLISHED,
                campaignRepository.findById(campaign.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Invalid source statuses rejected")
    void invalidSourceStatuses_rejected() {
        for (CampaignStatus status : new CampaignStatus[]{CampaignStatus.ACTIVE, CampaignStatus.COMPLETED, CampaignStatus.CANCELLED, CampaignStatus.SUSPENDED, CampaignStatus.ARCHIVED}) {
            VaccinationCampaign campaign = createBaseCampaign(status);
            addDoseProgramme(campaign);
            addQuestionnaireAssignment(campaign, 104L);

            assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
            assertEquals(status, campaignRepository.findById(campaign.getId()).orElseThrow().getStatus());
        }
    }

    @Test
    @DisplayName("Missing VaccineType rejected")
    void missingVaccineType_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 105L);

        // Deactivate vaccine type
        activeVaccineType.setActive(false);
        vaccineTypeRepository.saveAndFlush(activeVaccineType);

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));

        activeVaccineType.setActive(true);
        vaccineTypeRepository.saveAndFlush(activeVaccineType);
    }

    @Test
    @DisplayName("Blank TargetGroup description rejected")
    void blankTargetGroupDescription_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 106L);

        validTargetGroup.setDescription("   ");

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));

        validTargetGroup.setDescription("Valid Adult Target Group Description");
    }

    @Test
    @DisplayName("Zero dose programmes rejected")
    void zeroDoseProgrammes_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addQuestionnaireAssignment(campaign, 107L);

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
    }

    @Test
    @DisplayName("Multiple effective dose programmes rejected")
    void multipleEffectiveDoseProgrammes_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        // Add second overlapping effective dose programme directly
        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 2, DoseCategory.BOOSTER, "3", 180, false, 10,
                campaign.getServiceStartDate(), null
        ));
        addQuestionnaireAssignment(campaign, 108L);

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
    }

    @Test
    @DisplayName("Zero questionnaire assignments rejected")
    void zeroQuestionnaireAssignments_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
    }

    @Test
    @DisplayName("Multiple effective questionnaire assignments rejected")
    void multipleEffectiveQuestionnaireAssignments_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 109L);
        // Add second overlapping assignment directly
        testPort.publishVersion(110L);
        questionnaireAssignmentRepository.saveAndFlush(new CampaignQuestionnaireAssignment(
                campaign, 110L, campaign.getServiceStartDate(), null
        ));

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
    }

    @Test
    @DisplayName("Unpublished questionnaire rejected")
    void unpublishedQuestionnaire_rejected() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        questionnaireAssignmentRepository.saveAndFlush(new CampaignQuestionnaireAssignment(
                campaign, 999L, campaign.getServiceStartDate(), null
        ));
        testPort.unpublishVersion(999L);

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));
    }

    @Test
    @DisplayName("Valid published questionnaire allowed")
    void validPublishedQuestionnaire_allowed() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 111L);

        assertDoesNotThrow(() -> campaignService.publishCampaign(campaign.getId()));
    }

    @Test
    @DisplayName("Location target, center assignment, recurrence rule, and session NOT required for publication")
    void nonPrerequisitesNotRequired() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        addQuestionnaireAssignment(campaign, 112L);

        // No location targets, no center assignments, no recurrence rules, no sessions exist for campaign
        VaccinationCampaign published = campaignService.publishCampaign(campaign.getId());
        assertEquals(CampaignStatus.PUBLISHED, published.getStatus());
    }

    @Test
    @DisplayName("Failed validation leaves campaign status unchanged")
    void failedValidationLeavesStatusUnchanged() {
        VaccinationCampaign campaign = createBaseCampaign(CampaignStatus.DRAFT);
        addDoseProgramme(campaign);
        // Missing questionnaire assignment

        assertThrows(IllegalStateException.class, () -> campaignService.publishCampaign(campaign.getId()));

        VaccinationCampaign reloaded = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(CampaignStatus.DRAFT, reloaded.getStatus());
    }
}
