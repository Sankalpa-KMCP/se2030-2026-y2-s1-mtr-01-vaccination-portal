package com.se2030.vaccinationportal.assistant.config;

import com.se2030.vaccinationportal.assistant.provider.AiResponseProvider;
import com.se2030.vaccinationportal.assistant.provider.DisabledAiResponseProvider;
import com.se2030.vaccinationportal.assistant.provider.GeminiResponseProvider;
import com.se2030.vaccinationportal.assistant.provider.OpenAiResponsesProvider;
import com.se2030.vaccinationportal.assistant.provider.OllamaChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfig {

    private static final Logger log = LoggerFactory.getLogger(AssistantConfig.class);

    @Bean
    @ConditionalOnMissingBean(AiResponseProvider.class)
    public AiResponseProvider aiResponseProvider(AssistantProperties properties) {
        if (!properties.isEnabled()) {
            log.info("AI Assistant is disabled (app.ai.enabled=false). Registered DisabledAiResponseProvider.");
            return new DisabledAiResponseProvider();
        }

        if (!properties.isConfiguredForLiveProvider()) {
            log.warn("AI Assistant is enabled but configuration is incomplete. Registered DisabledAiResponseProvider.");
            return new DisabledAiResponseProvider();
        }

        if ("ollama".equals(properties.getProvider())) {
            log.info("AI Assistant initialized with local Ollama provider.");
            return new OllamaChatProvider(properties);
        }

        if ("openai".equals(properties.getProvider())) {
            log.info("AI Assistant initialized with live Responses provider.");
            return new OpenAiResponsesProvider(properties);
        }

        if ("gemini".equals(properties.getProvider())) {
            log.info("AI Assistant initialized with Google Gemini provider.");
            return new GeminiResponseProvider(properties);
        }
        log.warn("AI Assistant provider is unsupported. Registered DisabledAiResponseProvider.");
        return new DisabledAiResponseProvider();
    }
}
