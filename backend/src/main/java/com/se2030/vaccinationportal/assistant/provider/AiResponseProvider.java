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
     * Indicates whether the provider is active and ready to service requests.
     */
    boolean isAvailable();
}
