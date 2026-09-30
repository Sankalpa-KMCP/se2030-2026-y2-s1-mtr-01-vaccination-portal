package com.se2030.vaccinationportal.campaign.session.recurrence.service;

import com.se2030.vaccinationportal.campaign.session.recurrence.model.RecurrenceRuleDefinitionSnapshot;
import java.util.List;

/**
 * Immutable preview result representing the projected diff, categorization, impact counts,
 * and cryptographic stale-preview token for a proposed series-wide change.
 *
 * @param ruleId                         ID of the target SessionRecurrenceRule
 * @param currentRuleDefinition          Snapshot of the rule's current active definition
 * @param proposedRuleDefinition         Snapshot of the proposed new rule definition
 * @param occurrenceEntries              Chronologically ordered list of categorized occurrence entries
 * @param totalAffectedAppointmentsCount Total affected confirmed appointments across all mutated sessions
 * @param totalAffectedWaitlistCount     Total affected waitlist entries across all mutated sessions
 * @param previewToken                   Cryptographic SHA-256 fingerprint protecting against stale confirmation
 */
public record SeriesChangePreview(
        Long ruleId,
        RecurrenceRuleDefinitionSnapshot currentRuleDefinition,
        RecurrenceRuleDefinitionSnapshot proposedRuleDefinition,
        List<SeriesOccurrencePreviewItem> occurrenceEntries,
        int totalAffectedAppointmentsCount,
        int totalAffectedWaitlistCount,
        String previewToken
) {
    public SeriesChangePreview {
        if (ruleId == null) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        if (currentRuleDefinition == null) {
            throw new IllegalArgumentException("Current rule definition snapshot is required");
        }
        if (proposedRuleDefinition == null) {
            throw new IllegalArgumentException("Proposed rule definition snapshot is required");
        }
        if (occurrenceEntries == null) {
            throw new IllegalArgumentException("Occurrence entries list is required");
        }
        occurrenceEntries = List.copyOf(occurrenceEntries);
        if (totalAffectedAppointmentsCount < 0) {
            throw new IllegalArgumentException("Total affected appointments count cannot be negative");
        }
        if (totalAffectedWaitlistCount < 0) {
            throw new IllegalArgumentException("Total affected waitlist count cannot be negative");
        }
        if (previewToken == null || previewToken.isBlank()) {
            throw new IllegalArgumentException("Preview token is required");
        }
        previewToken = previewToken.trim();
    }
}
