package com.se2030.vaccinationportal.assistant.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextConfig;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseTextConfig;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantActionDto;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantMessageDto;
import com.se2030.vaccinationportal.assistant.exception.AssistantProviderException;
import com.se2030.vaccinationportal.assistant.exception.AssistantTimeoutException;
import com.se2030.vaccinationportal.assistant.policy.AssistantRefusalPolicy;
import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Live OpenAI implementation utilizing the official OpenAI Java SDK and Responses API (/v1/responses).
 * Enforces strict structured output with JSON schema and caller-aware route authorization.
 * Never exposes credentials, prompts, or upstream diagnostics to callers.
 */
public class OpenAiResponsesProvider implements AiResponseProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiResponsesProvider.class);

    public static final int MAX_ANSWER_CHARACTERS = 2000;
    public static final int MAX_SUGGESTED_ACTIONS = 3;
    public static final int MAX_ACTION_LABEL_LENGTH = 100;

    public static final String SAFE_REFUSAL_MESSAGE =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;

    public static final String SAFE_UNSTRUCTURED_FALLBACK =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;

    public static final String OPERATIONAL_FALLBACK =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;

    private final AssistantProperties properties;
    private final OpenAIClient openAiClient;
    private final ObjectMapper objectMapper;
    private final ResponseFormatTextConfig structuredFormatConfig;

    public OpenAiResponsesProvider(AssistantProperties properties) {
        this(properties, createDefaultClient(properties), new ObjectMapper());
    }

    public OpenAiResponsesProvider(AssistantProperties properties, OpenAIClient openAiClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.structuredFormatConfig = createStrictJsonSchemaFormat();
    }

    private static OpenAIClient createDefaultClient(AssistantProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .timeout(properties.getTimeout())
                .build();
    }

    private static ResponseFormatTextConfig createStrictJsonSchemaFormat() {
        Map<String, Object> schemaMap = Map.of(
                "type", "object",
                "properties", Map.of(
                        "answer", Map.of(
                                "type", "string",
                                "description", "Plain text response to the user."
                        ),
                        "suggestedActions", Map.of(
                                "type", "array",
                                "description", "List of allowed internal navigation routes.",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "label", Map.of("type", "string"),
                                                "path", Map.of("type", "string")
                                        ),
                                        "required", List.of("label", "path"),
                                        "additionalProperties", false
                                )
                        )
                ),
                "required", List.of("answer", "suggestedActions"),
                "additionalProperties", false
        );

        Map<String, JsonValue> jsonValueMap = schemaMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> JsonValue.from(e.getValue())));

        ResponseFormatTextJsonSchemaConfig.Schema schema = ResponseFormatTextJsonSchemaConfig.Schema.builder()
                .putAllAdditionalProperties(jsonValueMap)
                .build();

        ResponseFormatTextJsonSchemaConfig jsonSchemaConfig = ResponseFormatTextJsonSchemaConfig.builder()
                .name("assistant_response")
                .strict(true)
                .schema(schema)
                .build();

        return ResponseFormatTextConfig.ofJsonSchema(jsonSchemaConfig);
    }

    @Override
    public AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId,
            Set<String> callerAuthorities
    ) {
        List<ResponseInputItem> inputItems = new ArrayList<>();

        if (request.history() != null) {
            for (AssistantMessageDto historyItem : request.history()) {
                EasyInputMessage.Role role = AssistantMessageDto.ROLE_ASSISTANT.equalsIgnoreCase(historyItem.role())
                        ? EasyInputMessage.Role.ASSISTANT
                        : EasyInputMessage.Role.USER;

                inputItems.add(ResponseInputItem.ofEasyInputMessage(
                        EasyInputMessage.builder()
                                .role(role)
                                .content(EasyInputMessage.Content.ofTextInput(historyItem.content()))
                                .build()
                ));
            }
        }

        // Add current user message
        inputItems.add(ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                        .role(EasyInputMessage.Role.USER)
                        .content(EasyInputMessage.Content.ofTextInput(request.message()))
                        .build()
        ));

        ResponseCreateParams createParams = ResponseCreateParams.builder()
                .model(properties.getModel())
                .instructions(systemInstruction)
                .input(ResponseCreateParams.Input.ofResponse(inputItems))
                .maxOutputTokens((long) properties.getMaxOutputTokens())
                .store(false)
                .text(ResponseTextConfig.builder()
                        .format(structuredFormatConfig)
                        .build())
                .build();

        try {
            Response response = openAiClient.responses().create(createParams);
            return parseResponse(response, requestId, callerAuthorities);
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                log.warn("Upstream assistant provider timeout occurred for requestId: {}", requestId);
                throw new AssistantTimeoutException("Assistant provider request timed out", ex);
            }
            log.error("Upstream assistant provider error occurred for requestId: {}", requestId);
            return new AssistantChatResponse(OPERATIONAL_FALLBACK, Collections.emptyList(), requestId);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfiguredForLiveProvider();
    }

    /**
     * Parses the response from OpenAI with strict validation and caller authorization.
     */
    public AssistantChatResponse parseResponse(Response response, String requestId, Set<String> callerAuthorities) {
        String rawOutput = "";
        boolean isRefusal = false;

        if (response != null && response.output() != null) {
            for (ResponseOutputItem item : response.output()) {
                if (item.message().isPresent()) {
                    ResponseOutputMessage message = item.message().get();
                    if (message.content() != null) {
                        for (ResponseOutputMessage.Content content : message.content()) {
                            if (content.isRefusal()) {
                                isRefusal = true;
                                break;
                            } else if (content.isOutputText()) {
                                rawOutput = content.asOutputText().text();
                                break;
                            }
                        }
                    }
                }
                if (isRefusal || !rawOutput.isEmpty()) {
                    break;
                }
            }
        }

        // 1. Check for provider refusal
        if (isRefusal) {
            return new AssistantChatResponse(SAFE_REFUSAL_MESSAGE, Collections.emptyList(), requestId);
        }

        if (rawOutput.isBlank()) {
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        // 2. Strict JSON parsing
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(rawOutput);
        } catch (Exception parseException) {
            log.warn("Assistant response was not valid JSON for requestId: {}", requestId);
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        // 3. Answer extraction and character limit enforcement
        if (!rootNode.hasNonNull("answer") || !rootNode.get("answer").isTextual()
                || rootNode.get("answer").asText().isBlank()) {
            log.warn("Assistant response missing textual answer field for requestId: {}", requestId);
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        String answer = rootNode.get("answer").asText().trim();
        answer = safeTruncate(answer, MAX_ANSWER_CHARACTERS);

        // 4. Suggested actions extraction and caller-aware authorization filtering
        List<AssistantActionDto> actions = new ArrayList<>();
        if (rootNode.has("suggestedActions") && rootNode.get("suggestedActions").isArray()) {
            for (JsonNode actionNode : rootNode.get("suggestedActions")) {
                if (actions.size() >= MAX_SUGGESTED_ACTIONS) {
                    break;
                }


                String label = actionNode.hasNonNull("label") ? actionNode.get("label").asText().trim() : "";
                String path = actionNode.hasNonNull("path") ? actionNode.get("path").asText().trim() : "";

                // Enforce label length and non-blankness
                if (label.isBlank() || label.length() > MAX_ACTION_LABEL_LENGTH) {
                    continue;
                }

                // Strictly enforce route authorization for the caller
                if (AssistantRouteCatalog.isAuthorizedRoute(path, callerAuthorities)) {
                    actions.add(new AssistantActionDto(label, path));
                }
            }
        }

        return new AssistantChatResponse(answer, actions, requestId);
    }

    public static String safeTruncate(String text, int maxCharacters) {
        if (text == null || text.length() <= maxCharacters) {
            return text;
        }

        int targetIndex = maxCharacters;
        // Step back if index splits a UTF-16 surrogate pair
        if (Character.isHighSurrogate(text.charAt(targetIndex - 1))) {
            targetIndex--;
        } else if (Character.isLowSurrogate(text.charAt(targetIndex))) {
            targetIndex--;
        }

        if (targetIndex < 0) {
            targetIndex = 0;
        }

        return text.substring(0, targetIndex) + "...";
    }

    private boolean isTimeout(Throwable ex) {

        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof TimeoutException
                    || (current.getMessage() != null && current.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
