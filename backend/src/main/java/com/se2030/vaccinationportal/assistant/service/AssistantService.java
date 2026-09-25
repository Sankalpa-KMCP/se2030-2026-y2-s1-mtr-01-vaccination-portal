package com.se2030.vaccinationportal.assistant.service;

import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantStatusResponse;
import com.se2030.vaccinationportal.assistant.exception.AssistantDisabledException;
import com.se2030.vaccinationportal.assistant.exception.AssistantRateLimitExceededException;
import com.se2030.vaccinationportal.assistant.policy.AssistantSystemPolicy;
import com.se2030.vaccinationportal.assistant.provider.AiResponseProvider;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service coordinating assistant requests, privacy-conscious rate limiting,
 * system policy prompts, and structured response delivery.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AiResponseProvider aiResponseProvider;
    private final AssistantRateLimiter rateLimiter;

    public AssistantService(AiResponseProvider aiResponseProvider, AssistantRateLimiter rateLimiter) {
        this.aiResponseProvider = aiResponseProvider;
        this.rateLimiter = rateLimiter;
    }

    public AssistantStatusResponse getStatus() {
        return AssistantStatusResponse.of(aiResponseProvider.isAvailable());
    }

    public AssistantChatResponse processMessage(
            AssistantChatRequest request,
            UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        boolean isAuthenticated = principal != null;
        String category = isAuthenticated ? "authenticated" : "anonymous";
        String actorId = isAuthenticated ? String.valueOf(principal.getUserId()) : null;
        String clientKey = resolveClientKey(principal, httpRequest);
        int inputLength = request.message() != null ? request.message().length() : 0;
        int historyCount = request.history() != null ? request.history().size() : 0;

        // Rate limit check
        if (!rateLimiter.tryAcquire(clientKey, isAuthenticated)) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("Assistant rate limit exceeded: requestId={}, category={}, actorId={}, status=429, latencyMs={}, inputLength={}, historyCount={}",
                    requestId, category, actorId != null ? actorId : "anonymous", latencyMs, inputLength, historyCount);
            throw new AssistantRateLimitExceededException("Too many requests. Please wait a moment before sending another message.");
        }

        // Provider availability check
        if (!aiResponseProvider.isAvailable()) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("Assistant request rejected (disabled/misconfigured): requestId={}, category={}, actorId={}, status=503, latencyMs={}, inputLength={}, historyCount={}",
                    requestId, category, actorId != null ? actorId : "anonymous", latencyMs, inputLength, historyCount);
            throw new AssistantDisabledException("The AI assistant is currently disabled or unavailable.");
        }

        // Derive user role context
        String actorRole = null;
        if (isAuthenticated && principal.getAuthorities() != null) {
            actorRole = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse(null);
        }

        String systemInstruction = AssistantSystemPolicy.buildSystemInstruction(request.currentPath(), actorRole);

        try {
            AssistantChatResponse response = aiResponseProvider.generateResponse(request, systemInstruction, requestId);

            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("Assistant request completed: requestId={}, category={}, actorId={}, status=200, latencyMs={}, inputLength={}, historyCount={}",
                    requestId, category, actorId != null ? actorId : "anonymous", latencyMs, inputLength, historyCount);

            return response;
        } catch (RuntimeException ex) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Assistant request failed: requestId={}, category={}, actorId={}, exceptionClass={}, latencyMs={}, inputLength={}, historyCount={}",
                    requestId, category, actorId != null ? actorId : "anonymous", ex.getClass().getSimpleName(), latencyMs, inputLength, historyCount);
            throw ex;
        }
    }

    private String resolveClientKey(UserPrincipal principal, HttpServletRequest httpRequest) {
        if (principal != null) {
            return "auth:" + principal.getUserId();
        }

        // Privacy-conscious client IP strategy for anonymous users:
        // Hash remote address so raw IP addresses are not retained in memory or logs.
        String remoteAddr = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown-remote";
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = "unknown-remote";
        }

        return "anon:" + hashSha256(remoteAddr);
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
