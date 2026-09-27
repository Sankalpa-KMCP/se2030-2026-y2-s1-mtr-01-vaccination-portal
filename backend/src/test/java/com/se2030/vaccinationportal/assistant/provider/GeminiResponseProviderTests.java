package com.se2030.vaccinationportal.assistant.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponsePromptFeedback;
import com.google.genai.types.Part;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantMessageDto;
import com.se2030.vaccinationportal.assistant.exception.AssistantTimeoutException;
import com.se2030.vaccinationportal.assistant.policy.AssistantRefusalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeminiResponseProvider Unit & Integration Tests")
class GeminiResponseProviderTests {

    private AssistantProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGoogleApiKey("AIzaSyTestGoogleKey123");
        properties.setTimeout(Duration.ofSeconds(2));
        properties.setMaxOutputTokens(500);

        objectMapper = new ObjectMapper();
    }

    private GenerateContentResponse createMockResponseWithText(String innerText) {
        String json = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": %s
                          }
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """.formatted(objectMapper.valueToTree(innerText).toString());
        return GenerateContentResponse.fromJson(json);
    }

    @Test
    @DisplayName("Scenario 1: Valid structured JSON response generates parsed answer and authorized actions")
    void testValidStructuredJsonResponse() {
        String structuredJson = """
                {
                  "answer": "You can book an appointment through the portal booking screen.",
                  "suggestedActions": [
                    {"label": "Book Appointment", "path": "/app/appointments/book"}
                  ]
                }
                """;

        GenerateContentResponse sdkResponse = createMockResponseWithText(structuredJson);

        GeminiResponseProvider.GeminiContentGenerator generator = (model, contents, config) ->
                CompletableFuture.completedFuture(sdkResponse);

        GeminiResponseProvider provider = new GeminiResponseProvider(properties, generator, objectMapper);

        AssistantChatRequest request = new AssistantChatRequest("How do I book?", Collections.emptyList());
        AssistantChatResponse response = provider.generateResponse(
                request,
                "System instructions",
                "req-gemini-1",
                Set.of("ROLE_CITIZEN")
        );

        assertNotNull(response);
        assertEquals("You can book an appointment through the portal booking screen.", response.answer());
        assertEquals("req-gemini-1", response.requestId());
        assertEquals(1, response.suggestedActions().size());
        assertEquals("Book Appointment", response.suggestedActions().get(0).label());
        assertEquals("/app/appointments/book", response.suggestedActions().get(0).path());
    }

    @Test
    @DisplayName("Scenario 2: Provider selection with gemini resolves provider correctly")
    void testProviderSelectionWithGemini() {
        assertEquals("gemini", properties.getProvider());
        assertTrue(properties.isConfiguredForLiveProvider());
        assertEquals("gemini-flash-latest", properties.getEffectiveModel());
    }

    @Test
    @DisplayName("Scenario 3: API key fallback resolution from googleApiKey to apiKey")
    void testApiKeyFallbackResolution() {
        AssistantProperties props = new AssistantProperties();
        props.setEnabled(true);
        props.setProvider("gemini");

        // Primary: googleApiKey
        props.setGoogleApiKey("AIzaSyPrimary");
        props.setApiKey("AIzaSySecondary");
        assertEquals("AIzaSyPrimary", props.getEffectiveGoogleApiKey());

        // Fallback: apiKey when googleApiKey is empty
        props.setGoogleApiKey("");
        assertEquals("AIzaSySecondary", props.getEffectiveGoogleApiKey());

        // Both empty
        props.setApiKey("");
        assertEquals("", props.getEffectiveGoogleApiKey());
        assertFalse(props.isConfiguredForLiveProvider());
    }

    @Test
    @DisplayName("Scenario 4: Missing key handling fails closed")
    void testMissingKeyHandlingFailsClosed() {
        AssistantProperties props = new AssistantProperties();
        props.setEnabled(true);
        props.setProvider("gemini");
        props.setGoogleApiKey("");
        props.setApiKey("");

        assertFalse(props.isConfiguredForLiveProvider());
        assertThrows(IllegalArgumentException.class, () -> new GeminiResponseProvider(props));
    }

    @Test
    @DisplayName("Scenario 5: Empty or blank response returns operational fallback")
    void testEmptyOrBlankResponseReturnsFallback() {
        GenerateContentResponse blankResponse = createMockResponseWithText("   ");

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(blankResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("hello", Collections.emptyList()),
                "sys",
                "req-blank",
                Collections.emptySet()
        );

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, response.answer());
        assertTrue(response.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Scenario 6: Malformed JSON response returns operational fallback")
    void testMalformedJsonResponseReturnsFallback() {
        GenerateContentResponse malformedResponse = createMockResponseWithText("{not-valid-json: true");

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(malformedResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("hello", Collections.emptyList()),
                "sys",
                "req-malformed",
                Collections.emptySet()
        );

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, response.answer());
        assertTrue(response.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Scenario 7: Missing required answer field returns operational fallback")
    void testMissingRequiredAnswerFieldReturnsFallback() {
        GenerateContentResponse missingAnswerResponse = createMockResponseWithText("{\"suggestedActions\": []}");

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(missingAnswerResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("hello", Collections.emptyList()),
                "sys",
                "req-no-answer",
                Collections.emptySet()
        );

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, response.answer());
    }

    @Test
    @DisplayName("Scenario 8: Suggested action count limit enforcement truncates to max 3")
    void testSuggestedActionsMaxThreeEnforced() {
        String fourActionsJson = """
                {
                  "answer": "Public resources overview",
                  "suggestedActions": [
                    {"label": "How It Works", "path": "/how-it-works"},
                    {"label": "About Portal", "path": "/about"},
                    {"label": "Verify Certificate", "path": "/verify-certificate"},
                    {"label": "Citizen Login", "path": "/login"}
                  ]
                }
                """;

        GenerateContentResponse sdkResponse = createMockResponseWithText(fourActionsJson);
        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(sdkResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("info", Collections.emptyList()),
                "sys",
                "req-max-actions",
                Collections.emptySet()
        );

        assertEquals(3, response.suggestedActions().size());
        assertEquals("/how-it-works", response.suggestedActions().get(0).path());
        assertEquals("/about", response.suggestedActions().get(1).path());
        assertEquals("/verify-certificate", response.suggestedActions().get(2).path());
    }

    @Test
    @DisplayName("Scenario 9: Suggested action label length limit enforcement ignores labels > 100 chars")
    void testSuggestedActionLabelLengthEnforcement() {
        String longLabel = "A".repeat(105);
        String json = """
                {
                  "answer": "Test answer",
                  "suggestedActions": [
                    {"label": "%s", "path": "/how-it-works"},
                    {"label": "Valid Label", "path": "/about"}
                  ]
                }
                """.formatted(longLabel);

        GenerateContentResponse sdkResponse = createMockResponseWithText(json);
        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(sdkResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("info", Collections.emptyList()),
                "sys",
                "req-label-len",
                Collections.emptySet()
        );

        assertEquals(1, response.suggestedActions().size());
        assertEquals("Valid Label", response.suggestedActions().get(0).label());
        assertEquals("/about", response.suggestedActions().get(0).path());
    }

    @Test
    @DisplayName("Scenario 10: Role-based route authorization filtering on suggested actions")
    void testRoleBasedRouteAuthorizationFiltering() {
        String json = """
                {
                  "answer": "Here are options",
                  "suggestedActions": [
                    {"label": "Staff Request Queue", "path": "/app/admin/staff-requests"},
                    {"label": "How It Works", "path": "/how-it-works"}
                  ]
                }
                """;

        GenerateContentResponse sdkResponse = createMockResponseWithText(json);
        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(sdkResponse),
                objectMapper
        );

        // Anonymous caller: /app/admin/staff-requests is stripped, /how-it-works is kept
        AssistantChatResponse anonResponse = provider.generateResponse(
                new AssistantChatRequest("info", Collections.emptyList()),
                "sys",
                "req-auth-1",
                Collections.emptySet()
        );

        assertEquals(1, anonResponse.suggestedActions().size());
        assertEquals("/how-it-works", anonResponse.suggestedActions().get(0).path());

        // Admin caller: both routes authorized
        AssistantChatResponse adminResponse = provider.generateResponse(
                new AssistantChatRequest("info", Collections.emptyList()),
                "sys",
                "req-auth-2",
                Set.of("ROLE_ADMIN")
        );

        assertEquals(2, adminResponse.suggestedActions().size());
    }

    @Test
    @DisplayName("Scenario 11: Completely unauthorized routes stripped from output")
    void testUnauthorizedRoutesStripped() {
        String json = """
                {
                  "answer": "Check this",
                  "suggestedActions": [
                    {"label": "Secret Endpoint", "path": "/api/internal/secret"},
                    {"label": "External Link", "path": "https://google.com"}
                  ]
                }
                """;

        GenerateContentResponse sdkResponse = createMockResponseWithText(json);
        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(sdkResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("info", Collections.emptyList()),
                "sys",
                "req-unauth",
                Set.of("ROLE_SUPER_ADMIN")
        );

        assertTrue(response.suggestedActions().isEmpty(), "Non-catalog routes must be stripped");
    }

    @Test
    @DisplayName("Scenario 12: Character truncation at 2000 characters")
    void testCharacterTruncationAt2000Characters() {
        String longText = "X".repeat(2500);
        String json = """
                {
                  "answer": "%s",
                  "suggestedActions": []
                }
                """.formatted(longText);

        GenerateContentResponse sdkResponse = createMockResponseWithText(json);
        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(sdkResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("long", Collections.emptyList()),
                "sys",
                "req-trunc",
                Collections.emptySet()
        );

        assertThat(response.answer().length()).isLessThanOrEqualTo(2003);
        assertThat(response.answer()).endsWith("...");
    }

    @Test
    @DisplayName("Scenario 13: Character truncation preserves multi-byte UTF-16 surrogate pairs")
    void testCharacterTruncationWithSurrogatePairPreservation() {
        // Emoji 💉 is represented as high surrogate \uD83D and low surrogate \uDC89
        String prefix = "A".repeat(1999);
        String textWithSurrogate = prefix + "💉" + "BBBB";

        String json = """
                {
                  "answer": %s,
                  "suggestedActions": []
                }
                """.formatted(objectMapper.valueToTree(textWithSurrogate).toString());

        GenerateContentResponse sdkResponse = createMockResponseWithText(json);
        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(sdkResponse),
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("emoji", Collections.emptyList()),
                "sys",
                "req-surrogate",
                Collections.emptySet()
        );

        assertNotNull(response.answer());
        assertThat(response.answer()).endsWith("...");
        // Verify surrogate pair is not split
        for (int i = 0; i < response.answer().length(); i++) {
            char c = response.answer().charAt(i);
            if (Character.isHighSurrogate(c)) {
                assertTrue(i + 1 < response.answer().length(), "High surrogate must be followed by low surrogate");
                assertTrue(Character.isLowSurrogate(response.answer().charAt(i + 1)));
            }
        }
    }

    @Test
    @DisplayName("Scenario 14: System instruction passed to configuration")
    void testSystemInstructionPassedToConfiguration() {
        AtomicReference<GenerateContentConfig> capturedConfig = new AtomicReference<>();

        GenerateContentResponse sdkResponse = createMockResponseWithText("{\"answer\":\"ok\",\"suggestedActions\":[]}");

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> {
                    capturedConfig.set(config);
                    return CompletableFuture.completedFuture(sdkResponse);
                },
                objectMapper
        );

        provider.generateResponse(
                new AssistantChatRequest("test", Collections.emptyList()),
                "EXPLICIT_TEST_SYSTEM_INSTRUCTION",
                "req-sys",
                Collections.emptySet()
        );

        assertNotNull(capturedConfig.get());
        assertTrue(capturedConfig.get().systemInstruction().isPresent());
        Content sysContent = capturedConfig.get().systemInstruction().get();
        assertThat(sysContent.parts().orElse(List.of()).get(0).text().orElse(""))
                .contains("EXPLICIT_TEST_SYSTEM_INSTRUCTION");
    }

    @Test
    @DisplayName("Scenario 15: Multi-turn conversation history role mapping (user and model)")
    void testMultiTurnConversationHistoryRoleMapping() {
        AtomicReference<List<Content>> capturedContents = new AtomicReference<>();

        GenerateContentResponse sdkResponse = createMockResponseWithText("{\"answer\":\"ok\",\"suggestedActions\":[]}");

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> {
                    capturedContents.set(contents);
                    return CompletableFuture.completedFuture(sdkResponse);
                },
                objectMapper
        );

        List<AssistantMessageDto> history = List.of(
                new AssistantMessageDto("user", "Hello"),
                new AssistantMessageDto("assistant", "Hi there! How can I help?"),
                new AssistantMessageDto("user", "Where can I get vaccinated?")
        );

        provider.generateResponse(
                new AssistantChatRequest("Tell me more", history),
                "sys",
                "req-hist",
                Collections.emptySet()
        );

        List<Content> contents = capturedContents.get();
        assertNotNull(contents);
        assertEquals(4, contents.size());

        // First message: user
        assertEquals("user", contents.get(0).role().orElse(""));
        assertEquals("Hello", contents.get(0).parts().orElse(List.of()).get(0).text().orElse(""));

        // Second message: assistant mapped to "model" for Gemini
        assertEquals("model", contents.get(1).role().orElse(""));
        assertEquals("Hi there! How can I help?", contents.get(1).parts().orElse(List.of()).get(0).text().orElse(""));

        // Third message: user
        assertEquals("user", contents.get(2).role().orElse(""));
        assertEquals("Where can I get vaccinated?", contents.get(2).parts().orElse(List.of()).get(0).text().orElse(""));

        // Fourth message: current user prompt
        assertEquals("user", contents.get(3).role().orElse(""));
        assertEquals("Tell me more", contents.get(3).parts().orElse(List.of()).get(0).text().orElse(""));
    }

    @Test
    @DisplayName("Scenario 16: Timeout handling throws AssistantTimeoutException and cancels future")
    void testTimeoutHandlingThrowsAssistantTimeoutException() {
        properties.setTimeout(Duration.ofMillis(100));

        CompletableFuture<GenerateContentResponse> hangingFuture = new CompletableFuture<>();

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> hangingFuture,
                objectMapper
        );

        assertThrows(AssistantTimeoutException.class, () -> provider.generateResponse(
                new AssistantChatRequest("test", Collections.emptyList()),
                "sys",
                "req-timeout",
                Collections.emptySet()
        ));

        assertTrue(hangingFuture.isCancelled(), "Timed-out future must be explicitly cancelled");
    }

    @Test
    @DisplayName("Scenario 17: Upstream error handling returns operational fallback without crashing")
    void testUpstreamErrorHandlingReturnsOperationalFallback() {
        CompletableFuture<GenerateContentResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Google API Service Unavailable"));

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> failedFuture,
                objectMapper
        );

        AssistantChatResponse response = provider.generateResponse(
                new AssistantChatRequest("test", Collections.emptyList()),
                "sys",
                "req-err",
                Collections.emptySet()
        );

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, response.answer());
        assertTrue(response.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Scenario 18: Model override via app.ai.model overrides default gemini-flash-latest")
    void testModelOverrideUsesConfiguredModel() {
        properties.setModel("gemini-2.5-pro");

        AtomicReference<String> capturedModel = new AtomicReference<>();
        GenerateContentResponse sdkResponse = createMockResponseWithText("{\"answer\":\"ok\",\"suggestedActions\":[]}");

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> {
                    capturedModel.set(model);
                    return CompletableFuture.completedFuture(sdkResponse);
                },
                objectMapper
        );

        provider.generateResponse(
                new AssistantChatRequest("test", Collections.emptyList()),
                "sys",
                "req-model",
                Collections.emptySet()
        );

        assertEquals("gemini-2.5-pro", capturedModel.get());
    }

    @Test
    @DisplayName("Scenario 19: Safety finish reason returns safe refusal fallback")
    void testSafetyFinishReasonReturnsSafeRefusalFallback() {
        String safetyJson = """
                {
                  "candidates": [
                    {
                      "finishReason": "SAFETY"
                    }
                  ]
                }
                """;
        GenerateContentResponse response = GenerateContentResponse.fromJson(safetyJson);

        GeminiResponseProvider provider = new GeminiResponseProvider(
                properties,
                (model, contents, config) -> CompletableFuture.completedFuture(response),
                objectMapper
        );

        AssistantChatResponse result = provider.generateResponse(
                new AssistantChatRequest("unsafe", Collections.emptyList()),
                "sys",
                "req-safety",
                Collections.emptySet()
        );

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, result.answer());
        assertTrue(result.suggestedActions().isEmpty());
    }
}
