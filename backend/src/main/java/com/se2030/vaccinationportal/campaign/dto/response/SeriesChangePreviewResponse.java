package com.se2030.vaccinationportal.campaign.dto.response;

import java.util.List;

/**
 * Representation of a series-wide schedule change preview with impact counts and token.
 */
public record SeriesChangePreviewResponse(
        Long ruleId,
        RecurrenceRuleDefinitionSnapshotResponse currentRuleDefinition,
        RecurrenceRuleDefinitionSnapshotResponse proposedRuleDefinition,
        List<SeriesOccurrencePreviewItemResponse> occurrenceEntries,
        int totalAffectedAppointmentsCount,
        int totalAffectedWaitlistCount,
        String previewToken
) {
}
