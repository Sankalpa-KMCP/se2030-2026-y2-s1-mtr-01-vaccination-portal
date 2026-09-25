package com.se2030.vaccinationportal.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputRefusal;
import com.openai.models.responses.ResponseOutputText;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.provider.OpenAiResponsesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("Provider refusal returns safe user-facing refusal and zero actions")
    void testProviderRefusalHandling() {
        Response response = createMockResponseWithRefusal("I cannot discuss this topic.");
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-1", Collections.emptySet());

        assertEquals(OpenAiResponsesProvider.SAFE_REFUSAL_MESSAGE, chatResponse.answer());
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Malformed JSON returns safe generic message and does not leak raw output")
    void testMalformedJsonHandling() {
        Response response = createMockResponseWithText("This is totally not JSON at all!");
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-2", Collections.emptySet());

        assertEquals(OpenAiResponsesProvider.SAFE_UNSTRUCTURED_FALLBACK, chatResponse.answer());
        assertFalse(chatResponse.answer().contains("not JSON"));
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Missing answer field returns safe generic fallback")
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

        assertEquals(OpenAiResponsesProvider.SAFE_UNSTRUCTURED_FALLBACK, chatResponse.answer());
        assertTrue(chatResponse.suggestedActions().isEmpty());
    }

    @Test
    @DisplayName("Oversized answer is truncated to MAX_ANSWER_CHARACTERS")
    void testOversizedAnswerTruncated() {
        String longText = "A".repeat(2500);
        String json = String.format("{\"answer\":\"%s\",\"suggestedActions\":[]}", longText);

        Response response = createMockResponseWithText(json);
        AssistantChatResponse chatResponse = provider.parseResponse(response, "req-4", Collections.emptySet());

        assertTrue(chatResponse.answer().length() <= OpenAiResponsesProvider.MAX_ANSWER_CHARACTERS + 3);
        assertTrue(chatResponse.answer().endsWith("..."));
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
}
