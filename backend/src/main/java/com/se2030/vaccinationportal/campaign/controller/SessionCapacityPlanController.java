package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor;
import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.response.CapacityPlanPreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionCapacityPlanResponse;
import com.se2030.vaccinationportal.campaign.mapper.SessionCapacityPlanDtoMapper;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing Session Capacity Plan preview, approval, and revision inspection endpoints.
 */
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/capacity-plans")
public class SessionCapacityPlanController {

    private final SessionCapacityPlanService capacityPlanService;

    public SessionCapacityPlanController(SessionCapacityPlanService capacityPlanService) {
        this.capacityPlanService = Objects.requireNonNull(capacityPlanService, "capacityPlanService must not be null");
    }

    @PostMapping("/preview")
    public ResponseEntity<CapacityPlanPreviewResponse> previewCapacityPlan(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody PreviewCapacityPlanRequest request
    ) {
        CapacityPlanPreviewResponse response = capacityPlanService.previewCapacityPlan(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SessionCapacityPlanResponse> createCapacityPlan(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody CreateSessionCapacityPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AuthenticatedActor actor = principal != null
                ? new AuthenticatedActor(principal.getUserId(), new HashSet<>(principal.getRoleCodes()))
                : null;
        SessionCapacityPlan created = capacityPlanService.createCapacityPlan(sessionId, request, actor);
        SessionCapacityPlanResponse response = SessionCapacityPlanDtoMapper.toResponse(created);
        URI location = URI.create(String.format("/api/v1/sessions/%d/capacity-plans/%d", sessionId, created.getRevision()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SessionCapacityPlanResponse>> listCapacityPlans(
            @PathVariable("sessionId") Long sessionId
    ) {
        List<SessionCapacityPlan> plans = capacityPlanService.getCapacityPlansForSession(sessionId);
        List<SessionCapacityPlanResponse> response = plans.stream()
                .map(SessionCapacityPlanDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<SessionCapacityPlanResponse> getLatestCapacityPlan(
            @PathVariable("sessionId") Long sessionId
    ) {
        SessionCapacityPlan plan = capacityPlanService.getLatestCapacityPlan(sessionId);
        return ResponseEntity.ok(SessionCapacityPlanDtoMapper.toResponse(plan));
    }

    @GetMapping("/{revision:[0-9]+}")
    public ResponseEntity<SessionCapacityPlanResponse> getCapacityPlanByRevision(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("revision") int revision
    ) {
        SessionCapacityPlan plan = capacityPlanService.getCapacityPlanByRevision(sessionId, revision);
        return ResponseEntity.ok(SessionCapacityPlanDtoMapper.toResponse(plan));
    }
}
