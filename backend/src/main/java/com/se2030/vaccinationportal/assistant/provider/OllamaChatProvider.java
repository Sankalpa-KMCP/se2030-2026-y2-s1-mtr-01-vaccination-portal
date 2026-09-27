package com.se2030.vaccinationportal.assistant.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantActionDto;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantMessageDto;
import com.se2030.vaccinationportal.assistant.exception.AssistantTimeoutException;
import com.se2030.vaccinationportal.assistant.policy.AssistantRefusalPolicy;
import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Local Ollama implementation of the assistant provider SPI.
 *
 * <p>The provider sends prompts only to the configured loopback Ollama endpoint and requests a
 * strict JSON response. The same server-side policy, ownership context, and route authorization
 * used by the OpenAI provider are applied before and after model generation.</p>
 */
public class OllamaChatProvider implements AiResponseProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatProvider.class);

    private static final int MAX_ANSWER_CHARACTERS = 2000;
    private static final int MAX_SUGGESTED_ACTIONS = 3;
    private static final int MAX_ACTION_LABEL_LENGTH = 100;
    private static final String OPERATIONAL_FALLBACK =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;

    private final AssistantProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OllamaChatProvider(AssistantProperties properties) {
        this(properties, createRestClient(properties), new ObjectMapper());
    }

    OllamaChatProvider(AssistantProperties properties, RestClient restClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    private static RestClient createRestClient(AssistantProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getTimeout());

        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId,
            Set<String> callerAuthorities
    ) {
        try {
            String upstreamBody = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createPayload(request, systemInstruction).toString())
                    .retrieve()
                    .body(String.class);

            JsonNode upstreamResponse = upstreamBody == null
                    ? null
                    : objectMapper.readTree(upstreamBody);
            String rawOutput = upstreamResponse == null
                    ? ""
                    : upstreamResponse.path("message").path("content").asText("");
            return parseStructuredOutput(rawOutput, requestId, callerAuthorities);
        } catch (RestClientResponseException ex) {
            log.warn("Local assistant provider rejected request: requestId={}, upstreamStatus={}",
                    requestId, ex.getStatusCode().value());
            return fallback(requestId);
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                log.warn("Local assistant provider timeout occurred for requestId: {}", requestId);
                throw new AssistantTimeoutException("Assistant provider request timed out", ex);
            }
            log.warn("Local assistant provider is unavailable for requestId: {}", requestId);
            return fallback(requestId);
        } catch (Exception ex) {
            log.warn("Local assistant provider error occurred: requestId={}, exceptionClass={}",
                    requestId, ex.getClass().getSimpleName());
            return fallback(requestId);
        }
    }

    private ObjectNode createPayload(AssistantChatRequest request, String systemInstruction) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.getModel());
        payload.put("stream", false);
        payload.put("think", false);
        payload.set("format", responseSchema());

        ObjectNode options = payload.putObject("options");
        options.put("temperature", 0);
        options.put("num_predict", properties.getMaxOutputTokens());

        ArrayNode messages = payload.putArray("messages");
        addMessage(messages, "system", systemInstruction +
                "\nReturn only JSON matching the supplied response schema.");

        if (request.history() != null) {
            for (AssistantMessageDto historyItem : request.history()) {
                String role = AssistantMessageDto.ROLE_ASSISTANT.equalsIgnoreCase(historyItem.role())
                        ? "assistant"
                        : "user";
                addMessage(messages, role, historyItem.content());
            }
        }
        addMessage(messages, "user", request.message());
        return payload;
    }

    private void addMessage(ArrayNode messages, String role, String content) {
        ObjectNode message = messages.addObject();
        message.put("role", role);
        message.put("content", content);
    }

    private ObjectNode responseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode propertiesNode = schema.putObject("properties");
        propertiesNode.putObject("answer").put("type", "string");

        ObjectNode actions = propertiesNode.putObject("suggestedActions");
        actions.put("type", "array");
        ObjectNode actionItem = actions.putObject("items");
        actionItem.put("type", "object");
        ObjectNode actionProperties = actionItem.putObject("properties");
        actionProperties.putObject("label").put("type", "string");
        actionProperties.putObject("path").put("type", "string");
        actionItem.putArray("required").add("label").add("path");
        actionItem.put("additionalProperties", false);

        schema.putArray("required").add("answer").add("suggestedActions");
        schema.put("additionalProperties", false);
        return schema;
    }

    AssistantChatResponse parseStructuredOutput(
            String rawOutput,
            String requestId,
            Set<String> callerAuthorities
    ) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return fallback(requestId);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawOutput);
        } catch (Exception ex) {
            log.warn("Local assistant response was not valid JSON for requestId: {}", requestId);
            return fallback(requestId);
        }

        if (!root.hasNonNull("answer") || !root.get("answer").isTextual()
                || root.get("answer").asText().isBlank()) {
            return fallback(requestId);
        }

        String answer = OpenAiResponsesProvider.safeTruncate(
                root.get("answer").asText().trim(),
                MAX_ANSWER_CHARACTERS
        );
        List<AssistantActionDto> actions = new ArrayList<>();

        JsonNode suggestedActions = root.get("suggestedActions");
        if (suggestedActions != null && suggestedActions.isArray()) {
            for (JsonNode action : suggestedActions) {
                if (actions.size() >= MAX_SUGGESTED_ACTIONS) {
                    break;
                }
                String label = action.path("label").asText("").trim();
                String path = action.path("path").asText("").trim();
                if (label.isBlank() || label.length() > MAX_ACTION_LABEL_LENGTH) {
                    continue;
                }
                if (AssistantRouteCatalog.isAuthorizedRoute(path, callerAuthorities)) {
                    actions.add(new AssistantActionDto(label, path));
                }
            }
        }

        return new AssistantChatResponse(answer, actions, requestId);
    }

    private AssistantChatResponse fallback(String requestId) {
        return new AssistantChatResponse(OPERATIONAL_FALLBACK, Collections.emptyList(), requestId);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.http.HttpTimeoutException
                    || (current.getMessage() != null
                    && current.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfiguredForLiveProvider();
    }
}
