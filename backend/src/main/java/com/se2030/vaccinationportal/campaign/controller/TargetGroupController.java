package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.response.TargetGroupSummaryResponse;
import com.se2030.vaccinationportal.campaign.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.service.TargetGroupService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing read-only lookup endpoints for active Target Groups.
 */
@RestController
@RequestMapping("/api/v1/target-groups")
public class TargetGroupController {

    private final TargetGroupService targetGroupService;

    public TargetGroupController(TargetGroupService targetGroupService) {
        this.targetGroupService = targetGroupService;
    }

    /**
     * List all currently active target demographic groups.
     *
     * @return List of active TargetGroupSummaryResponse objects
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<List<TargetGroupSummaryResponse>> getActiveTargetGroups() {
        List<TargetGroup> activeGroups = targetGroupService.getActiveTargetGroups();
        List<TargetGroupSummaryResponse> response = activeGroups.stream()
                .map(CampaignDtoMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
