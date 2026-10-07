package com.se2030.vaccinationportal.assistant.provider;

import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;

/**
 * Provider SPI abstraction for executing AI Assistant queries.
 */
public interface AiResponseProvider {

    /**
     * Executes the conversational completion against the configured provider with caller authorities for filtering.
     *
     * @param request           the validated incoming client chat request
     * @param systemInstruction the formatted system instruction policy
     * @param requestId         the unique tracking request ID
     * @param callerAuthorities the normalized set of authorities held by the caller
     * @return the structured assistant chat response
     */
    AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId,
            java.util.Set<String> callerAuthorities
    );

    default AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId
    ) {
        return generateResponse(request, systemInstruction, requestId, java.util.Collections.emptySet());
    }

    /**
     * Maximum character limit for assistant responses to ensure subsequent history submission
     * complies with the 1,500-character AssistantMessageDto constraint.
     */
    int MAX_ANSWER_CHARACTERS = 1500;

    /**
     * Safely truncates response text to maxCharacters, preserving valid Unicode surrogate pairs
     * and appending an ellipsis if truncation occurred. The resulting string is guaranteed to be
     * <= maxCharacters in length.
     */
    static String safeTruncate(String text, int maxCharacters) {
        if (text == null || text.length() <= maxCharacters) {
            return text;
        }

        int targetIndex = maxCharacters - 3;
        if (targetIndex < 0) {
            targetIndex = 0;
        }

        // Step back if targetIndex splits a UTF-16 surrogate pair (orphaned high surrogate)
        if (targetIndex > 0 && Character.isHighSurrogate(text.charAt(targetIndex - 1))) {
            targetIndex--;
        }

        return text.substring(0, targetIndex) + "...";
    }

    static String safeTruncate(String text) {
        return safeTruncate(text, MAX_ANSWER_CHARACTERS);
    }

    /**
     * Indicates whether the provider is active and ready to service requests.
     */
    boolean isAvailable();
}
