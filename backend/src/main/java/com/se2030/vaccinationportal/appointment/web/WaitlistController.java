package com.se2030.vaccinationportal.appointment.web;

import com.se2030.vaccinationportal.appointment.dto.WaitlistEntryResponse;
import com.se2030.vaccinationportal.appointment.dto.WaitlistJoinRequest;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.service.WaitlistJoinService;
import com.se2030.vaccinationportal.appointment.service.WaitlistLeaveService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing waitlist join, leave, and citizen waitlist status inspection endpoints.
 *
 * <p><b>User Story:</b>
 * <ul>
 *   <li>US-05: Citizen waitlist join, queue position tracking, voluntary leave, and promotion status inspection.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-01: Screening eligibility verified before waitlist entry creation.</li>
 *   <li>MF03-BR-06: Allowed only when all session slots are full; maintains deterministic FIFO queue.</li>
 *   <li>MF03-BR-14: Rejects waitlist join if citizen already holds an active appointment for the campaign.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistJoinService joinService;
    private final WaitlistLeaveService leaveService;
    private final WaitlistRepository waitlistRepository;

    public WaitlistController(WaitlistJoinService joinService, WaitlistRepository waitlistRepository) {
        this(joinService, null, waitlistRepository);
    }

    @Autowired
    public WaitlistController(
            WaitlistJoinService joinService,
            @Autowired(required = false) WaitlistLeaveService leaveService,
            WaitlistRepository waitlistRepository
    ) {
        this.joinService = joinService;
        this.leaveService = leaveService;
        this.waitlistRepository = waitlistRepository;
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<WaitlistEntryResponse> joinWaitlist(
            @Valid @RequestBody WaitlistJoinRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        if (request.citizenId() != null && !request.citizenId().equals(user.getUserId())) {
            throw new AccessDeniedException("Citizens may only join waitlist for themselves");
        }
        Long effectiveCitizenId = user.getUserId();
        WaitlistEntry entry = joinService.joinWaitlist(
                effectiveCitizenId,
                request.campaignId(),
                request.sessionId(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WaitlistEntryResponse.fromEntity(entry));
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<WaitlistEntryResponse> leaveWaitlist(
            @PathVariable("id") Long id,
            @RequestParam(value = "citizenId", required = false) Long citizenId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        if (citizenId != null && !citizenId.equals(user.getUserId())) {
            throw new AccessDeniedException("Citizens may only leave their own waitlist entries");
        }
        if (waitlistRepository != null) {
            waitlistRepository.findById(id).ifPresent(entry -> {
                if (!user.getUserId().equals(entry.getCitizenId())) {
                    throw new AccessDeniedException("Waitlist entry " + id + " does not belong to citizen " + user.getUserId());
                }
            });
        }
        if (leaveService == null) {
            throw new IllegalStateException("WaitlistLeaveService is not available");
        }
        WaitlistEntry entry = leaveService.leaveWaitlist(id, user.getUserId());
        return ResponseEntity.ok(WaitlistEntryResponse.fromEntity(entry));
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<WaitlistEntryResponse> getWaitlistStatus(
            @RequestParam(value = "citizenId", required = false) Long citizenId,
            @RequestParam("campaignId") Long campaignId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        if (citizenId != null && !citizenId.equals(user.getUserId())) {
            throw new AccessDeniedException("Citizens may only inspect their own waitlist status");
        }
        Long effectiveCitizenId = user.getUserId();
        if (leaveService != null) {
            return leaveService.getWaitlistStatus(effectiveCitizenId, campaignId)
                    .map(entry -> {
                        Integer position = leaveService.getQueuePosition(entry);
                        return ResponseEntity.ok(WaitlistEntryResponse.fromEntity(entry, position));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(effectiveCitizenId, campaignId, WaitlistStatus.WAITING)
                .or(() -> waitlistRepository.findFirstByCitizenIdAndCampaignIdAndStatusOrderByJoinedAtDesc(
                        effectiveCitizenId, campaignId, WaitlistStatus.PROMOTED))
                .map(entry -> ResponseEntity.ok(WaitlistEntryResponse.fromEntity(entry)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UserPrincipal getAuthenticatedPrincipal(UserPrincipal principal) {
        if (principal != null) {
            return principal;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        throw new AccessDeniedException("Authentication required");
    }
}
