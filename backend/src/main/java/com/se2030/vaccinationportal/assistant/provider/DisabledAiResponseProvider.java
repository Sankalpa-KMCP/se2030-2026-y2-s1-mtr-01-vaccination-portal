package com.se2030.vaccinationportal.assistant.provider;

import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.exception.AssistantDisabledException;

/**
 * Fallback provider used when AI is disabled or configuration (API key/model) is missing.
 */
public class DisabledAiResponseProvider implements AiResponseProvider {

    @Override
    public AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId
    ) {
        throw new AssistantDisabledException("The AI assistant is currently disabled or unavailable.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
