package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionCancellationRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionRescheduleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionTransferRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSessionRescheduleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSessionTransferRequest;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangeEventResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.mapper.SessionDtoMapper;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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

    public SessionController(VaccinationSessionService sessionService) {
        this.sessionService = sessionService;
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
            @Valid @RequestBody ConfirmSessionCancellationRequest request
    ) {
        VaccinationSession cancelled = sessionService.confirmCancellation(
                id,
                request.previewToken(),
                request.reason(),
                request.actorId()
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
            @Valid @RequestBody ConfirmSessionRescheduleRequest request
    ) {
        VaccinationSession rescheduled = sessionService.confirmReschedule(
                id,
                request.newStartAt(),
                request.newEndAt(),
                request.newBookingCloseAt(),
                request.previewToken(),
                request.reason(),
                request.actorId()
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
            @Valid @RequestBody ConfirmSessionTransferRequest request
    ) {
        VaccinationSession transferred = sessionService.confirmCenterTransfer(
                id,
                request.newCenterId(),
                request.newStartAt(),
                request.newEndAt(),
                request.newBookingCloseAt(),
                request.previewToken(),
                request.reason(),
                request.actorId()
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
