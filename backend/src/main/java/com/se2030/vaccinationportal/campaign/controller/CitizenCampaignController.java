package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.response.CitizenCampaignDiscoveryResponse;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing authenticated Citizen vaccination campaign discovery (MF02-R-001).
 * Dedicated to authenticated citizens only (ROLE_CITIZEN). Anonymous and staff roles are denied.
 */
@RestController
@RequestMapping("/api/v1/citizen/campaigns")
public class CitizenCampaignController {

    private final VaccinationCampaignService campaignService;

    public CitizenCampaignController(VaccinationCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<CitizenCampaignDiscoveryResponse>> getCitizenCampaigns() {
        return ResponseEntity.ok(campaignService.getCitizenDiscoveryCampaigns());
    }
}
