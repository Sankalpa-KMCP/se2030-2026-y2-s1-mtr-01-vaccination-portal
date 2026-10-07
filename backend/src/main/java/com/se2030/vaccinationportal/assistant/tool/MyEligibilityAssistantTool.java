package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.screening.api.query.CitizenEligibilityQuery;
import com.se2030.vaccinationportal.screening.api.query.EligibilitySummary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-side read-only tool providing bounded eligibility assessment summary for the authenticated citizen.
 * Preserves clinical governance statement that final clinical eligibility is determined only by an authorized Healthcare Officer.
 */
@Component
public class MyEligibilityAssistantTool implements AssistantReadOnlyTool {

    public static final String GOVERNANCE_STATEMENT =
            "Final clinical eligibility is determined only by an authorized Healthcare Officer.";

    private final CitizenEligibilityQuery eligibilityQuery;

    private static final List<Pattern> INTENT_PATTERNS = List.of(
            Pattern.compile("\\b(what\\s+is|what'?s|check|show|view|tell\\s+me)\\s+(the\\s+status\\s+of\\s+)?(my\\s+)?eligibility(\\s+status)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my\\s+eligibility\\s+status|my\\s+eligibility)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(am\\s+i|is\\s+my\\s+account)\\s+(marked\\s+)?eligible(\\s+for\\s+(a|the)?\\s*campaign)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(has|is)\\s+my\\s+screening\\s+(been\\s+)?(reviewed|approved|evaluated)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(check\\s+my\\s+eligibility|eligibility\\s+for\\s+(citizen|user|me|\\d+))\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> NEGATIVE_PATTERNS = List.of(
            Pattern.compile("\\bwhat\\s+is\\s+(an?\\s+)?(eligibility|screening)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+(do|can|to)\\s+i\\s+(complete|submit|take)\\s+(a\\s+)?screening\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhat\\s+are\\s+the\\s+screening\\s+questions\\b", Pattern.CASE_INSENSITIVE)
    );

    public MyEligibilityAssistantTool(CitizenEligibilityQuery eligibilityQuery) {
        this.eligibilityQuery = eligibilityQuery;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_ELIGIBILITY_STATUS;
    }

    @Override
    public boolean matchesIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String normalized = userMessage.trim();
        for (Pattern negative : NEGATIVE_PATTERNS) {
            if (negative.matcher(normalized).find()) {
                return false;
            }
        }
        for (Pattern pattern : INTENT_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context) {
        if (context == null || context.principal() == null || !context.isCitizen()) {
            throw new IllegalArgumentException("Personalized tools require an authenticated citizen principal.");
        }

        List<EligibilitySummary> latest = eligibilityQuery.latestForCitizen(context.principal());
        List<String> routes = List.of("/app/screening/eligibility");

        if (latest.isEmpty()) {
            return AssistantToolResult.empty(
                    AssistantToolId.MY_ELIGIBILITY_STATUS,
                    "No recorded eligibility assessment was found.\nGovernance Statement: " + GOVERNANCE_STATEMENT,
                    routes
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Recorded Eligibility Assessments for the authenticated citizen (showing latest assessment per campaign, up to 5 campaigns):\n");

        for (EligibilitySummary a : latest) {
            sb.append("- Campaign ID: ").append(a.campaignId())
                    .append(", Recorded Status: ").append(a.recordedStatus())
                    .append(", Reviewed At: ").append(a.reviewedAt())
                    .append(", Valid Until: ").append(a.validUntil() != null ? a.validUntil().toString() : "Not specified")
                    .append(", Assessment Lifecycle: ").append(a.lifecycleState())
                    .append("\n");
        }

        sb.append("Governance Rule: ").append(GOVERNANCE_STATEMENT)
                .append(" The assistant may report the recorded status exactly, but it must not interpret symptoms or independently determine eligibility.");

        return new AssistantToolResult(
                AssistantToolId.MY_ELIGIBILITY_STATUS,
                true,
                latest.size(),
                sb.toString(),
                routes
        );
    }
}
