package com.se2030.vaccinationportal.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.provider.AiResponseProvider;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Assistant Security & Spring Context Integration Tests")
class AssistantSecurityIntegrationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public AiResponseProvider testAiResponseProvider() {
            return new DeterministicFakeAiResponseProvider();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private AiResponseProvider aiResponseProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void resetFake() {
        if (aiResponseProvider instanceof DeterministicFakeAiResponseProvider fake) {
            fake.reset();
        }
    }

    @Test
    @DisplayName("GET /api/v1/assistant/status is publicly permitted through Spring Security filter chain")
    void testPublicStatusPermitted() throws Exception {
        mockMvc.perform(get("/api/v1/assistant/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.personalizedFeaturesAvailable").value(false))
                .andExpect(jsonPath("$.disclaimer").value(containsString("does not provide medical advice")));
    }

    @Test
    @DisplayName("POST /api/v1/assistant/messages without JWT succeeds anonymously through Spring Security")
    void testAnonymousMessagePermitted() throws Exception {
        AssistantChatRequest request = new AssistantChatRequest("Help with registration", Collections.emptyList(), null);

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/assistant/messages with valid JWT derives authenticated principal")
    void testAuthenticatedMessageWithValidJwt() throws Exception {
        String token = jwtTokenService.generateToken(999L, "citizen999@example.com", List.of("ROLE_CITIZEN"));
        AssistantChatRequest request = new AssistantChatRequest("Where are my appointments?", Collections.emptyList(), "/app/citizen");

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty());

        if (aiResponseProvider instanceof DeterministicFakeAiResponseProvider fake) {
            assertEquals(1, fake.getRecordedSystemInstructions().size());
            org.junit.jupiter.api.Assertions.assertTrue(
                    fake.getRecordedSystemInstructions().get(0).contains("ROLE_CITIZEN")
            );
        }
    }

    @Test
    @DisplayName("POST /api/v1/assistant/messages with invalid/tampered JWT is rejected with 401 Unauthorized")
    void testInvalidJwtRejectedByAssistantController() throws Exception {
        AssistantChatRequest request = new AssistantChatRequest("Help me", Collections.emptyList(), null);

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.signature.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired authentication token")));
    }
}
