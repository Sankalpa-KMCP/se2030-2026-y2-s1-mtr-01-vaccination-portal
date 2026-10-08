package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.config.AssistantConfig;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.provider.AiResponseProvider;
import com.se2030.vaccinationportal.assistant.provider.DisabledAiResponseProvider;
import com.se2030.vaccinationportal.assistant.provider.OllamaChatProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantProviderSelectionTests {

    private final AssistantConfig config = new AssistantConfig();

    @Test
    void selectsOllamaWithoutAnApiKey() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("OLLAMA");
        properties.setModel("qwen3:4b");

        AiResponseProvider provider = config.aiResponseProvider(properties);

        assertThat(provider).isInstanceOf(OllamaChatProvider.class);
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void selectsGeminiWithGoogleApiKey() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGoogleApiKey("AIzaSyTestFakeGoogleKeyForSelectionTest");

        AiResponseProvider provider = config.aiResponseProvider(properties);

        assertThat(provider).isInstanceOf(com.se2030.vaccinationportal.assistant.provider.GeminiResponseProvider.class);
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void geminiWithoutApiKeyFailsClosedToDisabledProvider() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGoogleApiKey("");
        properties.setApiKey("");

        AiResponseProvider provider = config.aiResponseProvider(properties);

        assertThat(provider).isInstanceOf(DisabledAiResponseProvider.class);
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void geminiWithOnlyOpenAiApiKeyFailsClosedToDisabledProvider() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("gemini");
        properties.setGoogleApiKey("");
        properties.setApiKey("sk-proj-only-openai-key-present");

        AiResponseProvider provider = config.aiResponseProvider(properties);

        assertThat(provider).isInstanceOf(DisabledAiResponseProvider.class);
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void unsupportedOrRemoteProviderConfigurationFailsClosed() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setProvider("ollama");
        properties.setModel("qwen3:4b");
        properties.setBaseUrl("https://remote-model.example.com");

        assertThat(config.aiResponseProvider(properties)).isInstanceOf(DisabledAiResponseProvider.class);

        properties.setProvider("unknown");
        assertThat(config.aiResponseProvider(properties)).isInstanceOf(DisabledAiResponseProvider.class);
    }
}
