package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignQuestionnaireAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying CampaignQuestionnaireAssignment persistence, effective-dated history,
 * overlap prevention, atomic version transition, and cross-module publication validation (WBVP-SE2030 / MF02-BR-02 / MF02-BR-17).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CampaignQuestionnaireAssignmentPersistenceIntegrationTests.TestPortConfig.class)
@Transactional
class CampaignQuestionnaireAssignmentPersistenceIntegrationTests {

    @TestConfiguration
    static class TestPortConfig {
        @Bean
        @Primary
        public TestQuestionnaireVersionPublicationPort testQuestionnaireVersionPublicationPort() {
            return new TestQuestionnaireVersionPublicationPort();
        }
    }

    /**
     * Test double simulating MF-01 QuestionnaireVersion publication status without creating duplicate entities.
     */
    static class TestQuestionnaireVersionPublicationPort implements QuestionnaireVersionPublicationPort {
        private final Set<Long> publishedVersions = new HashSet<>();

        public void setPublished(Long versionId, boolean published) {
            if (published) {
                publishedVersions.add(versionId);
            } else {
                publishedVersions.remove(versionId);
            }
        }

        public void clear() {
            publishedVersions.clear();
        }

        @Override
        public boolean isPublished(Long questionnaireVersionId) {
            return questionnaireVersionId != null && publishedVersions.contains(questionnaireVersionId);
        }
    }

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private CampaignQuestionnaireAssignmentRepository assignmentRepository;

    @Autowired
    private CampaignQuestionnaireAssignmentService assignmentService;

    @Autowired
    private TestQuestionnaireVersionPublicationPort testPublicationPort;

    private VaccinationCampaign campaignA;
    private VaccinationCampaign campaignB;

