package com.se2030.vaccinationportal.assistant.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaChatProviderTests {

    private AssistantProperties properties;
    private MockRestServiceServer server;
    private OllamaChatProvider provider;

    @BeforeEach
    void setUp() {
        properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("ollama");
        properties.setModel("qwen3:4b");

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OllamaChatProvider(properties, builder.build(), new ObjectMapper());
    }

    @Test
    void sendsNonStreamingStructuredChatRequestAndParsesResponse() {
        server.expect(once(), requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("qwen3:4b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format.type").value("object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("Earlier question"))
                .andExpect(jsonPath("$.messages[2].content").value("How do I register?"))
                .andRespond(withSuccess("""
                        {
                          "message": {
                            "role": "assistant",
                            "content": "{\\\"answer\\\":\\\"Create a citizen account.\\\",\\\"suggestedActions\\\":[{\\\"label\\\":\\\"Register\\\",\\\"path\\\":\\\"/register/citizen\\\"}]}"
                          },
                          "done": true
                        }
                        """, MediaType.APPLICATION_JSON));

        AssistantChatRequest request = new AssistantChatRequest(
                "How do I register?",
                List.of(new AssistantMessageDto("user", "Earlier question")),
                "/"
        );

        AssistantChatResponse response = provider.generateResponse(
                request,
                "Portal policy",
                "request-1",
                Set.of()
        );

        assertThat(response.answer()).isEqualTo("Create a citizen account.");
        assertThat(response.suggestedActions()).hasSize(1);
        assertThat(response.suggestedActions().getFirst().path()).isEqualTo("/register/citizen");
        server.verify();
    }

    @Test
    void filtersRoutesThatCallerIsNotAuthorizedToUse() {
        String output = """
                {"answer":"Use the available portal page.","suggestedActions":[
                  {"label":"Register","path":"/register/citizen"},
                  {"label":"Inventory","path":"/app/inventory"},
                  {"label":"External","path":"https://example.com"}
                ]}
                """;

        AssistantChatResponse response = provider.parseStructuredOutput(output, "request-2", Set.of());

        assertThat(response.suggestedActions())
                .extracting(action -> action.path())
                .containsExactly("/register/citizen");
    }

    @Test
    void malformedModelOutputFailsClosed() {
        AssistantChatResponse response = provider.parseStructuredOutput(
                "This is not JSON",
                "request-3",
                Set.of()
        );

        assertThat(response.answer()).isEqualTo(OpenAiResponsesProvider.OPERATIONAL_FALLBACK);
        assertThat(response.suggestedActions()).isEmpty();
    }

    @Test
    void truncatesAnswersLongerThan1500Characters() {
        String longText = "A".repeat(1650);
        String output = String.format("{\"answer\":\"%s\",\"suggestedActions\":[]}", longText);

        AssistantChatResponse response = provider.parseStructuredOutput(output, "request-4", Set.of());

        assertThat(response.answer().length()).isLessThanOrEqualTo(1500);
        assertThat(response.answer()).endsWith("...");

        AssistantMessageDto history = new AssistantMessageDto("assistant", response.answer());
        assertThat(history.content()).isEqualTo(response.answer());
    }
}
