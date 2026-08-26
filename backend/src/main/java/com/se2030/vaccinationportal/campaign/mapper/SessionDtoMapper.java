package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.response.SessionChangeEventResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionOperationalReadinessResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionScheduleSnapshotResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionScheduleSnapshot;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionOperationalReadinessService;

/**
 * Pure transformation utility mapping VaccinationSession domain models to REST response DTOs.
 */
public final class SessionDtoMapper {

    private SessionDtoMapper() {
    }

    public static SessionOperationalReadinessResponse toReadinessResponse(
            SessionOperationalReadinessService.OperationalReadinessResult result
    ) {
        if (result == null) {
            return null;
        }
        return new SessionOperationalReadinessResponse(
                result.sessionId(),
                result.ready(),
                result.violations(),
                result.assignedVaccinators(),
                result.availableStations(),
                result.effectiveStations(),
                result.recommendedThroughput(),
                result.approvedCapacity(),
                result.revision()
        );
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

    public static SessionScheduleSnapshotResponse toSnapshotResponse(SessionScheduleSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new SessionScheduleSnapshotResponse(
                snapshot.getCenterId(),
                snapshot.getScheduleTimezone(),
                snapshot.getStartAt(),
                snapshot.getEndAt(),
                snapshot.getBookingCloseAt(),
                snapshot.getLifecycleStatus()
        );
    }

    public static SessionChangePreviewResponse toPreviewResponse(SessionChangePreview preview) {
        if (preview == null) {
            return null;
        }
        return new SessionChangePreviewResponse(
                preview.sessionId(),
                preview.changeType(),
                toSnapshotResponse(preview.oldSnapshot()),
                toSnapshotResponse(preview.newSnapshot()),
                preview.affectedAppointmentCount(),
                preview.affectedWaitlistCount(),
                preview.previewToken()
        );
    }

    public static SessionChangeEventResponse toChangeEventResponse(SessionChangeEvent event) {
        if (event == null) {
            return null;
        }
        Long sessionId = event.getSession() != null ? event.getSession().getId() : null;
        return new SessionChangeEventResponse(
                event.getId(),
                sessionId,
                event.getChangeType(),
                toSnapshotResponse(event.getOldSnapshot()),
                toSnapshotResponse(event.getNewSnapshot()),
                event.getReason(),
                event.getAffectedAppointmentCount(),
                event.getAffectedWaitlistCount(),
                event.getActorId(),
                event.getCreatedAt()
        );
    }
}
