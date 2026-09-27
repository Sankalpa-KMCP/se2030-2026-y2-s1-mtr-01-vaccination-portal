package com.se2030.vaccinationportal.assistant.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

/**
 * Validated configuration properties for the AI Assistant subsystem.
 * API keys and sensitive settings are strictly protected and never exposed in logs or API responses.
 */
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AssistantProperties {

    private boolean enabled = false;

    private String provider = "openai";

    private String apiKey = "";

    private String googleApiKey = "";

    private String model = "";

    private String baseUrl = "http://localhost:11434";

    @NotNull
    private Duration timeout = Duration.ofSeconds(15);

    @Min(100)
    @Max(10000)
    private int maxInputCharacters = 1500;

    @Min(1)
    @Max(50)
    private int maxHistoryMessages = 10;

    @Min(50)
    @Max(4000)
    private int maxOutputTokens = 500;

    @Valid
    @NotNull
    private RateLimit rateLimit = new RateLimit();

    public static class RateLimit {
        @Min(1)
        @Max(1000)
        private int anonymousPerMinute = 5;

        @Min(1)
        @Max(1000)
        private int authenticatedPerMinute = 15;

        public int getAnonymousPerMinute() {
            return anonymousPerMinute;
        }

        public void setAnonymousPerMinute(int anonymousPerMinute) {
            this.anonymousPerMinute = anonymousPerMinute;
        }

        public int getAuthenticatedPerMinute() {
            return authenticatedPerMinute;
        }

        public void setAuthenticatedPerMinute(int authenticatedPerMinute) {
            this.authenticatedPerMinute = authenticatedPerMinute;
        }

        @Override
        public String toString() {
            return "RateLimit{" +
                    "anonymousPerMinute=" + anonymousPerMinute +
                    ", authenticatedPerMinute=" + authenticatedPerMinute +
                    '}';
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider != null ? provider.trim().toLowerCase(Locale.ROOT) : "openai";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public String getGoogleApiKey() {
        return googleApiKey;
    }

    public void setGoogleApiKey(String googleApiKey) {
        this.googleApiKey = googleApiKey != null ? googleApiKey.trim() : "";
    }

    public String getEffectiveGoogleApiKey() {
        return googleApiKey != null ? googleApiKey.trim() : "";
    }

    public String getEffectiveModel() {
        if (model != null && !model.isBlank()) {
            return model.trim();
        }
        if ("gemini".equalsIgnoreCase(provider)) {
            return "gemini-flash-latest";
        }
        return "";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : "";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(15);
    }

    public int getMaxInputCharacters() {
        return maxInputCharacters;
    }

    public void setMaxInputCharacters(int maxInputCharacters) {
        this.maxInputCharacters = maxInputCharacters;
    }

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit != null ? rateLimit : new RateLimit();
    }

    public boolean isConfiguredForLiveProvider() {
        return switch (provider) {
            case "openai" -> enabled && !apiKey.isEmpty() && !model.isEmpty();
            case "ollama" -> enabled && isLoopbackBaseUrl() && !model.isEmpty();
            case "gemini" -> enabled && !getEffectiveGoogleApiKey().isEmpty() && !getEffectiveModel().isEmpty();
            default -> false;
        };
    }

    /**
     * Local-model prompts can contain authorized user context, so Ollama is deliberately restricted
     * to the loopback interface and cannot be redirected to an arbitrary remote host.
     */
    public boolean isLoopbackBaseUrl() {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            return "http".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && ("localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "AssistantProperties{" +
                "enabled=" + enabled +
                ", provider='" + provider + '\'' +
                ", apiKey='" + (apiKey.isEmpty() ? "<empty>" : "<redacted>") + '\'' +
                ", googleApiKey='" + (getEffectiveGoogleApiKey().isEmpty() ? "<empty>" : "<redacted>") + '\'' +
                ", model='" + model + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", timeout=" + timeout +
                ", maxInputCharacters=" + maxInputCharacters +
                ", maxHistoryMessages=" + maxHistoryMessages +
                ", maxOutputTokens=" + maxOutputTokens +
                ", rateLimit=" + rateLimit +
                '}';
    }
}
