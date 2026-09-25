package com.se2030.vaccinationportal.assistant.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextConfig;
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
import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Live OpenAI implementation utilizing the official OpenAI Java SDK and Responses API (/v1/responses).
 * Never exposes credentials, prompts, or upstream diagnostics to callers.
 */
public class OpenAiResponsesProvider implements AiResponseProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiResponsesProvider.class);

    private final AssistantProperties properties;
    private final OpenAIClient openAiClient;
    private final ObjectMapper objectMapper;

    public OpenAiResponsesProvider(AssistantProperties properties) {
        this(properties, createDefaultClient(properties), new ObjectMapper());
    }

    public OpenAiResponsesProvider(AssistantProperties properties, OpenAIClient openAiClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    private static OpenAIClient createDefaultClient(AssistantProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .timeout(properties.getTimeout())
                .build();
    }

    @Override
    public AssistantChatResponse generateResponse(
            AssistantChatRequest request,
            String systemInstruction,
            String requestId
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
                        .format(ResponseFormatTextConfig.ofJsonObject(ResponseFormatJsonObject.builder().build()))
                        .build())
                .build();

        try {
            Response response = openAiClient.responses().create(createParams);
            return parseResponse(response, requestId);
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                log.warn("Upstream assistant provider timeout occurred for requestId: {}", requestId);
                throw new AssistantTimeoutException("Assistant provider request timed out", ex);
            }
            log.error("Upstream assistant provider error occurred for requestId: {}", requestId);
            throw new AssistantProviderException("Assistant provider temporarily unavailable", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfiguredForLiveProvider();
    }

    private AssistantChatResponse parseResponse(Response response, String requestId) {
        String rawOutput = "";
        if (response.output() != null) {
            for (ResponseOutputItem item : response.output()) {
                if (item.message().isPresent()) {
                    ResponseOutputMessage message = item.message().get();
                    if (message.content() != null) {
                        for (ResponseOutputMessage.Content content : message.content()) {
                            if (content.isOutputText()) {
                                rawOutput = content.asOutputText().text();
                                break;
                            } else if (content.isRefusal()) {
                                rawOutput = content.asRefusal().refusal();
                                break;
                            }
                        }
                    }
                }
                if (!rawOutput.isEmpty()) {
                    break;
                }
            }
        }

        if (rawOutput.isBlank()) {
            return new AssistantChatResponse(
                    "I am currently unable to provide an answer. Please contact an authorized healthcare professional or local emergency service.",
                    Collections.emptyList(),
                    requestId
            );
        }

        // Attempt JSON parse
        try {
            JsonNode rootNode = objectMapper.readTree(rawOutput);
            String answer = rootNode.hasNonNull("answer")
                    ? rootNode.get("answer").asText()
                    : rawOutput;

            List<AssistantActionDto> actions = new ArrayList<>();
            if (rootNode.has("suggestedActions") && rootNode.get("suggestedActions").isArray()) {
                for (JsonNode actionNode : rootNode.get("suggestedActions")) {
                    String label = actionNode.hasNonNull("label") ? actionNode.get("label").asText() : "";
                    String path = actionNode.hasNonNull("path") ? actionNode.get("path").asText() : "";

                    // Strictly enforce allowlisted internal routes
                    if (AssistantRouteCatalog.isAllowedRoute(path) && !label.isBlank()) {
                        actions.add(new AssistantActionDto(label.trim(), path.trim()));
                    }
                }
            }

            return new AssistantChatResponse(answer.trim(), actions, requestId);
        } catch (Exception ignored) {
            // Fallback to plain text output if provider did not return strict JSON
            return new AssistantChatResponse(rawOutput.trim(), Collections.emptyList(), requestId);
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
