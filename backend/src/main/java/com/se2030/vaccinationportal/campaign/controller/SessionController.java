package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionCancellationRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionRescheduleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSessionTransferRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSessionRescheduleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSessionTransferRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ResumeSessionBookingRequest;
import com.se2030.vaccinationportal.campaign.dto.request.SuspendSessionBookingRequest;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangeEventResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionOperationalReadinessResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionSummaryResponse;
import com.se2030.vaccinationportal.campaign.mapper.SessionDtoMapper;
import com.se2030.vaccinationportal.campaign.model.SessionBookingStatus;
import com.se2030.vaccinationportal.campaign.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.service.SessionBookingStatusService;
import com.se2030.vaccinationportal.campaign.service.SessionChangePreview;
import com.se2030.vaccinationportal.campaign.service.SessionOperationalReadinessService;
import com.se2030.vaccinationportal.campaign.service.VaccinationSessionService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing vaccination session core management, change control, booking suspension,
 * and lifecycle endpoints.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final VaccinationSessionService sessionService;
    private final SessionOperationalReadinessService readinessService;
    private final SessionBookingStatusService bookingStatusService;

    public SessionController(
            VaccinationSessionService sessionService,
            SessionOperationalReadinessService readinessService,
            SessionBookingStatusService bookingStatusService
    ) {
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService must not be null");
        this.readinessService = Objects.requireNonNull(readinessService, "readinessService must not be null");
        this.bookingStatusService = Objects.requireNonNull(bookingStatusService, "bookingStatusService must not be null");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        VaccinationSession created = sessionService.createOneOffDraftSession(
                request.campaignId(),
                request.centerId(),
                request.startAt(),
                request.endAt(),
                request.bookingCloseAt()
        );

        URI location = URI.create("/api/v1/sessions/" + created.getId());
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(created);
        return ResponseEntity.created(location).body(SessionDtoMapper.toResponse(created, bookingStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER', 'HEALTHCARE_OFFICER')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        VaccinationSession session = sessionService.getRequiredSessionById(id);
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(session);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(session, bookingStatus));
    }

    @GetMapping("/{id}/readiness")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER', 'HEALTHCARE_OFFICER')")
    public ResponseEntity<SessionOperationalReadinessResponse> getSessionReadiness(@PathVariable Long id) {
        SessionOperationalReadinessService.OperationalReadinessResult result = readinessService.evaluateReadiness(id);
        return ResponseEntity.ok(SessionDtoMapper.toReadinessResponse(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER', 'HEALTHCARE_OFFICER')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<SessionSummaryResponse>> getSessions(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) SessionLifecycleStatus status
    ) {
        List<VaccinationSession> sessions = sessionService.getSessions(campaignId, centerId, status);
        Map<Long, SessionBookingStatus> statusMap = bookingStatusService.deriveBookingStatuses(sessions);
        List<SessionSummaryResponse> response = sessions.stream()
                .map(s -> SessionDtoMapper.toSummaryResponse(s, statusMap.get(s.getId())))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<SessionResponse> scheduleSession(@PathVariable Long id) {
        VaccinationSession scheduled = sessionService.scheduleSession(id);
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(scheduled);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(scheduled, bookingStatus));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'HEALTHCARE_OFFICER')")
    public ResponseEntity<SessionResponse> startSession(@PathVariable Long id) {
        VaccinationSession started = sessionService.startSession(id);
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(started);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(started, bookingStatus));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'HEALTHCARE_OFFICER')")
    public ResponseEntity<SessionResponse> completeSession(@PathVariable Long id) {
        VaccinationSession completed = sessionService.completeSession(id);
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(completed);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(completed, bookingStatus));
    }

    @PostMapping("/{id}/cancellation-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<SessionChangePreviewResponse> previewCancellation(@PathVariable Long id) {
        SessionChangePreview preview = sessionService.previewCancellation(id);
        return ResponseEntity.ok(SessionDtoMapper.toPreviewResponse(preview));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<SessionResponse> confirmCancellation(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSessionCancellationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String trustedActorId = String.valueOf(principal.getUserId());
        VaccinationSession cancelled = sessionService.confirmCancellation(
                id,
                request.previewToken(),
                request.reason(),
                trustedActorId
        );
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(cancelled);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(cancelled, bookingStatus));
    }

    @PostMapping("/{id}/reschedule-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<SessionResponse> confirmReschedule(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSessionRescheduleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
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
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(rescheduled);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(rescheduled, bookingStatus));
    }

    @PostMapping("/{id}/transfer-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<SessionResponse> confirmCenterTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSessionTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
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
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(transferred);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(transferred, bookingStatus));
    }

    @PostMapping("/{id}/booking-suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<SessionResponse> suspendBooking(
            @PathVariable Long id,
            @Valid @RequestBody SuspendSessionBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String trustedActorId = String.valueOf(principal.getUserId());
        VaccinationSession session = sessionService.suspendBooking(id, request.reason(), trustedActorId);
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(session);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(session, bookingStatus));
    }

    @PostMapping("/{id}/booking-resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<SessionResponse> resumeBooking(
            @PathVariable Long id,
            @Valid @RequestBody ResumeSessionBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String trustedActorId = String.valueOf(principal.getUserId());
        VaccinationSession session = sessionService.resumeBooking(id, request.reason(), trustedActorId);
        SessionBookingStatus bookingStatus = bookingStatusService.deriveBookingStatus(session);
        return ResponseEntity.ok(SessionDtoMapper.toResponse(session, bookingStatus));
    }

    @GetMapping("/{id}/change-events")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'CAMPAIGN_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<SessionChangeEventResponse>> getSessionChangeEvents(@PathVariable Long id) {
        List<SessionChangeEvent> events = sessionService.getSessionChangeEvents(id);
        List<SessionChangeEventResponse> response = events.stream()
                .map(SessionDtoMapper::toChangeEventResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