    @BeforeEach
    void setUp() {
        testPublicationPort.clear();
        // Register test published questionnaire versions in test port
        testPublicationPort.setPublished(100L, true);
        testPublicationPort.setPublished(101L, true);
        testPublicationPort.setPublished(102L, true);
        testPublicationPort.setPublished(200L, true);
        // Note: 999L remains UNPUBLISHED for negative tests

        VaccineType vt = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-MRNA-QA", "mRNA QA Test", "dose", true)
        );
        TargetGroup tg = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-ALL-QA", "General Population QA Test", true)
        );

        campaignA = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-QA-A",
                "Campaign A Questionnaire",
                "Campaign A description",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 15, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.DRAFT
        ));

        campaignB = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-QA-B",
                "Campaign B Questionnaire",
                "Campaign B description",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 15, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.PUBLISHED
        ));
    }

    @Test
    @DisplayName("Initial questionnaire assignment persists with open-ended activeTo")
    void initialAssignmentPersists() {
        CampaignQuestionnaireAssignment saved = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );

        assertNotNull(saved.getId());
        assertEquals(campaignA.getId(), saved.getCampaign().getId());
        assertEquals(100L, saved.getQuestionnaireVersionId());
        assertEquals(LocalDate.of(2026, 9, 1), saved.getActiveFrom());
        assertNull(saved.getActiveTo());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        assertEquals(1, assignmentRepository.countByCampaignId(campaignA.getId()));
    }

    @Test
    @DisplayName("Effective assignment lookup accurately resolves by evaluation date")
    void effectiveAssignmentLookupByDate() {
        assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );

        // Before activeFrom -> empty
        Optional<CampaignQuestionnaireAssignment> before = assignmentService.getEffectiveAssignmentAt(
                campaignA.getId(),
                LocalDate.of(2026, 8, 31)
        );
        assertTrue(before.isEmpty());

        // On activeFrom -> present
        Optional<CampaignQuestionnaireAssignment> onStart = assignmentService.getEffectiveAssignmentAt(
                campaignA.getId(),
                LocalDate.of(2026, 9, 1)
        );
        assertTrue(onStart.isPresent());
        assertEquals(100L, onStart.get().getQuestionnaireVersionId());

        // During open-ended future -> present
        Optional<CampaignQuestionnaireAssignment> inFuture = assignmentService.getEffectiveAssignmentAt(
                campaignA.getId(),
                LocalDate.of(2026, 10, 15)
        );
        assertTrue(inFuture.isPresent());
        assertEquals(100L, inFuture.get().getQuestionnaireVersionId());
    }

    @Test
    @DisplayName("Open-ended assignment query accurately returns open-ended record")
    void openEndedAssignmentQuery() {
        assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );

        Optional<CampaignQuestionnaireAssignment> openEnded = assignmentService.getOpenEndedAssignment(campaignA.getId());
        assertTrue(openEnded.isPresent());
        assertNull(openEnded.get().getActiveTo());
        assertEquals(100L, openEnded.get().getQuestionnaireVersionId());
    }

    @Test
    @DisplayName("New assignment version atomically closes previous record at effectiveStartDate minus 1 day")
    void newAssignmentClosesPreviousRecordAtomically() {
        // Initial assignment: activeFrom = 2026-09-01, activeTo = null
        CampaignQuestionnaireAssignment v1 = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );

        // New assignment: activeFrom = 2026-09-20, activeTo = null
        CampaignQuestionnaireAssignment v2 = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                101L,
                LocalDate.of(2026, 9, 20)
        );

        assertNotNull(v2.getId());
        assertEquals(101L, v2.getQuestionnaireVersionId());
        assertEquals(LocalDate.of(2026, 9, 20), v2.getActiveFrom());
        assertNull(v2.getActiveTo());

        // Verify previous record v1 is now closed at 2026-09-19
        CampaignQuestionnaireAssignment v1Reloaded = assignmentRepository.findById(v1.getId()).orElseThrow();
        assertEquals(LocalDate.of(2026, 9, 1), v1Reloaded.getActiveFrom());
        assertEquals(LocalDate.of(2026, 9, 19), v1Reloaded.getActiveTo());

        // Verify history contains both versions in order
        List<CampaignQuestionnaireAssignment> history = assignmentService.getAssignmentHistory(campaignA.getId());
        assertEquals(2, history.size());
        assertEquals(101L, history.get(0).getQuestionnaireVersionId());
        assertEquals(100L, history.get(1).getQuestionnaireVersionId());

        // Verify date resolution across boundary
        assertEquals(100L, assignmentService.getEffectiveAssignmentAt(campaignA.getId(), LocalDate.of(2026, 9, 19)).orElseThrow().getQuestionnaireVersionId());
        assertEquals(101L, assignmentService.getEffectiveAssignmentAt(campaignA.getId(), LocalDate.of(2026, 9, 20)).orElseThrow().getQuestionnaireVersionId());
    }

    @Test
    @DisplayName("New version with effective date on or before current version's activeFrom is rejected")
    void newVersionOnOrBeforeCurrentActiveFromRejected() {
        CampaignQuestionnaireAssignment v1 = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );

        // Same start date
        IllegalArgumentException exSame = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.assignQuestionnaireVersion(
                        campaignA.getId(),
                        101L,
                        LocalDate.of(2026, 9, 1)
                )
        );
        assertTrue(exSame.getMessage().contains("must be strictly after the current assignment activeFrom date"));

        // Prior start date
        IllegalArgumentException exBefore = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.assignQuestionnaireVersion(
                        campaignA.getId(),
                        101L,
                        LocalDate.of(2026, 8, 15)
                )
        );
        assertTrue(exBefore.getMessage().contains("must be strictly after the current assignment activeFrom date"));

        // Verify version 1 activeTo remains null (unmodified)
        CampaignQuestionnaireAssignment refreshedV1 = assignmentRepository.findById(v1.getId()).orElseThrow();
        assertNull(refreshedV1.getActiveTo());
    }

    @Test
    @DisplayName("Overlapping assignment range is rejected")
    void overlappingAssignmentRejected() {
        assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );

        // Direct repository overlap query test
        List<CampaignQuestionnaireAssignment> overlaps = assignmentRepository.findOverlappingAssignments(
                campaignA.getId(),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 25),
                null
        );
        assertFalse(overlaps.isEmpty(), "Overlapping query must detect overlap with open-ended version");
    }

    @Test
    @DisplayName("Port rejects assignment of UNPUBLISHED questionnaire version")
    void portRejectsUnpublishedQuestionnaireVersion() {
        // 999L is not published in test port
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.assignQuestionnaireVersion(
                        campaignA.getId(),
                        999L,
                        LocalDate.of(2026, 9, 1)
                )
        );
        assertTrue(ex.getMessage().contains("is not in PUBLISHED status in MF-01"));
        assertEquals(0, assignmentRepository.countByCampaignId(campaignA.getId()));
    }

    @Test
    @DisplayName("Port permits assignment of PUBLISHED questionnaire version")
    void portPermitsPublishedQuestionnaireVersion() {
        // 100L is registered as published in setUp
        CampaignQuestionnaireAssignment assignment = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );
        assertNotNull(assignment.getId());
        assertEquals(100L, assignment.getQuestionnaireVersionId());
    }

    @Test
    @DisplayName("Transaction rollback preserves previous open-ended assignment if new version creation fails")
    void transactionRollbackPreservesPreviousAssignmentOnFailure() {
        CampaignQuestionnaireAssignment v1 = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                100L,
                LocalDate.of(2026, 9, 1)
        );
        assertNull(v1.getActiveTo());

        // Attempt second version with an unpublished ID (999L), which will fail validation
        assertThrows(IllegalArgumentException.class, () ->
                assignmentService.assignQuestionnaireVersion(
                        campaignA.getId(),
                        999L,
                        LocalDate.of(2026, 9, 20)
                )
        );

        // Verify v1 is still open-ended (activeTo IS NULL)
        CampaignQuestionnaireAssignment reloaded = assignmentRepository.findById(v1.getId()).orElseThrow();
        assertNull(reloaded.getActiveTo(), "Failed version creation must preserve v1 activeTo as null");
    }

    @Test
    @DisplayName("Different campaigns can link to the same questionnaire version ID")
    void differentCampaignsCanLinkSameQuestionnaireVersion() {
        CampaignQuestionnaireAssignment assignA = assignmentService.assignQuestionnaireVersion(
                campaignA.getId(),
                200L,
                LocalDate.of(2026, 9, 1)
        );

        CampaignQuestionnaireAssignment assignB = assignmentService.assignQuestionnaireVersion(
                campaignB.getId(),
                200L,
                LocalDate.of(2026, 9, 1)
        );

        assertNotNull(assignA.getId());
        assertNotNull(assignB.getId());
        assertNotEquals(assignA.getId(), assignB.getId());

        List<CampaignQuestionnaireAssignment> byVersion = assignmentService.getAssignmentsForQuestionnaireVersion(200L);
        assertEquals(2, byVersion.size());
    }

    @Test
    @DisplayName("Assignment service exposes no hard-delete workflow and retains history")
    void assignmentsAreRetainedWithoutHardDelete() {
        assignmentService.assignQuestionnaireVersion(campaignA.getId(), 100L, LocalDate.of(2026, 9, 1));
        assignmentService.assignQuestionnaireVersion(campaignA.getId(), 101L, LocalDate.of(2026, 9, 15));

        List<CampaignQuestionnaireAssignment> history = assignmentService.getAssignmentHistory(campaignA.getId());
        assertEquals(2, history.size(), "Both versions must be retained permanently as historical records");
    }
}
