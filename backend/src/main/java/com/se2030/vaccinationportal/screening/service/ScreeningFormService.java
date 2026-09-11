package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormAnswerOptionResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormConsentNoticeResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormQuestionResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningFormResponse;
import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.model.AnswerOption;
import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Builds the complete citizen-facing screening form for an applicable campaign. */
@Service
public class ScreeningFormService {

    private final VaccinationCampaignService campaignService;
    private final CampaignQuestionnaireAssignmentService assignmentService;
    private final QuestionnaireVersionRepository versionRepository;
    private final QuestionnaireItemRepository itemRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ScreeningConsentNoticeService consentNoticeService;
    private final Clock clock;

    public ScreeningFormService(
            VaccinationCampaignService campaignService,
            CampaignQuestionnaireAssignmentService assignmentService,
            QuestionnaireVersionRepository versionRepository,
            QuestionnaireItemRepository itemRepository,
            AnswerOptionRepository answerOptionRepository,
            ScreeningConsentNoticeService consentNoticeService,
            Clock clock
    ) {
        this.campaignService = campaignService;
        this.assignmentService = assignmentService;
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.consentNoticeService = consentNoticeService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScreeningFormResponse getFormForCampaign(Long campaignId) {
        VaccinationCampaign campaign = campaignService.getRequiredCampaignById(campaignId);
        if (!campaign.isBookingWindowOpenAt(clock.instant())) {
            throw new IllegalStateException(
                    "Campaign " + campaignId + " is not currently accepting screening submissions"
            );
        }

        LocalDate effectiveDate = LocalDate.now(clock);
        CampaignQuestionnaireAssignment assignment = assignmentService
                .getEffectiveAssignmentAt(campaignId, effectiveDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Campaign " + campaignId + " has no effective questionnaire assignment on " + effectiveDate
                ));

        Long versionId = assignment.getQuestionnaireVersionId();
        QuestionnaireVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionnaire version not found with ID: " + versionId
                ));
        if (version.getStatus() != QuestionnaireVersionStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Campaign screening form requires a PUBLISHED questionnaire version"
            );
        }

        List<ScreeningFormQuestionResponse> questions = itemRepository
                .findByQuestionnaireVersionIdOrderByDisplayOrderAsc(versionId)
                .stream()
                .map(this::toQuestionResponse)
                .toList();
        ConsentNoticeVersion consent = consentNoticeService.getCurrentScreeningConsentNotice();

        return new ScreeningFormResponse(
                campaignId,
                version.getId(),
                version.getVersionNumber(),
                toConsentResponse(consent),
                questions
        );
    }

    private ScreeningFormQuestionResponse toQuestionResponse(QuestionnaireItem item) {
        ScreeningQuestion question = item.getQuestion();
        boolean required = item.getRequiredOverride() != null
                ? item.getRequiredOverride()
                : question.isDefaultRequired();
        List<ScreeningFormAnswerOptionResponse> options = answerOptionRepository
                .findByQuestionIdOrderByDisplayOrderAsc(question.getId())
                .stream()
                .map(this::toOptionResponse)
                .toList();

        return new ScreeningFormQuestionResponse(
                question.getId(),
                question.getCode(),
                question.getText(),
                question.getDescription(),
                question.getQuestionType(),
                required,
                item.getDisplayOrder(),
                options
        );
    }

    private ScreeningFormAnswerOptionResponse toOptionResponse(AnswerOption option) {
        return new ScreeningFormAnswerOptionResponse(
                option.getId(),
                option.getCode(),
                option.getLabel(),
                option.getTypedValue(),
                option.getDisplayOrder()
        );
    }

    private ScreeningFormConsentNoticeResponse toConsentResponse(ConsentNoticeVersion consent) {
        return new ScreeningFormConsentNoticeResponse(
                consent.getId(),
                consent.getNoticeCode(),
                consent.getVersionNumber(),
                consent.getTitle(),
                consent.getContent(),
                consent.getContentHash(),
                consent.getPublishedAt()
        );
    }
}
