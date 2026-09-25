package com.se2030.vaccinationportal.assistant.service;

import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantStatusResponse;
import com.se2030.vaccinationportal.assistant.exception.AssistantDisabledException;
import com.se2030.vaccinationportal.assistant.exception.AssistantRateLimitExceededException;
import com.se2030.vaccinationportal.assistant.knowledge.AssistantKnowledgeCatalog;
import com.se2030.vaccinationportal.assistant.knowledge.AssistantKnowledgeContextBuilder;
import com.se2030.vaccinationportal.assistant.knowledge.AssistantKnowledgeEntry;
import com.se2030.vaccinationportal.assistant.policy.AssistantRefusalPolicy;
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
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;



/**
 * Service coordinating assistant requests, privacy-conscious rate limiting,
 * caller authority resolution, system policy prompts, and structured response delivery.
 *
 * <p>Deployment Assumptions:
 * Assumes a servlet environment where HttpServletRequest.getRemoteAddr() accurately reflects
 * the client network origin (configured via trusted reverse proxy / container valves). Forwarded
 * headers are not trusted directly to prevent IP spoofing attacks against rate limits.
 */
import com.se2030.vaccinationportal.assistant.tool.AssistantPersonalizedContextBuilder;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolContext;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AiResponseProvider aiResponseProvider;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantPersonalizedContextBuilder personalizedContextBuilder;
    private final AssistantToolRegistry toolRegistry;

    @Autowired
    public AssistantService(
            AiResponseProvider aiResponseProvider,
            AssistantRateLimiter rateLimiter,
            AssistantPersonalizedContextBuilder personalizedContextBuilder,
            AssistantToolRegistry toolRegistry
    ) {
        this.aiResponseProvider = aiResponseProvider;
        this.rateLimiter = rateLimiter;
        this.personalizedContextBuilder = personalizedContextBuilder;
        this.toolRegistry = toolRegistry;
    }

    public AssistantService(AiResponseProvider aiResponseProvider, AssistantRateLimiter rateLimiter) {
        this(
                aiResponseProvider,
                rateLimiter,
                new AssistantPersonalizedContextBuilder(new AssistantToolRegistry(Collections.emptyList())),
                new AssistantToolRegistry(Collections.emptyList())
        );
    }

    public AssistantStatusResponse getStatus(UserPrincipal principal) {
        boolean available = aiResponseProvider.isAvailable();
        if (!available) {
            return AssistantStatusResponse.of(false, false);
        }
        boolean personalizedAvailable = false;
        if (principal != null && principal.getAuthorities() != null) {
            boolean isCitizen = principal.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_CITIZEN".equals(a.getAuthority()));
            personalizedAvailable = isCitizen && toolRegistry.hasToolsAvailable();
        }
        return AssistantStatusResponse.of(true, personalizedAvailable);
    }

    public AssistantStatusResponse getStatus() {
        return getStatus(null);
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

        // Derive normalized, immutable authority set
        Set<String> callerAuthorities = Collections.emptySet();
        if (isAuthenticated && principal.getAuthorities() != null) {
            callerAuthorities = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .collect(Collectors.toCollection(TreeSet::new));
            callerAuthorities = Collections.unmodifiableSet(callerAuthorities);
        }

        // Server-side deterministic safety policy evaluation
        Optional<AssistantChatResponse> policyRefusal = AssistantRefusalPolicy.evaluatePolicy(
                request.message(), callerAuthorities, requestId);
        if (policyRefusal.isPresent()) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("Assistant request intercepted by safety policy: requestId={}, category={}, actorId={}, status=200, latencyMs={}, inputLength={}",
                    requestId, category, actorId != null ? actorId : "anonymous", latencyMs, inputLength);
            return policyRefusal.get();
        }

        // Authorized citizen read-only personalized tool execution
        String personalizedContext = null;
        if (isAuthenticated && callerAuthorities.contains("ROLE_CITIZEN")) {
            AssistantToolContext toolContext = new AssistantToolContext(requestId, principal, callerAuthorities);
            personalizedContext = personalizedContextBuilder
                    .buildPersonalizedContext(request.message(), toolContext)
                    .orElse(null);
        }

        // Deterministic role-aware knowledge catalogue retrieval
        List<AssistantKnowledgeEntry> knowledgeEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                request.message(), callerAuthorities, 3);
        String knowledgeContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(
                knowledgeEntries, callerAuthorities);

        String systemInstruction = AssistantSystemPolicy.buildSystemInstruction(
                request.currentPath(), callerAuthorities, knowledgeContext, personalizedContext);

        try {
            AssistantChatResponse response = aiResponseProvider.generateResponse(request, systemInstruction, requestId, callerAuthorities);

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

        // Deployment Assumptions for Anonymous Rate Limiting:
        // 1. Direct servlet request remote address is used via HttpServletRequest.getRemoteAddr().
        // 2. X-Forwarded-For and Forwarded headers are intentionally not trusted directly by application code
        //    to prevent header spoofing attacks. If deployed behind a reverse proxy (e.g. AWS ALB, NGINX, or Cloudflare),
        //    the reverse proxy must sanitize headers and Spring Boot must be configured with
        //    `server.forward-headers-strategy=framework` or `native` with trusted-proxies configured at the container level.
        // 3. If remoteAddr is unavailable (null or blank), the system fails closed rather than sharing an unmetered or
        //    shared "unknown-remote" bucket, preventing denial-of-service / quota exhaustion across separate visitors.
        // 4. Raw IP addresses are NEVER logged or retained in plaintext; only a 16-character SHA-256 digest is stored.
        if (httpRequest == null) {
            throw new AssistantRateLimitExceededException("Unable to determine client origin for rate limiting. Request rejected.");
        }

        String remoteAddr = httpRequest.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            throw new AssistantRateLimitExceededException("Unable to determine client origin for rate limiting. Request rejected.");
        }

        return "anon:" + hashSha256(remoteAddr.trim());
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
