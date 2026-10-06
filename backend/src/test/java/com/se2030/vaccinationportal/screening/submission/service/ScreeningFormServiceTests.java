package com.se2030.vaccinationportal.screening.submission.service;

import com.se2030.vaccinationportal.screening.questionnaire.service.ScreeningConsentNoticeService;


import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningReader;
import com.se2030.vaccinationportal.campaign.api.screening.CampaignScreeningSnapshot;
import com.se2030.vaccinationportal.campaign.api.screening.QuestionnaireAssignmentSnapshot;
import com.se2030.vaccinationportal.screening.submission.dto.response.ScreeningFormResponse;
import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.questionnaire.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningFormServiceTests {

    private static final Long CAMPAIGN_ID = 2L;
    private static final Long VERSION_ID = 20L;
    private static final Instant NOW = Instant.parse("2026-09-03T04:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);

    @Mock
    private CampaignScreeningReader campaignReader;
    @Mock
    private QuestionnaireVersionRepository versionRepository;
    @Mock
    private QuestionnaireItemRepository itemRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private ScreeningConsentNoticeService consentNoticeService;

    private ScreeningFormService service;
    private CampaignScreeningSnapshot campaign;
    private QuestionnaireVersion version;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ScreeningFormService(
                campaignReader,
                versionRepository,
                itemRepository,
                answerOptionRepository,
                consentNoticeService,
                clock
        );
        campaign = new CampaignScreeningSnapshot(CAMPAIGN_ID, TODAY.plusDays(30),
                NOW.minusSeconds(3600), NOW.plusSeconds(3600), true);
        version = new QuestionnaireVersion();
        version.setId(VERSION_ID);
        version.setVersionNumber(3);
        version.setStatus(QuestionnaireVersionStatus.PUBLISHED);
    }

    @Test
    void buildsHydratedOrderedFormWithEffectiveRequiredValuesAndCanonicalConsent() {
        stubApplicableCampaignAndPublishedVersion();

        ScreeningQuestion first = question(101L, "FEVER", "Do you have a fever?", true);
        ScreeningQuestion second = question(102L, "ALLERGY", "Do you have allergies?", true);
        ScreeningQuestion third = question(103L, "NOTES", "Additional notes", false);
        QuestionnaireItem firstItem = new QuestionnaireItem(version, first, 1, true);
        QuestionnaireItem secondItem = new QuestionnaireItem(version, second, 2, false);
        QuestionnaireItem thirdItem = new QuestionnaireItem(version, third, 3, null);
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of(firstItem, secondItem, thirdItem));

        AnswerOption later = option(first, 202L, "NO", "No", "false", 2);
        AnswerOption earlier = option(first, 201L, "YES", "Yes", "true", 1);
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(101L))
                .thenReturn(List.of(earlier, later));
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(102L)).thenReturn(List.of());
        when(answerOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(103L)).thenReturn(List.of());

        ConsentNoticeVersion consent = consent(30L, 4);
        when(consentNoticeService.getCurrentScreeningConsentNotice()).thenReturn(consent);

        ScreeningFormResponse result = service.getFormForCampaign(CAMPAIGN_ID);

        assertThat(result.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(result.questionnaireVersionId()).isEqualTo(VERSION_ID);
        assertThat(result.questionnaireVersionNumber()).isEqualTo(3);
        assertThat(result.questions()).extracting(question -> question.displayOrder())
                .containsExactly(1, 2, 3);
        assertThat(result.questions()).extracting(question -> question.required())
                .containsExactly(true, false, false);
        assertThat(result.questions().getFirst().id()).isEqualTo(101L);
        assertThat(result.questions().getFirst().code()).isEqualTo("FEVER");
        assertThat(result.questions().getFirst().text()).isEqualTo("Do you have a fever?");
        assertThat(result.questions().getFirst().description()).isEqualTo("Synthetic description");
        assertThat(result.questions().getFirst().questionType()).isEqualTo(QuestionType.BOOLEAN);
        assertThat(result.questions().getFirst().options())
                .extracting(option -> option.displayOrder())
                .containsExactly(1, 2);
        assertThat(result.questions().getFirst().options().getFirst().typedValue()).isEqualTo("true");
        assertThat(result.consentNotice().id()).isEqualTo(30L);
        assertThat(result.consentNotice().noticeCode()).isEqualTo("PRE_VACCINATION_SCREENING");
        assertThat(result.consentNotice().versionNumber()).isEqualTo(4);
        assertThat(result.consentNotice().title()).isEqualTo("Synthetic screening consent");
        assertThat(result.consentNotice().content()).isEqualTo("Synthetic test content");
        assertThat(result.consentNotice().contentHash()).isEqualTo("synthetic-hash");
        assertThat(result.consentNotice().publishedAt()).isEqualTo(NOW.minusSeconds(60));
        verify(consentNoticeService).getCurrentScreeningConsentNotice();
    }

    @Test
    void rejectsQuestionnaireVersionThatIsNotPublished() {
        stubApplicableCampaign();
        version.setStatus(QuestionnaireVersionStatus.DRAFT);
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.getFormForCampaign(CAMPAIGN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void rejectsCampaignOutsideExistingScreeningWindow() {
        when(campaignReader.getRequiredCampaign(CAMPAIGN_ID)).thenReturn(
                new CampaignScreeningSnapshot(CAMPAIGN_ID, TODAY.plusDays(30),
                        NOW.plusSeconds(1), NOW.plusSeconds(3600), true));

        assertThatThrownBy(() -> service.getFormForCampaign(CAMPAIGN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not currently accepting screening submissions");
    }

    @Test
    void reportsCampaignWithoutEffectiveQuestionnaireAssignment() {
        when(campaignReader.getRequiredCampaign(CAMPAIGN_ID)).thenReturn(campaign);
        when(campaignReader.getEffectiveAssignmentAt(CAMPAIGN_ID, TODAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFormForCampaign(CAMPAIGN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no effective questionnaire assignment");
    }

    @Test
    void preservesInvalidCampaignNotFoundBehavior() {
        when(campaignReader.getRequiredCampaign(CAMPAIGN_ID))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: 2"));

        assertThatThrownBy(() -> service.getFormForCampaign(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Campaign not found with ID: 2");
    }

    @Test
    void reportsMissingCanonicalConsentWithoutFabricatingOne() {
        stubApplicableCampaignAndPublishedVersion();
        when(itemRepository.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(VERSION_ID))
                .thenReturn(List.of());
        when(consentNoticeService.getCurrentScreeningConsentNotice())
                .thenThrow(new com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException(
                        "No current consent notice exists for screening notice code PRE_VACCINATION_SCREENING"
                ));

        assertThatThrownBy(() -> service.getFormForCampaign(CAMPAIGN_ID))
                .isInstanceOf(com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException.class)
                .hasMessageContaining("PRE_VACCINATION_SCREENING");
    }

    private void stubApplicableCampaignAndPublishedVersion() {
        stubApplicableCampaign();
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
    }

    private void stubApplicableCampaign() {
        when(campaignReader.getRequiredCampaign(CAMPAIGN_ID)).thenReturn(campaign);
        QuestionnaireAssignmentSnapshot assignment = new QuestionnaireAssignmentSnapshot(1L, CAMPAIGN_ID, VERSION_ID);
        when(campaignReader.getEffectiveAssignmentAt(CAMPAIGN_ID, TODAY)).thenReturn(Optional.of(assignment));
    }

    private ScreeningQuestion question(Long id, String code, String text, boolean defaultRequired) {
        ScreeningQuestion question = new ScreeningQuestion(code, text, QuestionType.BOOLEAN, defaultRequired, true);
        question.setId(id);
        question.setDescription("Synthetic description");
        return question;
    }

    private AnswerOption option(
            ScreeningQuestion question,
            Long id,
            String code,
            String label,
            String typedValue,
            int displayOrder
    ) {
        AnswerOption option = new AnswerOption(question, code, label, typedValue, displayOrder);
        option.setId(id);
        return option;
    }

    private ConsentNoticeVersion consent(Long id, int versionNumber) {
        ConsentNoticeVersion consent = new ConsentNoticeVersion(
                ScreeningConsentNoticeService.SCREENING_NOTICE_CODE,
                versionNumber,
                "Synthetic screening consent",
                "Synthetic test content",
                "synthetic-hash",
                NOW.minusSeconds(60)
        );
        consent.setId(id);
        return consent;
    }
}
