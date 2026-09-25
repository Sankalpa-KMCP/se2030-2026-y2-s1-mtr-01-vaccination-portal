package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.dto.AssistantActionDto;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.exception.AssistantDisabledException;
import com.se2030.vaccinationportal.assistant.exception.AssistantProviderException;
import com.se2030.vaccinationportal.assistant.exception.AssistantTimeoutException;
import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import com.se2030.vaccinationportal.assistant.provider.AiResponseProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Deterministic fake provider strictly used in test suites.
 * Guarantees zero outbound network calls to external OpenAI endpoints.
 */
public class DeterministicFakeAiResponseProvider implements AiResponseProvider {

    private boolean available = true;
    private boolean simulateTimeout = false;
    private boolean simulateProviderError = false;

    private String cannedAnswer = "This is a deterministic assistant answer explaining portal navigation.";
    private List<AssistantActionDto> cannedActions = List.of(
            new AssistantActionDto("Open appointment booking", "/app/appointments/book")
    );

    private final List<AssistantChatRequest> recordedRequests = new ArrayList<>();
    private final List<String> recordedSystemInstructions = new ArrayList<>();
    private final List<Set<String>> recordedCallerAuthorities = new ArrayList<>();

    @Override
    public AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId,
            Set<String> callerAuthorities
    ) {
        if (!available) {
            throw new AssistantDisabledException("The AI assistant is currently disabled or unavailable.");
        }
        if (simulateTimeout) {
            throw new AssistantTimeoutException("Simulated provider timeout");
        }
        if (simulateProviderError) {
            throw new AssistantProviderException("Simulated upstream provider error");
        }

        recordedRequests.add(request);
        recordedSystemInstructions.add(systemInstruction);
        recordedCallerAuthorities.add(callerAuthorities);

        // Filter suggested actions through caller authorization
        List<AssistantActionDto> authorizedActions = new ArrayList<>();
        if (cannedActions != null) {
            for (AssistantActionDto action : cannedActions) {
                if (AssistantRouteCatalog.isAuthorizedRoute(action.path(), callerAuthorities)) {
                    authorizedActions.add(action);
                }
            }
        }

        return new AssistantChatResponse(cannedAnswer, authorizedActions, requestId);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    public void setSimulateProviderError(boolean simulateProviderError) {
        this.simulateProviderError = simulateProviderError;
    }

    public void setCannedAnswer(String cannedAnswer) {
        this.cannedAnswer = cannedAnswer;
    }

    public void setCannedActions(List<AssistantActionDto> cannedActions) {
        this.cannedActions = cannedActions != null ? cannedActions : Collections.emptyList();
    }

    public List<AssistantChatRequest> getRecordedRequests() {
        return recordedRequests;
    }

    public List<String> getRecordedSystemInstructions() {
        return recordedSystemInstructions;
    }

    public List<Set<String>> getRecordedCallerAuthorities() {
        return recordedCallerAuthorities;
    }

    public void reset() {
        recordedRequests.clear();
        recordedSystemInstructions.clear();
        recordedCallerAuthorities.clear();
        available = true;
        simulateTimeout = false;
        simulateProviderError = false;
        cannedAnswer = "This is a deterministic assistant answer explaining portal navigation.";
        cannedActions = List.of(new AssistantActionDto("Open appointment booking", "/app/appointments/book"));
    }
}
