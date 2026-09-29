package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.service.QuestionnaireVersionPublicationPort;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormResponse;
import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.QuestionType;
import com.se2030.vaccinationportal.screening.model.Questionnaire;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.repository.ConsentNoticeVersionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningQuestionRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.screening.service.ScreeningConsentNoticeService;
import com.se2030.vaccinationportal.screening.service.ScreeningFormService;
import com.se2030.vaccinationportal.screening.service.ScreeningSubmissionService;
import com.se2030.vaccinationportal.screening.service.QuestionnaireVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CampaignScreeningCrossModuleIntegrationTests {

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private CampaignQuestionnaireAssignmentService assignmentService;
    @Autowired private QuestionnaireVersionPublicationPort publicationPort;
    @Autowired private ScreeningFormService screeningFormService;
    @Autowired private ScreeningSubmissionService screeningSubmissionService;
    @Autowired private QuestionnaireVersionService questionnaireVersionService;

    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private QuestionnaireRepository questionnaireRepository;
    @Autowired private QuestionnaireVersionRepository versionRepository;
    @Autowired private QuestionnaireItemRepository itemRepository;
    @Autowired private ScreeningQuestionRepository questionRepository;
    @Autowired private ConsentNoticeVersionRepository consentNoticeVersionRepository;
    @Autowired private ScreeningSubmissionRepository submissionRepository;
    @Autowired private Clock clock;
    @Autowired private CampaignScreeningReader campaignReader;
    @Autowired private ApplicationContext applicationContext;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private ConsentNoticeVersion consentNotice;

    @Test
    void campaignReaderPreservesMissingCampaignBehavior() {
        assertThrows(ResourceNotFoundException.class,
                () -> campaignReader.getRequiredCampaign(Long.MAX_VALUE));
        assertTrue(campaignReader.getEffectiveAssignmentAt(Long.MAX_VALUE, LocalDate.now(clock)).isEmpty());
        assertTrue(campaignReader.getEffectiveAssignmentAt(null, LocalDate.now(clock)).isEmpty());
    }

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("CROSS-VAX", "Cross Module Vaccine", "dose", true)
        );
        targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("CROSS-TG", "Cross Module Target Group", true)
        );

        consentNotice = consentNoticeVersionRepository.saveAndFlush(
                new ConsentNoticeVersion(
                        ScreeningConsentNoticeService.SCREENING_NOTICE_CODE,
                        1,
                        "Pre-Vaccination Screening Consent",
                        "Consent notice terms",
                        "hash-cross-module-test",
                        Instant.now(clock)
                )
        );
    }

    @Test
    @DisplayName("Complete assignment contract: Version B assigned to campaign is resolved for citizen screening, submitted, and permanently bound")
    void completeCampaignQuestionnaireAssignmentContract() {
        LocalDate today = LocalDate.now(clock);

        // 1. Create and persist two different PUBLISHED questionnaire versions: Version A and Version B
        Questionnaire questionnaire = questionnaireRepository.saveAndFlush(
                new Questionnaire("CROSS-Q", "Cross Module Clinical Questionnaire", "General screening")
        );

        QuestionnaireVersion versionA = versionRepository.saveAndFlush(
                new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.PUBLISHED)
        );
        ScreeningQuestion questionA = questionRepository.saveAndFlush(
                new ScreeningQuestion("Q-A", "Question for Version A", QuestionType.TEXT, true, true)
        );
        itemRepository.saveAndFlush(new QuestionnaireItem(versionA, questionA, 1, null));

        QuestionnaireVersion versionB = versionRepository.saveAndFlush(
                new QuestionnaireVersion(questionnaire, 2, QuestionnaireVersionStatus.PUBLISHED)
        );
        ScreeningQuestion questionB = questionRepository.saveAndFlush(
                new ScreeningQuestion("Q-B", "Question for Version B", QuestionType.TEXT, true, true)
        );
        itemRepository.saveAndFlush(new QuestionnaireItem(versionB, questionB, 1, null));

        assertTrue(publicationPort.isPublished(versionA.getId()));
        assertTrue(publicationPort.isPublished(versionB.getId()));

        // 2. Assign Version B specifically to a test campaign with an open booking window
        Instant bookingOpenAt = clock.instant().minusSeconds(3600);
        Instant bookingCloseAt = clock.instant().plusSeconds(86400 * 30);
        LocalDate serviceStartDate = today.minusDays(1);
        LocalDate serviceEndDate = today.plusDays(30);

        VaccinationCampaign campaign = campaignService.createCampaign(
                "CMP-CROSS-TEST",
                "Cross Module Test Campaign",
                "Testing Version B specific resolution",
                vaccineType.getId(),
                targetGroup.getId(),
                serviceStartDate,
                serviceEndDate,
                bookingOpenAt,
                bookingCloseAt,
                versionB.getId()
        );
        assertNotNull(campaign.getId());
        assertEquals(versionB.getId(), campaignService.getAssignedQuestionnaireVersionId(campaign.getId()));

        // Publish the campaign so that the booking and screening window is active
        campaign.publish();
        campaignRepository.saveAndFlush(campaign);

        assertEquals(1, applicationContext.getBeansOfType(CampaignScreeningReader.class).size());
        assertTrue(campaignReader.getRequiredCampaign(campaign.getId()).isBookingWindowOpenAt(clock.instant()));
        assertEquals(serviceEndDate, campaignReader.getRequiredCampaign(campaign.getId()).serviceEndDate());
        var assignmentSnapshot = campaignReader.getEffectiveAssignmentAt(campaign.getId(), today).orElseThrow();
        assertNotNull(assignmentSnapshot.id());
        assertEquals(campaign.getId(), assignmentSnapshot.campaignId());
        assertEquals(versionB.getId(), assignmentSnapshot.questionnaireVersionId());
        assertTrue(campaignReader.getEffectiveAssignmentAt(Long.MAX_VALUE, today).isEmpty());

        // 3. Resolve the citizen screening questionnaire for that campaign
        ScreeningFormResponse screeningForm = screeningFormService.getFormForCampaign(campaign.getId());

        // 4. Assert that the resolved questionnaire version is Version B, NOT Version A
        assertNotNull(screeningForm);
        assertEquals(versionB.getId(), screeningForm.questionnaireVersionId(),
                "Resolved questionnaire version must be Version B, NOT Version A");
        assertEquals(2, screeningForm.questionnaireVersionNumber());
        assertEquals(1, screeningForm.questions().size());
        assertEquals("Q-B", screeningForm.questions().get(0).code());

        // 5. Submit citizen screening answers for that campaign referencing Version B
        Long testCitizenId = 999123L;
        Map<Long, String> answers = Map.of(questionB.getId(), "No contraindications");

        ScreeningSubmission submission = screeningSubmissionService.submitScreening(
                testCitizenId,
                campaign.getId(),
                versionB.getId(),
                consentNotice.getId(),
                answers
        );

        // 6. Assert the ScreeningSubmission permanently references Version B
        assertNotNull(submission.getId());
        assertNotNull(submission.getQuestionnaireVersion());
        assertEquals(versionB.getId(), submission.getQuestionnaireVersion().getId(),
                "Submission must permanently bind to Version B");
        assertEquals(2, submission.getQuestionnaireVersion().getVersionNumber());

        // 7. Verify hasCampaignSubmissions port returns true
        assertTrue(publicationPort.hasCampaignSubmissions(campaign.getId()),
                "Publication port must detect citizen screening submissions for the campaign");

        // 8. Assert changing questionnaire version is blocked due to historical integrity protection
        IllegalStateException assignmentEx = assertThrows(IllegalStateException.class, () ->
                assignmentService.assignQuestionnaireVersion(campaign.getId(), versionA.getId(), today.plusDays(1))
        );
        assertTrue(assignmentEx.getMessage().contains("Cannot change questionnaire assignment for a campaign with existing citizen screening submissions"));
    }

    @Test
    @DisplayName("Campaign cannot be scheduled or published without an effective questionnaire assignment")
    void campaignCannotBeScheduledOrPublishedWithoutQuestionnaireAssignment() {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.plusDays(10);
        LocalDate end = today.plusDays(40);

        // Create a DRAFT campaign without questionnaire assignment
        VaccinationCampaign campaign = campaignService.createCampaign(
                "CMP-NO-Q-TEST",
                "Campaign Without Questionnaire",
                "Validation check",
                vaccineType.getId(),
                targetGroup.getId(),
                start,
                end,
                clock.instant().plusSeconds(3600),
                clock.instant().plusSeconds(86400 * 30),
                null
        );
        assertFalse(campaignReader.getRequiredCampaign(campaign.getId()).isBookingWindowOpenAt(clock.instant()));
        assertTrue(campaignReader.getEffectiveAssignmentAt(campaign.getId(), today).isEmpty());

        // Add dose programme so scheduling and publishing proceed to the questionnaire assignment prerequisite check
        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1,2", 21, false, 10,
                start, null
        ));

        // Scheduling must fail because questionnaire assignment is missing
        IllegalStateException schedEx = assertThrows(IllegalStateException.class, () ->
                campaignService.scheduleCampaign(campaign.getId())
        );
        assertTrue(schedEx.getMessage().contains("requires exactly one effective questionnaire assignment"));

        // Direct publication must also fail because questionnaire assignment is missing
        IllegalStateException pubEx = assertThrows(IllegalStateException.class, () ->
                campaignService.publishCampaign(campaign.getId())
        );
        assertTrue(pubEx.getMessage().contains("requires exactly one effective questionnaire assignment"));
    }

    @Test
    @DisplayName("QuestionnaireVersionRepository and QuestionnaireVersionService return existing published versions")
    void existingPublishedQuestionnaireVersionsReturnedFromRepositoryAndService() {
        Questionnaire questionnaire = questionnaireRepository.saveAndFlush(
                new Questionnaire("PUB-VERIFY-Q", "Verification Clinical Questionnaire", "General screening")
        );

        QuestionnaireVersion draft = versionRepository.saveAndFlush(
                new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.DRAFT)
        );
        QuestionnaireVersion published = versionRepository.saveAndFlush(
                new QuestionnaireVersion(questionnaire, 2, QuestionnaireVersionStatus.PUBLISHED)
        );

        // 1. Verify repository findByStatus returns only published versions
        List<QuestionnaireVersion> repoPublished = versionRepository.findByStatus(QuestionnaireVersionStatus.PUBLISHED);
        assertTrue(repoPublished.stream().anyMatch(v -> v.getId().equals(published.getId())));
        assertTrue(repoPublished.stream().noneMatch(v -> v.getId().equals(draft.getId())));

        // 2. Verify QuestionnaireVersionService.getPublishedVersions returns the published version
        List<QuestionnaireVersion> servicePublished = questionnaireVersionService.getPublishedVersions();
        assertTrue(servicePublished.stream().anyMatch(v -> v.getId().equals(published.getId())));
        assertTrue(servicePublished.stream().noneMatch(v -> v.getId().equals(draft.getId())));

        // 3. Verify publication port contract matches
        assertTrue(publicationPort.isPublished(published.getId()));
        assertTrue(!publicationPort.isPublished(draft.getId()));
    }
}
