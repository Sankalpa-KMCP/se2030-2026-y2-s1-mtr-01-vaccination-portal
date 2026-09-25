package com.se2030.vaccinationportal.assistant.provider;

import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;

/**
 * Provider SPI abstraction for executing AI Assistant queries.
 */
public interface AiResponseProvider {

    /**
     * Executes the conversational completion against the configured provider.
     *
     * @param request           the validated incoming client chat request
     * @param systemInstruction the formatted system instruction policy
     * @param requestId         the unique tracking request ID
     * @return the structured assistant chat response
     */
    AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId
    );

    /**
     * Indicates whether the provider is active and ready to service requests.
     */
    boolean isAvailable();
}
