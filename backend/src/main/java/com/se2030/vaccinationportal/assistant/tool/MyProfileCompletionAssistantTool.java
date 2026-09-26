package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenProfileResponse;
import com.se2030.vaccinationportal.shared.auth.service.CitizenProfileService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Privacy-minimized profile completion summary for the authenticated citizen.
 */
@Component
public class MyProfileCompletionAssistantTool implements AssistantReadOnlyTool {

    private static final List<Pattern> INTENTS = List.of(
            Pattern.compile("\\b(is|check|show|view|what\\s+is)\\s+my\\s+profile\\s+(complete|completion|status)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhat('s|\\s+is)\\s+missing\\s+from\\s+my\\s+profile\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmy\\s+account\\s+status\\b", Pattern.CASE_INSENSITIVE)
    );

    private final CitizenProfileService citizenProfileService;

    public MyProfileCompletionAssistantTool(CitizenProfileService citizenProfileService) {
        this.citizenProfileService = citizenProfileService;
    }

    @Override
    public AssistantToolId getToolId() {
        return AssistantToolId.MY_PROFILE_COMPLETION;
    }

    @Override
    public boolean matchesIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        return INTENTS.stream().anyMatch(pattern -> pattern.matcher(userMessage.trim()).find());
    }

    @Override
    @Transactional(readOnly = true)
    public AssistantToolResult execute(AssistantToolContext context) {
        if (context == null || context.principal() == null || !context.isCitizen()) {
            throw new IllegalArgumentException("Personalized tools require an authenticated citizen principal.");
        }
        CitizenProfileResponse profile = citizenProfileService.getOwnProfile(context.getCitizenId());
        List<String> missing = new ArrayList<>();
        if (!hasText(profile.fullName())) missing.add("full name");
        if (!hasText(profile.phoneNumber())) missing.add("phone number");
        if (profile.dateOfBirth() == null) missing.add("date of birth");
        if (profile.gender() == null) missing.add("gender");
        if (!hasText(profile.address())) missing.add("address");

        String accountStatus = profile.accountStatus() == null ? "UNKNOWN" : profile.accountStatus().name();
        String summary = missing.isEmpty()
                ? "Profile completion: COMPLETE. Recorded account status: " + accountStatus + ". No required profile detail is missing."
                : "Profile completion: INCOMPLETE. Recorded account status: " + accountStatus
                    + ". Missing profile details: " + String.join(", ", missing) + ".";
        return new AssistantToolResult(
                AssistantToolId.MY_PROFILE_COMPLETION,
                true,
                1,
                summary + " Do not reveal the profile's raw personal values in chat.",
                List.of("/app/citizen/profile")
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
