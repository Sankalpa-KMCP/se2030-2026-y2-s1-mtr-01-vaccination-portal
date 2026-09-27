package com.se2030.vaccinationportal.assistant.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Google Gemini implementation of the Assistant Provider SPI utilizing the official Google Gen AI Java SDK.
 * Enforces server-side JSON schema structured outputs, role-aware route filtering, and fail-closed timeout boundaries.
 * Strictly read-only: does not register or execute any mutating database tools or function calls.
 */
public class GeminiResponseProvider implements AiResponseProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GeminiResponseProvider.class);

    public static final int MAX_ANSWER_CHARACTERS = AiResponseProvider.MAX_ANSWER_CHARACTERS;
    public static final int MAX_SUGGESTED_ACTIONS = 3;
    public static final int MAX_ACTION_LABEL_LENGTH = 100;

    public static final String SAFE_REFUSAL_MESSAGE =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;
    public static final String SAFE_UNSTRUCTURED_FALLBACK =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;
    public static final String OPERATIONAL_FALLBACK =
            AssistantRefusalPolicy.OPERATIONAL_FALLBACK_MESSAGE;

    @FunctionalInterface
    public interface GeminiContentGenerator {
        CompletableFuture<GenerateContentResponse> generateContent(
                String model,
                List<Content> contents,
                GenerateContentConfig config
        );
    }

    private final AssistantProperties properties;
    private final GeminiContentGenerator contentGenerator;
    private final ObjectMapper objectMapper;
    private final Schema responseSchema;
    private final Client client;

    public GeminiResponseProvider(AssistantProperties properties) {
        this(properties, createDefaultClient(properties), new ObjectMapper());
    }

    public GeminiResponseProvider(AssistantProperties properties, Client client, ObjectMapper objectMapper) {
        this(properties, (model, contents, config) -> client.async.models.generateContent(model, contents, config), objectMapper, client);
    }

    public GeminiResponseProvider(AssistantProperties properties, GeminiContentGenerator contentGenerator, ObjectMapper objectMapper) {
        this(properties, contentGenerator, objectMapper, null);
    }

    private GeminiResponseProvider(AssistantProperties properties, GeminiContentGenerator contentGenerator, ObjectMapper objectMapper, Client client) {
        this.properties = properties;
        this.contentGenerator = contentGenerator;
        this.objectMapper = objectMapper;
        this.client = client;
        this.responseSchema = createResponseSchema();
    }

    private static Client createDefaultClient(AssistantProperties properties) {
        String effectiveApiKey = properties.getEffectiveGoogleApiKey();
        if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
            throw new IllegalArgumentException("Google API key must not be blank when using Gemini provider");
        }
        return Client.builder()
                .apiKey(effectiveApiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout((int) properties.getTimeout().toMillis())
                        .build())
                .build();
    }

    private static Schema createResponseSchema() {
        Schema actionSchema = Schema.builder()
                .type(new Type("OBJECT"))
                .properties(Map.of(
                        "label", Schema.builder()
                                .type(new Type("STRING"))
                                .description("Action button text")
                                .build(),
                        "path", Schema.builder()
                                .type(new Type("STRING"))
                                .description("Internal portal route")
                                .build()
                ))
                .required(List.of("label", "path"))
                .build();

        return Schema.builder()
                .type(new Type("OBJECT"))
                .properties(Map.of(
                        "answer", Schema.builder()
                                .type(new Type("STRING"))
                                .description("Plain text response to the user.")
                                .build(),
                        "suggestedActions", Schema.builder()
                                .type(new Type("ARRAY"))
                                .description("List of allowed internal navigation routes.")
                                .items(actionSchema)
                                .build()
                ))
                .required(List.of("answer", "suggestedActions"))
                .build();
    }

    @Override
    public AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId,
            Set<String> callerAuthorities
    ) {
        List<Content> contents = new ArrayList<>();

        if (request.history() != null) {
            for (AssistantMessageDto historyItem : request.history()) {
                String role = AssistantMessageDto.ROLE_ASSISTANT.equalsIgnoreCase(historyItem.role())
                        ? "model"
                        : "user";
                contents.add(Content.builder()
                        .role(role)
                        .parts(List.of(Part.fromText(historyItem.content())))
                        .build());
            }
        }

        // Current user message
        contents.add(Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(request.message())))
                .build());

        Content systemContent = Content.builder()
                .parts(List.of(Part.fromText(systemInstruction)))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemContent)
                .responseMimeType("application/json")
                .responseSchema(responseSchema)
                .temperature(0.0f)
                .maxOutputTokens(properties.getMaxOutputTokens())
                .build();

        String model = properties.getEffectiveModel();

        CompletableFuture<GenerateContentResponse> future =
                contentGenerator.generateContent(model, contents, config);

        try {
            GenerateContentResponse response = future.get(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return parseResponse(response, requestId, callerAuthorities);
        } catch (TimeoutException ex) {
            future.cancel(true);
            log.warn("Gemini assistant provider timeout occurred for requestId: {}", requestId);
            throw new AssistantTimeoutException("Assistant provider request timed out");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (isTimeout(cause)) {
                log.warn("Gemini assistant provider timeout occurred for requestId: {}", requestId);
                throw new AssistantTimeoutException("Assistant provider request timed out");
            }
            log.error("Gemini assistant provider error occurred for requestId: {}, errorType: {}",
                    requestId, cause.getClass().getSimpleName());
            return new AssistantChatResponse(OPERATIONAL_FALLBACK, Collections.emptyList(), requestId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Gemini assistant provider execution interrupted for requestId: {}", requestId);
            return new AssistantChatResponse(OPERATIONAL_FALLBACK, Collections.emptyList(), requestId);
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                log.warn("Gemini assistant provider timeout occurred for requestId: {}", requestId);
                throw new AssistantTimeoutException("Assistant provider request timed out");
            }
            log.error("Gemini assistant provider unexpected error for requestId: {}, errorType: {}",
                    requestId, ex.getClass().getSimpleName());
            return new AssistantChatResponse(OPERATIONAL_FALLBACK, Collections.emptyList(), requestId);
        }
    }

    public AssistantChatResponse parseResponse(
            GenerateContentResponse response,
            String requestId,
            Set<String> callerAuthorities
    ) {
        if (response == null) {
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        // 1. Refusal check: Candidate finishReason
        if (response.candidates().isPresent() && !response.candidates().get().isEmpty()) {
            Candidate candidate = response.candidates().get().get(0);
            if (candidate.finishReason().isPresent()) {
                FinishReason reason = candidate.finishReason().get();
                if (reason.knownEnum() == FinishReason.Known.SAFETY
                        || reason.knownEnum() == FinishReason.Known.BLOCKLIST
                        || reason.knownEnum() == FinishReason.Known.PROHIBITED_CONTENT
                        || reason.knownEnum() == FinishReason.Known.SPII) {
                    log.warn("Gemini response refused due to candidate finish reason: {} for requestId: {}", reason, requestId);
                    return new AssistantChatResponse(SAFE_REFUSAL_MESSAGE, Collections.emptyList(), requestId);
                }
            }
        }

        // 2. Refusal check: Prompt feedback block reason
        if (response.promptFeedback().isPresent() && response.promptFeedback().get().blockReason().isPresent()) {
            log.warn("Gemini response blocked due to prompt feedback: {} for requestId: {}",
                    response.promptFeedback().get().blockReason().get(), requestId);
            return new AssistantChatResponse(SAFE_REFUSAL_MESSAGE, Collections.emptyList(), requestId);
        }

        // 3. Extract text
        String rawText = "";
        try {
            rawText = response.text();
        } catch (Exception ex) {
            log.warn("Failed to extract text from Gemini response for requestId: {}, errorType: {}",
                    requestId, ex.getClass().getSimpleName());
        }

        return parseStructuredOutput(rawText, requestId, callerAuthorities);
    }

    public AssistantChatResponse parseStructuredOutput(
            String rawOutput,
            String requestId,
            Set<String> callerAuthorities
    ) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(rawOutput);
        } catch (Exception parseException) {
            log.warn("Gemini response was not valid JSON for requestId: {}", requestId);
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        if (!rootNode.hasNonNull("answer") || !rootNode.get("answer").isTextual()
                || rootNode.get("answer").asText().isBlank()) {
            log.warn("Gemini response missing textual answer field for requestId: {}", requestId);
            return new AssistantChatResponse(SAFE_UNSTRUCTURED_FALLBACK, Collections.emptyList(), requestId);
        }

        String answer = AiResponseProvider.safeTruncate(
                rootNode.get("answer").asText().trim(),
                MAX_ANSWER_CHARACTERS
        );

        List<AssistantActionDto> actions = new ArrayList<>();
        if (rootNode.has("suggestedActions") && rootNode.get("suggestedActions").isArray()) {
            for (JsonNode actionNode : rootNode.get("suggestedActions")) {
                if (actions.size() >= MAX_SUGGESTED_ACTIONS) {
                    break;
                }

                String label = actionNode.hasNonNull("label") ? actionNode.get("label").asText().trim() : "";
                String path = actionNode.hasNonNull("path") ? actionNode.get("path").asText().trim() : "";

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

    @Override
    public boolean isAvailable() {
        return properties.isConfiguredForLiveProvider();
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ex) {
                log.debug("Error while closing Gemini Client: {}", ex.getClass().getSimpleName());
            }
        }
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
