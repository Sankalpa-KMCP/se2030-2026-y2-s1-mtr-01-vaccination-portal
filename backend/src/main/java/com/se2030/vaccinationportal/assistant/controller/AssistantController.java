package com.se2030.vaccinationportal.assistant.controller;

import com.se2030.vaccinationportal.assistant.dto.AssistantChatRequest;
import com.se2030.vaccinationportal.assistant.dto.AssistantChatResponse;
import com.se2030.vaccinationportal.assistant.dto.AssistantStatusResponse;
import com.se2030.vaccinationportal.assistant.exception.InvalidAssistantAuthenticationException;
import com.se2030.vaccinationportal.assistant.service.AssistantService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing AI Assistant status and conversational guidance endpoints.
 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @GetMapping("/status")
    public ResponseEntity<AssistantStatusResponse> getStatus() {
        return ResponseEntity.ok(assistantService.getStatus());
    }

    @PostMapping("/messages")
    public ResponseEntity<AssistantChatResponse> handleMessage(
            @Valid @RequestBody AssistantChatRequest request,
            HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If an Authorization header is supplied, verify that authentication succeeded.
        // Never silently downgrade an invalid/expired token to anonymous.
        if (authHeader != null && !authHeader.isBlank()) {
            if (!authHeader.startsWith("Bearer ")
                    || authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                throw new InvalidAssistantAuthenticationException("Invalid or expired authentication token");
            }
        }

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up)
                ? up
                : null;

        AssistantChatResponse response = assistantService.processMessage(request, principal, httpRequest);
        return ResponseEntity.ok(response);
    }
}
