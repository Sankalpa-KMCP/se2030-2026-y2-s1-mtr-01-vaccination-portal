package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.SessionResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;

/**
 * Pure transformation utility mapping VaccinationSession domain models to REST response DTOs.
 */
public final class SessionDtoMapper {

    private SessionDtoMapper() {
    }

    public static SessionResponse toResponse(VaccinationSession session) {
        if (session == null) {
            return null;
        }
        Long campaignId = session.getCampaign() != null ? session.getCampaign().getId() : null;
        Long centerId = session.getCenter() != null ? session.getCenter().getId() : null;
        Long recurrenceRuleId = session.getRecurrenceRule() != null ? session.getRecurrenceRule().getId() : null;
        return new SessionResponse(
                session.getId(),
                campaignId,
                centerId,
                recurrenceRuleId,
                session.getOccurrenceKey(),
                session.getOriginOccurrenceDate(),
                session.getStartAt(),
                session.getEndAt(),
                session.getBookingCloseAt(),
                session.getScheduleTimezone(),
                session.getLifecycleStatus(),
                session.isGenerated(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    public static SessionSummaryResponse toSummaryResponse(VaccinationSession session) {
        if (session == null) {
            return null;
        }
        Long campaignId = session.getCampaign() != null ? session.getCampaign().getId() : null;
        Long centerId = session.getCenter() != null ? session.getCenter().getId() : null;
        return new SessionSummaryResponse(
                session.getId(),
                campaignId,
                centerId,
                session.getStartAt(),
                session.getEndAt(),
                session.getBookingCloseAt(),
                session.getScheduleTimezone(),
                session.getLifecycleStatus(),
                session.isGenerated()
        );
    }
}
