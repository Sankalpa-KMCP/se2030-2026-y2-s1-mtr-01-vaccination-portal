package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssistantProperties Configuration & Validation Tests")
class AssistantPropertiesValidationTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Default properties enforce disabled state and safe default thresholds")
    void testDefaultProperties() {
        AssistantProperties props = new AssistantProperties();

        assertFalse(props.isEnabled(), "Assistant must default to disabled (false)");
        assertEquals("openai", props.getProvider());
        assertEquals("", props.getApiKey());
        assertEquals("", props.getModel());
        assertEquals("http://localhost:11434", props.getBaseUrl());
        assertEquals(Duration.ofSeconds(15), props.getTimeout());
        assertEquals(1500, props.getMaxInputCharacters());
        assertEquals(10, props.getMaxHistoryMessages());
        assertEquals(500, props.getMaxOutputTokens());
        assertEquals(5, props.getRateLimit().getAnonymousPerMinute());
        assertEquals(15, props.getRateLimit().getAuthenticatedPerMinute());

        assertFalse(props.isConfiguredForLiveProvider());

        Set<ConstraintViolation<AssistantProperties>> violations = validator.validate(props);
        assertTrue(violations.isEmpty(), "Default properties should satisfy validation constraints");
    }

    @Test
    @DisplayName("Ollama requires an enabled local endpoint and model but no API key")
    void testOllamaConfiguration() {
        AssistantProperties props = new AssistantProperties();
        props.setEnabled(true);
        props.setProvider("ollama");
        props.setModel("qwen3:4b");

        assertTrue(props.isConfiguredForLiveProvider());

        props.setBaseUrl("https://remote-model.example.com");
        assertFalse(props.isConfiguredForLiveProvider(), "Remote endpoints must fail closed");
    }

    @Test
    @DisplayName("Missing API key or model prevents live provider activation")
    void testMissingApiKeyOrModel() {
        AssistantProperties props = new AssistantProperties();
        props.setEnabled(true);

        // Model set, but API key missing
        props.setModel("gpt-4o");
        props.setApiKey("");
        assertFalse(props.isConfiguredForLiveProvider());

        // API key set, but model missing
        props.setModel("");
        props.setApiKey("sk-proj-test123456789");
        assertFalse(props.isConfiguredForLiveProvider());

        // Both set
        props.setModel("gpt-4o");
        props.setApiKey("sk-proj-test123456789");
        assertTrue(props.isConfiguredForLiveProvider());
    }

    @Test
    @DisplayName("Gemini configuration validates effective Google API key and default model")
    void testGeminiConfiguration() {
        AssistantProperties props = new AssistantProperties();
        props.setEnabled(true);
        props.setProvider("gemini");

        // Missing key: not configured
        assertFalse(props.isConfiguredForLiveProvider());

        // Canonical googleApiKey supplied: model defaults to gemini-flash-latest
        props.setGoogleApiKey("AIzaSyTestGoogleKey123");
        assertEquals("gemini-flash-latest", props.getEffectiveModel());
        assertTrue(props.isConfiguredForLiveProvider());

        // Explicit model overrides default
        props.setModel("gemini-2.5-flash");
        assertEquals("gemini-2.5-flash", props.getEffectiveModel());
        assertTrue(props.isConfiguredForLiveProvider());

        // Fallback to apiKey if googleApiKey is blank
        props.setGoogleApiKey("");
        props.setApiKey("AIzaSyFallbackKey456");
        assertEquals("AIzaSyFallbackKey456", props.getEffectiveGoogleApiKey());
        assertTrue(props.isConfiguredForLiveProvider());
    }

    @Test
    @DisplayName("toString() masks the API key and prevents credential leaking")
    void testApiKeyMaskingInToString() {
        AssistantProperties props = new AssistantProperties();
        props.setApiKey("secret-openai-api-key-12345");
        props.setGoogleApiKey("secret-google-api-key-67890");
        props.setModel("test-model");

        String str = props.toString();
        assertFalse(str.contains("secret-openai-api-key-12345"), "API key must be redacted in toString()");
        assertFalse(str.contains("secret-google-api-key-67890"), "Google API key must be redacted in toString()");
        assertTrue(str.contains("<redacted>"), "toString() should explicitly mark API key as redacted");
    }

    @Test
    @DisplayName("Validation rejects negative or out-of-range limits")
    void testValidationBounds() {
        AssistantProperties props = new AssistantProperties();
        props.setMaxInputCharacters(50); // Min is 100
        props.setMaxHistoryMessages(0);  // Min is 1
        props.setMaxOutputTokens(10);   // Min is 50

        Set<ConstraintViolation<AssistantProperties>> violations = validator.validate(props);
        assertEquals(3, violations.size());
    }
}
