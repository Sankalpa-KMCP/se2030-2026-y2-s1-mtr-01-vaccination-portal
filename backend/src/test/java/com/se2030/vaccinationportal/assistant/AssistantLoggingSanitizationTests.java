package com.se2030.vaccinationportal.assistant;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.se2030.vaccinationportal.assistant.config.AssistantConfig;
import com.se2030.vaccinationportal.assistant.config.AssistantProperties;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantMessageDto;
import com.se2030.vaccinationportal.assistant.exception.AssistantProviderException;
import com.se2030.vaccinationportal.assistant.provider.AiResponseProvider;
import com.se2030.vaccinationportal.assistant.service.AssistantRateLimiter;
import com.se2030.vaccinationportal.assistant.service.AssistantService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantLoggingSanitizationTests {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger configLogger;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.start();

        configLogger = (Logger) LoggerFactory.getLogger(AssistantConfig.class);
        serviceLogger = (Logger) LoggerFactory.getLogger(AssistantService.class);

        configLogger.addAppender(listAppender);
        serviceLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        configLogger.detachAppender(listAppender);
        serviceLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void startupLog_doesNotContainModelNameOrApiKey() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setApiKey("sk-proj-super-secret-1234567890abcdef");
        properties.setModel("gpt-4o-secret-test-model");

        AssistantConfig config = new AssistantConfig();
        config.aiResponseProvider(properties);

        List<String> logMessages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(logMessages).isNotEmpty();
        for (String msg : logMessages) {
            assertThat(msg).doesNotContain("gpt-4o-secret-test-model");
            assertThat(msg).doesNotContain("sk-proj-super-secret-1234567890abcdef");
            assertThat(msg).doesNotContain("secret");
        }
    }

    @Test
    void requestLogging_doesNotContainUserMessage_History_OrRawProviderError() {
        AssistantProperties properties = new AssistantProperties();
        properties.setEnabled(true);
        properties.setApiKey("sk-proj-super-secret-key");
        properties.setModel("gpt-4o-hidden");
        properties.setTimeout(Duration.ofSeconds(15));
        properties.setMaxInputCharacters(1500);
        properties.setMaxHistoryMessages(10);
        properties.setMaxOutputTokens(500);

        AssistantRateLimiter rateLimiter = mock(AssistantRateLimiter.class);
        when(rateLimiter.tryAcquire(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(true);

        AiResponseProvider provider = mock(AiResponseProvider.class);
        when(provider.isAvailable()).thenReturn(true);
        when(provider.generateResponse(any(), anyString(), anyString(), any()))
                .thenThrow(new AssistantProviderException("Raw upstream OpenAI failure 500 internal server crash with sensitive token XYZ"));


        AssistantService service = new AssistantService(provider, rateLimiter);

        String sensitiveUserMessage = "My confidential personal record identifier is RECORD-987654321-PRIVATE";
        String sensitiveHistoryContent = "Earlier private message about user identifier RECORD-123456789";

        AssistantChatRequest request = new AssistantChatRequest(
                sensitiveUserMessage,
                List.of(new AssistantMessageDto("user", sensitiveHistoryContent)),
                "/app/appointments/mine"
        );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("192.168.1.100");

        UserPrincipal principal = new UserPrincipal(
                101L,
                "user@test.local",
                "hashed",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CITIZEN"))
        );

        assertThatThrownBy(() -> service.processMessage(request, principal, httpRequest))
                .isInstanceOf(AssistantProviderException.class);


        List<String> logMessages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertThat(logMessages).isNotEmpty();
        for (String msg : logMessages) {
            assertThat(msg).doesNotContain(sensitiveUserMessage);
            assertThat(msg).doesNotContain(sensitiveHistoryContent);
            assertThat(msg).doesNotContain("sensitive token XYZ");
            assertThat(msg).doesNotContain("gpt-4o-hidden");
            assertThat(msg).doesNotContain("sk-proj");
            assertThat(msg).doesNotContain("192.168.1.100"); // raw IP must not be logged
        }
    }
}
