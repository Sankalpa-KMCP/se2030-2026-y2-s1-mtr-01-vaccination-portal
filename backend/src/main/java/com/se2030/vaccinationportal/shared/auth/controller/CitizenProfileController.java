package com.se2030.vaccinationportal.shared.auth.controller;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenProfileResponse;
import com.se2030.vaccinationportal.shared.auth.dto.UpdateCitizenProfileRequest;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.CitizenProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@PreAuthorize("hasRole('CITIZEN')")
public class CitizenProfileController {

    private final CitizenProfileService citizenProfileService;

    public CitizenProfileController(CitizenProfileService citizenProfileService) {
        this.citizenProfileService = citizenProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<CitizenProfileResponse> getOwnProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(citizenProfileService.getOwnProfile(principal.getUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<CitizenProfileResponse> updateOwnProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCitizenProfileRequest request
    ) {
        return ResponseEntity.ok(citizenProfileService.updateOwnProfile(principal.getUserId(), request));
    }
}
