package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.CampaignLifecycleEventResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignSummaryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CitizenCampaignDiscoveryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.TargetGroupSummaryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.VaccineTypeSummaryResponse;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;

/**
 * Pure transformation utility mapping Campaign domain models to REST response DTOs.
 */
public final class CampaignDtoMapper {

    private CampaignDtoMapper() {
    }

    public static VaccineTypeSummaryResponse toSummaryResponse(VaccineType vaccineType) {
        if (vaccineType == null) {
            return null;
        }
        return new VaccineTypeSummaryResponse(
                vaccineType.getId(),
                vaccineType.getCode(),
                vaccineType.getName(),
                vaccineType.getDoseUnit(),
                vaccineType.isActive()
        );
    }

    public static TargetGroupSummaryResponse toSummaryResponse(TargetGroup targetGroup) {
        if (targetGroup == null) {
            return null;
        }
        return new TargetGroupSummaryResponse(
                targetGroup.getId(),
                targetGroup.getCode(),
                targetGroup.getDescription(),
                targetGroup.isActive()
        );
    }

    public static CampaignResponse toResponse(VaccinationCampaign campaign) {
        return toResponse(campaign, null);
    }

    public static CampaignResponse toResponse(VaccinationCampaign campaign, Long questionnaireVersionId) {
        if (campaign == null) {
            return null;
        }
        return new CampaignResponse(
                campaign.getId(),
                campaign.getCode(),
                campaign.getName(),
                campaign.getDescription(),
                toSummaryResponse(campaign.getVaccineType()),
                toSummaryResponse(campaign.getTargetGroup()),
                campaign.getServiceStartDate(),
                campaign.getServiceEndDate(),
                campaign.getBookingOpenAt(),
                campaign.getBookingCloseAt(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt(),
                questionnaireVersionId
        );
    }

    public static CampaignSummaryResponse toSummaryResponse(VaccinationCampaign campaign) {
        return toSummaryResponse(campaign, null);
    }

    public static CampaignSummaryResponse toSummaryResponse(VaccinationCampaign campaign, Long questionnaireVersionId) {
        if (campaign == null) {
            return null;
        }
        String vaccineTypeCode = campaign.getVaccineType() != null ? campaign.getVaccineType().getCode() : null;
        String targetGroupCode = campaign.getTargetGroup() != null ? campaign.getTargetGroup().getCode() : null;
        return new CampaignSummaryResponse(
                campaign.getId(),
                campaign.getCode(),
                campaign.getName(),
                vaccineTypeCode,
                targetGroupCode,
                campaign.getServiceStartDate(),
                campaign.getServiceEndDate(),
                campaign.getStatus(),
                questionnaireVersionId
        );
    }

    public static CampaignLifecycleEventResponse toResponse(CampaignLifecycleEvent event) {
        if (event == null) {
            return null;
        }
        Long campaignId = event.getCampaign() != null ? event.getCampaign().getId() : null;
        return new CampaignLifecycleEventResponse(
                event.getId(),
                campaignId,
                event.getChangeType(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getActorId(),
                event.getCreatedAt()
        );
    }

    public static CitizenCampaignDiscoveryResponse toCitizenDiscoveryResponse(
            VaccinationCampaign campaign,
            boolean currentlyAcceptingScreening
    ) {
        if (campaign == null) {
            return null;
        }
        String vaccineTypeCode = campaign.getVaccineType() != null ? campaign.getVaccineType().getCode() : null;
        String targetGroupCode = campaign.getTargetGroup() != null ? campaign.getTargetGroup().getCode() : null;
        return new CitizenCampaignDiscoveryResponse(
                campaign.getId(),
                campaign.getCode(),
                campaign.getName(),
                vaccineTypeCode,
                targetGroupCode,
                campaign.getServiceStartDate(),
                campaign.getServiceEndDate(),
                campaign.getStatus(),
                currentlyAcceptingScreening
        );
    }
}
