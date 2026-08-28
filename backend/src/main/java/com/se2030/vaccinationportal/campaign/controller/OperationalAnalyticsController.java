package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.response.OperationalAnalyticsResponse;
import com.se2030.vaccinationportal.campaign.service.OperationalAnalyticsService;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for MF-02 descriptive operational analytics.
 * Canonical PB-10D, UC-10D, and Rule MF02-BR-19.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class OperationalAnalyticsController {

    private final OperationalAnalyticsService operationalAnalyticsService;

    public OperationalAnalyticsController(OperationalAnalyticsService operationalAnalyticsService) {
        this.operationalAnalyticsService = Objects.requireNonNull(operationalAnalyticsService, "OperationalAnalyticsService must not be null");
    }

    /**
     * Retrieves aggregated operational analytics across campaigns, sessions, capacity plans, time slots, and resources.
     * Authorized: ADMIN, CAMPAIGN_OFFICER, CENTER_MANAGER.
     *
     * @param centerId Optional center filter
     * @param campaignId Optional campaign filter
     * @return Reconciled OperationalAnalyticsResponse
     */
    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER')")
    public ResponseEntity<OperationalAnalyticsResponse> getOperationalAnalytics(
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) Long campaignId
    ) {
        OperationalAnalyticsResponse response = operationalAnalyticsService.getOperationalAnalytics(centerId, campaignId);
        return ResponseEntity.ok(response);
    }
}
