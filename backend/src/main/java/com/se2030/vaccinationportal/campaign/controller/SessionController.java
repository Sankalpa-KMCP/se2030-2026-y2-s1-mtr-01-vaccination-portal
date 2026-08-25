package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionCancellationRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionRescheduleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionTransferRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSessionRescheduleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSessionTransferRequest;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangeEventResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionOperationalReadinessResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.mapper.SessionDtoMapper;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionOperationalReadinessService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing vaccination session core management, change control, and lifecycle endpoints.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final VaccinationSessionService sessionService;
    private final SessionOperationalReadinessService readinessService;

    public SessionController(
            VaccinationSessionService sessionService,
            SessionOperationalReadinessService readinessService
    ) {
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService must not be null");
        this.readinessService = Objects.requireNonNull(readinessService, "readinessService must not be null");
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        VaccinationSession created = sessionService.createOneOffDraftSession(
                request.campaignId(),
                request.centerId(),
                request.startAt(),
                request.endAt(),
                request.bookingCloseAt()
        );

        URI location = URI.create("/api/v1/sessions/" + created.getId());
        return ResponseEntity.created(location).body(SessionDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        VaccinationSession session = sessionService.getRequiredSessionById(id);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(session));
    }

    @GetMapping("/{id}/readiness")
    public ResponseEntity<SessionOperationalReadinessResponse> getSessionReadiness(@PathVariable Long id) {
        SessionOperationalReadinessService.OperationalReadinessResult result = readinessService.evaluateReadiness(id);
        return ResponseEntity.ok(SessionDtoMapper.toReadinessResponse(result));
    }

    @GetMapping
    public ResponseEntity<List<SessionSummaryResponse>> getSessions(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) SessionLifecycleStatus status
    ) {
        List<VaccinationSession> sessions = sessionService.getSessions(campaignId, centerId, status);
        List<SessionSummaryResponse> response = sessions.stream()
                .map(SessionDtoMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<SessionResponse> scheduleSession(@PathVariable Long id) {
        VaccinationSession scheduled = sessionService.scheduleSession(id);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(scheduled));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SessionResponse> startSession(@PathVariable Long id) {
        VaccinationSession started = sessionService.startSession(id);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(started));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SessionResponse> completeSession(@PathVariable Long id) {
        VaccinationSession completed = sessionService.completeSession(id);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(completed));
    }

    @PostMapping("/{id}/cancellation-preview")
    public ResponseEntity<SessionChangePreviewResponse> previewCancellation(@PathVariable Long id) {
        SessionChangePreview preview = sessionService.previewCancellation(id);
        return ResponseEntity.ok(SessionDtoMapper.toPreviewResponse(preview));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SessionResponse> confirmCancellation(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSessionCancellationRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.se2030.vaccinationportal.shared.auth.security.UserPrincipal principal
    ) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String trustedActorId = String.valueOf(principal.getUserId());
        VaccinationSession cancelled = sessionService.confirmCancellation(
                id,
                request.previewToken(),
                request.reason(),
                trustedActorId
        );
        return ResponseEntity.ok(SessionDtoMapper.toResponse(cancelled));
    }

    @PostMapping("/{id}/reschedule-preview")
    public ResponseEntity<SessionChangePreviewResponse> previewReschedule(
            @PathVariable Long id,
            @Valid @RequestBody PreviewSessionRescheduleRequest request
    ) {
        SessionChangePreview preview = sessionService.previewReschedule(
                id,
                request.newStartAt(),
                request.newEndAt(),
                request.newBookingCloseAt()
        );
        return ResponseEntity.ok(SessionDtoMapper.toPreviewResponse(preview));
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<SessionResponse> confirmReschedule(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSessionRescheduleRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.se2030.vaccinationportal.shared.auth.security.UserPrincipal principal
    ) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String trustedActorId = String.valueOf(principal.getUserId());
        VaccinationSession rescheduled = sessionService.confirmReschedule(
                id,
                request.newStartAt(),
                request.newEndAt(),
                request.newBookingCloseAt(),
                request.previewToken(),
                request.reason(),
                trustedActorId
        );
        return ResponseEntity.ok(SessionDtoMapper.toResponse(rescheduled));
    }

    @PostMapping("/{id}/transfer-preview")
    public ResponseEntity<SessionChangePreviewResponse> previewCenterTransfer(
            @PathVariable Long id,
            @Valid @RequestBody PreviewSessionTransferRequest request
    ) {
        SessionChangePreview preview = sessionService.previewCenterTransfer(
                id,
                request.newCenterId(),
                request.newStartAt(),
                request.newEndAt(),
                request.newBookingCloseAt()
        );
        return ResponseEntity.ok(SessionDtoMapper.toPreviewResponse(preview));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<SessionResponse> confirmCenterTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSessionTransferRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.se2030.vaccinationportal.shared.auth.security.UserPrincipal principal
    ) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String trustedActorId = String.valueOf(principal.getUserId());
        VaccinationSession transferred = sessionService.confirmCenterTransfer(
                id,
                request.newCenterId(),
                request.newStartAt(),
                request.newEndAt(),
                request.newBookingCloseAt(),
                request.previewToken(),
                request.reason(),
                trustedActorId
        );
        return ResponseEntity.ok(SessionDtoMapper.toResponse(transferred));
    }

    @GetMapping("/{id}/change-events")
    public ResponseEntity<List<SessionChangeEventResponse>> getSessionChangeEvents(@PathVariable Long id) {
        List<SessionChangeEvent> events = sessionService.getSessionChangeEvents(id);
        List<SessionChangeEventResponse> response = events.stream()
                .map(SessionDtoMapper::toChangeEventResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
