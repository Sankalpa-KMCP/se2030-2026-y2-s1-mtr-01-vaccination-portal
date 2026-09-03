package com.se2030.vaccinationportal.campaign.mapper;

import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSeriesChangeRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSeriesChangeRequest;
import com.se2030.vaccinationportal.campaign.dto.response.RecurrenceGenerationResponse;
import com.se2030.vaccinationportal.campaign.dto.response.RecurrenceRuleDefinitionSnapshotResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesChangeResultResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesOccurrencePreviewItemResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionRecurrenceRuleResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesChangeResult;
import com.se2030.vaccinationportal.campaign.service.SeriesOccurrencePreviewItem;
import java.util.Collections;
import java.util.List;

/**
 * Pure transformation utility mapping SessionRecurrenceRule domain models and results to REST DTOs.
 */
public final class RecurrenceRuleDtoMapper {

    private RecurrenceRuleDtoMapper() {
    }

    public static SessionRecurrenceRuleResponse toResponse(SessionRecurrenceRule rule) {
        if (rule == null) {
            return null;
        }
        Long campaignId = rule.getCampaign() != null ? rule.getCampaign().getId() : null;
        Long centerId = rule.getCenter() != null ? rule.getCenter().getId() : null;
        return new SessionRecurrenceRuleResponse(
                rule.getId(),
                campaignId,
                centerId,
                rule.getWeekdays(),
                rule.getStartDate(),
                rule.getEndDate(),
                rule.getStartTime(),
                rule.getEndTime(),
                rule.getTimezone(),
                rule.getStatus(),
                rule.getGenerationKey(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    public static RecurrenceRuleDefinitionSnapshotResponse toSnapshotResponse(RecurrenceRuleDefinitionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new RecurrenceRuleDefinitionSnapshotResponse(
                snapshot.getWeekdays(),
                snapshot.getStartDate(),
                snapshot.getEndDate(),
                snapshot.getStartTime(),
                snapshot.getEndTime(),
                snapshot.getTimezone()
        );
    }

    public static RecurrenceRuleDefinitionSnapshot toDefinitionSnapshot(PreviewSeriesChangeRequest request) {
        if (request == null) {
            return null;
        }
        return new RecurrenceRuleDefinitionSnapshot(
                request.weekdays(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.timezone()
        );
    }

    public static RecurrenceRuleDefinitionSnapshot toDefinitionSnapshot(ConfirmSeriesChangeRequest request) {
        if (request == null) {
            return null;
        }
        return new RecurrenceRuleDefinitionSnapshot(
                request.weekdays(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.timezone()
        );
    }

    public static SeriesOccurrencePreviewItemResponse toPreviewItemResponse(SeriesOccurrencePreviewItem item) {
        if (item == null) {
            return null;
        }
        return new SeriesOccurrencePreviewItemResponse(
                item.occurrenceKey(),
                item.originOccurrenceDate(),
                item.category(),
                item.sessionId(),
                SessionDtoMapper.toSnapshotResponse(item.currentSnapshot()),
                SessionDtoMapper.toSnapshotResponse(item.proposedSnapshot()),
                item.affectedAppointmentCount(),
                item.affectedWaitlistCount()
        );
    }

    public static SeriesChangePreviewResponse toPreviewResponse(SeriesChangePreview preview) {
        if (preview == null) {
            return null;
        }
        List<SeriesOccurrencePreviewItemResponse> entries = preview.occurrenceEntries() != null
                ? preview.occurrenceEntries().stream().map(RecurrenceRuleDtoMapper::toPreviewItemResponse).toList()
                : Collections.emptyList();

        return new SeriesChangePreviewResponse(
                preview.ruleId(),
                toSnapshotResponse(preview.currentRuleDefinition()),
                toSnapshotResponse(preview.proposedRuleDefinition()),
                entries,
                preview.totalAffectedAppointmentsCount(),
                preview.totalAffectedWaitlistCount(),
                preview.previewToken()
        );
    }

    public static SeriesChangeResultResponse toResultResponse(SeriesChangeResult result) {
        if (result == null) {
            return null;
        }
        return new SeriesChangeResultResponse(
                toResponse(result.rule()),
                toPreviewResponse(result.preview()),
                result.rescheduledCount(),
                result.cancelledCount(),
                result.newCount(),
                result.preservedCount(),
                result.ineligibleCount(),
                result.eventsCreatedCount()
        );
    }

    public static RecurrenceGenerationResponse toGenerationResponse(RecurrenceGenerationResult result) {
        if (result == null) {
            return null;
        }
        List<SessionSummaryResponse> sessionSummaries = result.sessions() != null
                ? result.sessions().stream().map(SessionDtoMapper::toSummaryResponse).toList()
                : Collections.emptyList();

        return new RecurrenceGenerationResponse(
                result.createdCount(),
                result.existingCount(),
                sessionSummaries
        );
    }
}
