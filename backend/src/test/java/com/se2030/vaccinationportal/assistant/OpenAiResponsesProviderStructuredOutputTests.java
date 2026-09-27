package com.se2030.vaccinationportal.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputRefusal;
import com.openai.models.responses.ResponseOutputText;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantMessageDto;
import com.se2030.vaccinationportal.assistant.policy.AssistantRefusalPolicy;
import com.se2030.vaccinationportal.assistant.provider.OpenAiResponsesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiResponsesProvider Structured Output Tests")
class OpenAiResponsesProviderStructuredOutputTests {

    private OpenAiResponsesProvider provider;
    private OpenAIClient mockClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("test-model");

        mockClient = mock(OpenAIClient.class);
        provider = new OpenAiResponsesProvider(properties, mockClient, objectMapper);
    }

    private Response createMockResponseWithText(String outputText) {
        ResponseOutputText textObj = mock(ResponseOutputText.class);
        when(textObj.text()).thenReturn(outputText);

        ResponseOutputMessage.Content content = mock(ResponseOutputMessage.Content.class);
        when(content.isOutputText()).thenReturn(true);
        when(content.asOutputText()).thenReturn(textObj);
        when(content.isRefusal()).thenReturn(false);

        ResponseOutputMessage msg = mock(ResponseOutputMessage.class);
        when(msg.content()).thenReturn(List.of(content));

        ResponseOutputItem item = mock(ResponseOutputItem.class);
        when(item.message()).thenReturn(java.util.Optional.of(msg));

        Response response = mock(Response.class);
        when(response.output()).thenReturn(List.of(item));

        return response;
    }

    private Response createMockResponseWithRefusal(String refusalText) {
        ResponseOutputRefusal refusalObj = mock(ResponseOutputRefusal.class);
        when(refusalObj.refusal()).thenReturn(refusalText);

        ResponseOutputMessage.Content content = mock(ResponseOutputMessage.Content.class);
        when(content.isOutputText()).thenReturn(false);
        when(content.isRefusal()).thenReturn(true);
        when(content.asRefusal()).thenReturn(refusalObj);

        ResponseOutputMessage msg = mock(ResponseOutputMessage.class);
        when(msg.content()).thenReturn(List.of(content));

        ResponseOutputItem item = mock(ResponseOutputItem.class);
        when(item.message()).thenReturn(java.util.Optional.of(msg));

        Response response = mock(Response.class);
        when(response.output()).thenReturn(List.of(item));

        return response;
    }

    @Test
    @DisplayName("Provider refusal returns safe user-facing operational fallback and zero actions")
    void testProviderRefusalHandling() {
        Response response = createMockResponseWithRefusal("I cannot discuss this topic.");
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-1", Collections.emptySet());

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, chatResponse.answer());
        assertThat(chatResponse.answer()).doesNotContain("emergency");
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Malformed JSON returns safe operational fallback and does not leak raw output or mention emergency")
    void testMalformedJsonHandling() {
        Response response = createMockResponseWithText("This is totally not JSON at all!");
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-2", Collections.emptySet());

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, chatResponse.answer());
        assertFalse(chatResponse.answer().contains("not JSON"));
        assertThat(chatResponse.answer()).doesNotContain("emergency");
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Missing answer field returns safe operational fallback")
    void testMissingAnswerField() {
        String jsonWithoutAnswer = """
                {
                  "suggestedActions": [
                    { "label": "Home", "path": "/" }
                  ]
                }
                """;
        Response response = createMockResponseWithText(jsonWithoutAnswer);
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-3", Collections.emptySet());

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, chatResponse.answer());
        assertThat(chatResponse.answer()).doesNotContain("emergency");
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Provider outage when calling OpenAI returns operational fallback without emergency guidance")
    void testProviderOutageReturnsOperationalFallback() {
        com.openai.services.blocking.ResponseService responseService = mock(com.openai.services.blocking.ResponseService.class);
        when(mockClient.responses()).thenReturn(responseService);
        when(responseService.create(any(com.openai.models.responses.ResponseCreateParams.class)))
                .thenThrow(new RuntimeException("OpenAI HTTP 503 Service Unavailable"));


        AssistantChatRequest request = new AssistantChatRequest("Where are centers?", Collections.emptyList(), null);
        AssistantChatResponse chatResponse = provider.generateResponse(
                request,
                "system instruction",
                "req-outage-test",
                Collections.emptySet()
        );

        assertEquals(AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE, chatResponse.answer());
        assertThat(chatResponse.answer()).doesNotContain("emergency");
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Oversized answer is truncated to MAX_ANSWER_CHARACTERS (1500) and fits AssistantMessageDto constraint")
    void testOversizedAnswerTruncated() {
        String longText = "A".repeat(2500);
        String json = String.format("{\"answer\":\"%s\",\"suggestedActions\":[]}", longText);

        Response response = createMockResponseWithText(json);
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-4", Collections.emptySet());

        assertTrue(chatResponse.answer().length() <= OpenAiResponsesProvider.MAX_ANSWER_CHARACTERS);
        assertTrue(chatResponse.answer().endsWith("..."));

        // Prove valid next-turn history submission
        AssistantMessageDto historyItem = new AssistantMessageDto("assistant", chatResponse.answer());
        assertEquals(chatResponse.answer(), historyItem.content());
    }

    @Test
    @DisplayName("Answer truncation does not split Unicode surrogate pairs at 1500 character boundary")
    void testAnswerTruncationPreservesSurrogatePairs() {
        // Build a string with an emoji (surrogate pair) right at the boundary targetIndex (1497)
        // 1496 ASCII characters + 😀 (2 chars in UTF-16: \uD83D\uDE00) + 100 extra chars
        String prefix = "X".repeat(1496);
        String emoji = "\uD83D\uDE00";
        String suffix = "Y".repeat(100);
        String fullAnswer = prefix + emoji + suffix;

        String truncated = OpenAiResponsesProvider.safeTruncate(fullAnswer, 1500);

        assertTrue(truncated.length() <= 1500);
        assertTrue(truncated.endsWith("..."));
        String withoutEllipsis = truncated.substring(0, truncated.length() - 3);

        // The length must be 1496 (stepped back to avoid splitting the surrogate pair)
        assertEquals(1496, withoutEllipsis.length());
        assertFalse(Character.isHighSurrogate(withoutEllipsis.charAt(withoutEllipsis.length() - 1)));
        assertFalse(Character.isLowSurrogate(withoutEllipsis.charAt(withoutEllipsis.length() - 1)));
    }

    @Test
    @DisplayName("Suggested actions exceeding limit are capped at MAX_SUGGESTED_ACTIONS (3)")
    void testExcessiveSuggestedActionsCapped() {
        String jsonWith5Actions = """
                {
                  "answer": "Here are public options.",
                  "suggestedActions": [
                    { "label": "Home", "path": "/" },
                    { "label": "How It Works", "path": "/how-it-works" },
                    { "label": "About", "path": "/about" },
                    { "label": "Verify", "path": "/verify-certificate" },
                    { "label": "Login", "path": "/login" }
                  ]
                }
                """;

        Response response = createMockResponseWithText(jsonWith5Actions);
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-5", Collections.emptySet());

        assertEquals(3, chatResponse.suggestedActions().size());
        assertEquals("/", chatResponse.suggestedActions().get(0).path());
        assertEquals("/how-it-works", chatResponse.suggestedActions().get(1).path());
        assertEquals("/about", chatResponse.suggestedActions().get(2).path());
    }

    @Test
    @DisplayName("Suggested actions filter out routes unauthorized for the caller")
    void testCallerRouteAuthorizationFiltering() {
        String jsonWithMixedRoutes = """
                {
                  "answer": "Here is what you requested.",
                  "suggestedActions": [
                    { "label": "Public About", "path": "/about" },
                    { "label": "Citizen Bookings", "path": "/app/appointments/book" },
                    { "label": "Admin Staff Requests", "path": "/app/admin/staff-requests" }
                  ]
                }
                """;

        // Anonymous caller: should ONLY receive "/about"
        Response respAnon = createMockResponseWithText(jsonWithMixedRoutes);
        AssistantChatResponse chatAnon = provider.parseResponse(respAnon, "req-anon", Collections.emptySet());
        assertEquals(1, chatAnon.suggestedActions().size());
        assertEquals("/about", chatAnon.suggestedActions().get(0).path());

        // Citizen caller: should receive "/about" and "/app/appointments/book", but NOT "/app/admin/staff-requests"
        Response respCit = createMockResponseWithText(jsonWithMixedRoutes);
        AssistantChatResponse chatCit = provider.parseResponse(respCit, "req-cit", Set.of("ROLE_CITIZEN"));
        assertEquals(2, chatCit.suggestedActions().size());
        assertEquals("/about", chatCit.suggestedActions().get(0).path());
        assertEquals("/app/appointments/book", chatCit.suggestedActions().get(1).path());

        // Admin caller: should receive "/about" and "/app/admin/staff-requests", but NOT citizen private bookings
        Response respAdmin = createMockResponseWithText(jsonWithMixedRoutes);
        AssistantChatResponse chatAdmin = provider.parseResponse(respAdmin, "req-admin", Set.of("ROLE_ADMIN"));
        assertEquals(2, chatAdmin.suggestedActions().size());
        assertEquals("/about", chatAdmin.suggestedActions().get(0).path());
        assertEquals("/app/admin/staff-requests", chatAdmin.suggestedActions().get(1).path());
    }

    @Test
    @DisplayName("Screening routes in suggested actions are stripped for Admin but retained for Healthcare Officer")
    void testScreeningRouteStrippedForAdminRetainedForHealthcareOfficer() {
        String jsonWithScreening = """
                {
                  "answer": "Here is the screening queue.",
                  "suggestedActions": [
                    { "label": "Review Queue", "path": "/app/staff/screening/reviews" },
                    { "label": "Screening Questions", "path": "/app/screening/questions" }
                  ]
                }
                """;

        // Admin caller: screening routes must be stripped
        Response respAdmin = createMockResponseWithText(jsonWithScreening);
        AssistantChatResponse chatAdmin = provider.parseResponse(respAdmin, "req-admin-scr", Set.of("ROLE_ADMIN"));
        assertTrue(chatAdmin.suggestedActions().isEmpty(), "Screening routes must be stripped for ROLE_ADMIN");

        // Healthcare Officer caller: screening routes must be retained
        Response respHco = createMockResponseWithText(jsonWithScreening);
        AssistantChatResponse chatHco = provider.parseResponse(respHco, "req-hco-scr", Set.of("ROLE_HEALTHCARE_OFFICER"));
        assertEquals(2, chatHco.suggestedActions().size(), "Screening routes must be retained for ROLE_HEALTHCARE_OFFICER");
        assertEquals("/app/staff/screening/reviews", chatHco.suggestedActions().get(0).path());
        assertEquals("/app/screening/questions", chatHco.suggestedActions().get(1).path());
    }
}
