package com.se2030.vaccinationportal.campaign.core.web;

import com.se2030.vaccinationportal.campaign.core.dto.request.ArchiveCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.CancelCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.CompleteCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.CreateCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.ResumeCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.SuspendCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.UpdateCampaignRequest;
import com.se2030.vaccinationportal.campaign.core.dto.response.CampaignLifecycleEventResponse;
import com.se2030.vaccinationportal.campaign.core.dto.response.CampaignResponse;
import com.se2030.vaccinationportal.campaign.core.dto.response.CampaignSummaryResponse;
import com.se2030.vaccinationportal.campaign.core.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles campaign requests and passes business rules to the service.
 * Campaigns are cancelled or archived rather than permanently deleted.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final VaccinationCampaignService campaignService;

    public CampaignController(VaccinationCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /** Create a draft campaign; @Valid checks the submitted fields. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        VaccinationCampaign created = campaignService.createCampaign(
                request.code(),
                request.name(),
                request.description(),
                request.vaccineTypeId(),
                request.targetGroupId(),
                request.serviceStartDate(),
                request.serviceEndDate(),
                request.bookingOpenAt(),
                request.bookingCloseAt()
        );

        URI location = URI.create("/api/v1/campaigns/" + created.getId());
        return ResponseEntity.created(location).body(toResponseWithQuestionnaire(created));
    }

    /** Read the full details of one campaign. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'HEALTHCARE_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable Long id) {
        VaccinationCampaign campaign = campaignService.getRequiredCampaignById(id);
        return ResponseEntity.ok(toResponseWithQuestionnaire(campaign));
    }

    /** Update campaign details; the service checks business rules. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignRequest request
    ) {
        VaccinationCampaign updated = campaignService.updateCampaign(
                id,
                request.name(),
                request.description(),
                request.serviceStartDate(),
                request.serviceEndDate(),
                request.bookingOpenAt(),
                request.bookingCloseAt()
        );
        return ResponseEntity.ok(toResponseWithQuestionnaire(updated));
    }

    private CampaignResponse toResponseWithQuestionnaire(VaccinationCampaign campaign) {
        if (campaign == null) {
            return null;
        }
        Long assignedVersionId = campaignService.getAssignedQuestionnaireVersionId(campaign.getId());
        return CampaignDtoMapper.toResponse(campaign, assignedVersionId);
    }

    /** Read the campaign list, optionally filtered by status. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'HEALTHCARE_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CampaignSummaryResponse>> getCampaigns(
            @RequestParam(required = false) CampaignStatus status
    ) {
        List<CampaignSummaryResponse> response = campaignService.getCampaignSummaries(status);
        return ResponseEntity.ok(response);
    }

    /** Read the history of campaign status changes. */
    @GetMapping("/{id}/lifecycle-events")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CampaignLifecycleEventResponse>> getCampaignLifecycleEvents(@PathVariable Long id) {
        List<CampaignLifecycleEvent> events = campaignService.getCampaignLifecycleEvents(id);
        List<CampaignLifecycleEventResponse> response = events.stream()
                .map(CampaignDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /** Move a draft campaign to Scheduled. */
    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> scheduleCampaign(@PathVariable Long id) {
        VaccinationCampaign scheduled = campaignService.scheduleCampaign(id);
        return ResponseEntity.ok(toResponseWithQuestionnaire(scheduled));
    }

    /** Publish a campaign after the service checks its setup. */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> publishCampaign(@PathVariable Long id) {
        VaccinationCampaign published = campaignService.publishCampaign(id);
        return ResponseEntity.ok(toResponseWithQuestionnaire(published));
    }

    /** Activate a published campaign only within its service dates. */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> activateCampaign(@PathVariable Long id) {
        VaccinationCampaign activated = campaignService.activateCampaign(id);
        return ResponseEntity.ok(toResponseWithQuestionnaire(activated));
    }

    /** Pause a campaign and record the reason and acting user. */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> suspendCampaign(
            @PathVariable Long id,
            @Valid @RequestBody SuspendCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String trustedActorId = principal != null ? String.valueOf(principal.getUserId()) : request.actorId();
        VaccinationCampaign suspended = campaignService.suspendCampaign(id, request.reason(), trustedActorId);
        return ResponseEntity.ok(toResponseWithQuestionnaire(suspended));
    }

    /** Resume a suspended campaign and record the reason. */
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> resumeCampaign(
            @PathVariable Long id,
            @Valid @RequestBody ResumeCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String trustedActorId = principal != null ? String.valueOf(principal.getUserId()) : request.actorId();
        VaccinationCampaign resumed = campaignService.resumeCampaign(id, request.reason(), trustedActorId);
        return ResponseEntity.ok(toResponseWithQuestionnaire(resumed));
    }

    /** Mark a campaign as finished and keep its history. */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> completeCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CompleteCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String trustedActorId = principal != null ? String.valueOf(principal.getUserId()) : "system";
        VaccinationCampaign completed = campaignService.completeCampaign(id, request.reason(), trustedActorId);
        return ResponseEntity.ok(toResponseWithQuestionnaire(completed));
    }

    /** Cancel a campaign while keeping its record in the database. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignResponse> cancelCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CancelCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String trustedActorId = principal != null ? String.valueOf(principal.getUserId()) : "system";
        VaccinationCampaign cancelled = campaignService.cancelCampaign(id, request.reason(), trustedActorId);
        return ResponseEntity.ok(toResponseWithQuestionnaire(cancelled));
    }

    /** Archive a completed or cancelled campaign; only admins can do this. */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampaignResponse> archiveCampaign(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveCampaignRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String trustedActorId = principal != null ? String.valueOf(principal.getUserId()) : "system";
        VaccinationCampaign archived = campaignService.archiveCampaign(id, request.reason(), trustedActorId);
        return ResponseEntity.ok(toResponseWithQuestionnaire(archived));
    }
}
