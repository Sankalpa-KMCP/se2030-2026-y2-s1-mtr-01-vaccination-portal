package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.vaccination.dto.CitizenCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.CitizenVaccinationRecordResponse;
import com.se2030.vaccinationportal.vaccination.service.DigitalCertificateService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Server-side read-only tool providing privacy-minimized vaccination record and certificate summary for the authenticated citizen.
 * Reuses existing DigitalCertificateService citizen read methods.
 */
@Component
public class MyVaccinationSummaryAssistantTool implements AssistantReadOnlyTool {

    private final DigitalCertificateService digitalCertificateService;

    private static final List<Pattern> INTENT_PATTERNS = List.of(
            Pattern.compile("\\b(do\\s+i\\s+have|have\\s+i\\s+got)\\s+(a\\s+)?(vaccination|vaccine)\\s+record(s)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(do\\s+i\\s+have|have\\s+i\\s+got)\\s+(a\\s+)?(digital\\s+)?certificate(s)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(what\\s+is|what'?s|check|show|view)\\s+(the\\s+status\\s+of\\s+)?my\\s+certificate\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+many\\s+(finalized\\s+)?vaccination\\s+records\\s+do\\s+i\\s+have\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(my\\s+vaccination\\s+(record(s)?|history)|my\\s+digital\\s+certificate|my\\s+certificate\\s+status)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(show|view|find|get)\\s+(the\\s+)?certificate\\s+for\\s+(user|citizen|\\w+)\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> NEGATIVE_PATTERNS = List.of(
            Pattern.compile("\\bwhat\\s+is\\s+a\\s+(digital\\s+)?certificate\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+(do|can|to)\\s+i\\s+verify\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+to\\s+verify\\b", Pattern.CASE_INSENSITIVE)
    );

    public MyVaccinationSummaryAssistantTool(DigitalCertificateService digitalCertificateService) {
        this.digitalCertificateService = digitalCertificateService;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_VACCINATION_AND_CERTIFICATE_SUMMARY;
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

        Long citizenId = context.getCitizenId();
        List<CitizenVaccinationRecordResponse> records = digitalCertificateService.getCitizenVaccinationHistory(citizenId);
        List<CitizenCertificateResponse> certificates = digitalCertificateService.getCitizenCertificates(citizenId);

        List<String> routes = List.of("/app/citizen/vaccinations", "/verify-certificate");

        boolean hasRecords = records != null && !records.isEmpty();
        boolean hasCertificates = certificates != null && !certificates.isEmpty();

        if (!hasRecords && !hasCertificates) {
            return AssistantToolResult.empty(
                    AssistantToolId.MY_VACCINATION_AND_CERTIFICATE_SUMMARY,
                    "No finalized vaccination record or certificate was found.",
                    routes
            );
        }

        long finalizedCount = 0;
        List<CitizenVaccinationRecordResponse> boundedRecords = List.of();
        if (hasRecords) {
            finalizedCount = records.stream().filter(r -> r.finalizedAt() != null).count();
            boundedRecords = records.stream()
                    .filter(r -> r.finalizedAt() != null)
                    .limit(5)
                    .toList();
        }

        List<CitizenCertificateResponse> boundedCertificates = List.of();
        if (hasCertificates) {
            boundedCertificates = certificates.stream().limit(5).toList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Vaccination and Certificate Summary for authenticated citizen:\n");
        sb.append("- Total finalized vaccination records count: ").append(finalizedCount).append("\n");

        if (!boundedRecords.isEmpty()) {
            sb.append("- Recent administered doses (showing ").append(boundedRecords.size()).append(" most recent, bounded to at most 5):\n");
            for (CitizenVaccinationRecordResponse r : boundedRecords) {
                sb.append("  * Dose Number: ").append(r.doseNumber())
                        .append(", Administration Date/Time: ").append(r.administrationTime())
                        .append("\n");
            }
        }

        sb.append("- Digital Certificate Summary:\n");
        sb.append("  * Certificate exists: ").append(hasCertificates ? "Yes" : "No").append("\n");
        if (!boundedCertificates.isEmpty()) {
            for (CitizenCertificateResponse c : boundedCertificates) {
                sb.append("  * Certificate Status: ").append(c.status() != null ? c.status().name() : "UNKNOWN")
                        .append(", Issue Date: ").append(c.issuedAt())
                        .append("\n");
            }
        }

        sb.append("Note: Verification tokens, QR payloads, vaccine batch numbers, and clinical notes are confidential and excluded from assistant context.");

        int totalCount = boundedRecords.size() + boundedCertificates.size();
        return new AssistantToolResult(
                AssistantToolId.MY_VACCINATION_AND_CERTIFICATE_SUMMARY,
                true,
                totalCount,
                sb.toString(),
                routes
        );
    }
}
