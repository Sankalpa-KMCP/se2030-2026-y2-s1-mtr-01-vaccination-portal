package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.response.CitizenCenterDiscoveryResponse;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing authenticated Citizen vaccination center discovery.
 * Dedicated to exposing active center registry information (Center ID, Centre code, Name)
 * to citizens for appointment booking and search operations.
 */
@RestController
@RequestMapping("/api/v1/citizen/centers")
public class CitizenCenterController {

    private final VaccinationCenterService centerService;

    public CitizenCenterController(VaccinationCenterService centerService) {
        this.centerService = centerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'HEALTHCARE_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CitizenCenterDiscoveryResponse>> getCitizenCenters() {
        return ResponseEntity.ok(centerService.getCitizenDiscoveryCenters());
    }
}
