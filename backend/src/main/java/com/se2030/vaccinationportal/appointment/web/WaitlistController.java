package com.se2030.vaccinationportal.appointment.web;

import com.se2030.vaccinationportal.appointment.dto.WaitlistEntryResponse;
import com.se2030.vaccinationportal.appointment.dto.WaitlistJoinRequest;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.service.WaitlistJoinService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing waitlist join and citizen waitlist status inspection endpoints.
 */
@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistJoinService joinService;
    private final WaitlistRepository waitlistRepository;

    @Autowired
    public WaitlistController(WaitlistJoinService joinService, WaitlistRepository waitlistRepository) {
        this.joinService = joinService;
        this.waitlistRepository = waitlistRepository;
    }

    @PostMapping("/join")
    public ResponseEntity<WaitlistEntryResponse> joinWaitlist(@Valid @RequestBody WaitlistJoinRequest request) {
        WaitlistEntry entry = joinService.joinWaitlist(
                request.citizenId(),
                request.campaignId(),
                request.sessionId(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WaitlistEntryResponse.fromEntity(entry));
    }

    @GetMapping("/status")
    public ResponseEntity<WaitlistEntryResponse> getWaitlistStatus(
            @RequestParam("citizenId") Long citizenId,
            @RequestParam("campaignId") Long campaignId
    ) {
        return waitlistRepository.findByCitizenIdAndCampaignIdAndStatus(citizenId, campaignId, WaitlistStatus.WAITING)
                .map(entry -> ResponseEntity.ok(WaitlistEntryResponse.fromEntity(entry)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
