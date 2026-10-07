package com.se2030.vaccinationportal.campaign.reference.web;

import com.se2030.vaccinationportal.campaign.reference.dto.request.CreateTargetGroupRequest;
import com.se2030.vaccinationportal.campaign.reference.dto.request.UpdateTargetGroupRequest;
import com.se2030.vaccinationportal.campaign.reference.dto.response.TargetGroupSummaryResponse;
import com.se2030.vaccinationportal.campaign.core.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.service.TargetGroupService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing master data endpoints for Target Groups.
 */
@RestController
@RequestMapping("/api/v1/target-groups")
public class TargetGroupController {

    private final TargetGroupService targetGroupService;

    public TargetGroupController(TargetGroupService targetGroupService) {
        this.targetGroupService = targetGroupService;
    }

    /**
     * List target groups, defaulting to active-only for selector lookups.
     *
     * @param includeInactive if true, includes inactive records for management views
     * @return List of TargetGroupSummaryResponse objects
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<List<TargetGroupSummaryResponse>> getTargetGroups(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        List<TargetGroup> groups = targetGroupService.getTargetGroups(includeInactive);
        List<TargetGroupSummaryResponse> response = groups.stream()
                .map(CampaignDtoMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<TargetGroupSummaryResponse> createTargetGroup(
            @Valid @RequestBody CreateTargetGroupRequest request
    ) {
        TargetGroup created = targetGroupService.createTargetGroup(
                request.code(),
                request.description()
        );
        URI location = URI.create("/api/v1/target-groups/" + created.getId());
        return ResponseEntity.created(location).body(CampaignDtoMapper.toSummaryResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<TargetGroupSummaryResponse> updateTargetGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTargetGroupRequest request
    ) {
        TargetGroup updated = targetGroupService.updateTargetGroup(
                id,
                request.description()
        );
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(updated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<TargetGroupSummaryResponse> activateTargetGroup(@PathVariable Long id) {
        TargetGroup activated = targetGroupService.activateTargetGroup(id);
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<TargetGroupSummaryResponse> deactivateTargetGroup(@PathVariable Long id) {
        TargetGroup deactivated = targetGroupService.deactivateTargetGroup(id);
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(deactivated));
    }
}
