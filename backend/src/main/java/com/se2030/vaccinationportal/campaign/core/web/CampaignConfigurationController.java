package com.se2030.vaccinationportal.campaign.core.web;

import com.se2030.vaccinationportal.campaign.questionnaire.dto.request.AssignQuestionnaireRequest;
import com.se2030.vaccinationportal.campaign.center.dto.request.CreateCenterAssignmentRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.CreateDoseProgrammeRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.CreateDoseProgrammeVersionRequest;
import com.se2030.vaccinationportal.campaign.core.dto.request.CreateLocationTargetRequest;
import com.se2030.vaccinationportal.campaign.center.dto.request.EndCenterAssignmentRequest;
import com.se2030.vaccinationportal.campaign.center.dto.request.UpdateCenterAssignmentRequest;
import com.se2030.vaccinationportal.campaign.center.dto.response.CampaignCenterAssignmentResponse;
import com.se2030.vaccinationportal.campaign.core.dto.response.CampaignDoseProgrammeResponse;
import com.se2030.vaccinationportal.campaign.core.dto.response.CampaignLocationTargetResponse;
import com.se2030.vaccinationportal.campaign.questionnaire.dto.response.CampaignQuestionnaireAssignmentResponse;
import com.se2030.vaccinationportal.campaign.core.mapper.CampaignConfigurationDtoMapper;
import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.core.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.questionnaire.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.center.service.CampaignCenterAssignmentService;
import com.se2030.vaccinationportal.campaign.core.service.CampaignDoseProgrammeService;
import com.se2030.vaccinationportal.campaign.core.service.CampaignLocationTargetService;
import com.se2030.vaccinationportal.campaign.questionnaire.service.CampaignQuestionnaireAssignmentService;
import com.se2030.vaccinationportal.campaign.core.service.VaccinationCampaignService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing Campaign Configuration endpoints for dose programmes,
 * questionnaire assignments, center assignments, and location targets.
 */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignConfigurationController {

    private final CampaignDoseProgrammeService doseProgrammeService;
    private final CampaignQuestionnaireAssignmentService questionnaireAssignmentService;
    private final CampaignCenterAssignmentService centerAssignmentService;
    private final CampaignLocationTargetService locationTargetService;
    private final VaccinationCampaignService campaignService;

    public CampaignConfigurationController(
            CampaignDoseProgrammeService doseProgrammeService,
            CampaignQuestionnaireAssignmentService questionnaireAssignmentService,
            CampaignCenterAssignmentService centerAssignmentService,
            CampaignLocationTargetService locationTargetService,
            VaccinationCampaignService campaignService
    ) {
        this.doseProgrammeService = doseProgrammeService;
        this.questionnaireAssignmentService = questionnaireAssignmentService;
        this.centerAssignmentService = centerAssignmentService;
        this.locationTargetService = locationTargetService;
        this.campaignService = campaignService;
    }

    // ---------------------------------------------------------------------------------------------
    // Dose Programme Endpoints (3)
    // ---------------------------------------------------------------------------------------------

    @PostMapping("/{id}/dose-programmes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignDoseProgrammeResponse> createInitialDoseProgramme(
            @PathVariable Long id,
            @Valid @RequestBody CreateDoseProgrammeRequest request
    ) {
        CampaignDoseProgramme created = doseProgrammeService.createInitialProgramme(
                id,
                request.doseCategory(),
                request.permittedDoseNumbers(),
                request.minimumIntervalDays(),
                request.isPreviousEvidenceRequired(),
                request.expectedServiceMinutes(),
                request.activeFrom()
        );

        URI location = URI.create("/api/v1/campaigns/" + id + "/dose-programmes");
        return ResponseEntity.created(location).body(CampaignConfigurationDtoMapper.toResponse(created));
    }

    @PostMapping("/{id}/dose-programmes/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignDoseProgrammeResponse> createNewDoseProgrammeVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateDoseProgrammeVersionRequest request
    ) {
        CampaignDoseProgramme created = doseProgrammeService.createNewVersion(
                id,
                request.doseCategory(),
                request.permittedDoseNumbers(),
                request.minimumIntervalDays(),
                request.isPreviousEvidenceRequired(),
                request.expectedServiceMinutes(),
                request.effectiveStartDate()
        );

        URI location = URI.create("/api/v1/campaigns/" + id + "/dose-programmes");
        return ResponseEntity.created(location).body(CampaignConfigurationDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}/dose-programmes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CampaignDoseProgrammeResponse>> getDoseProgrammeHistory(@PathVariable Long id) {
        campaignService.getRequiredCampaignById(id);
        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(id);
        List<CampaignDoseProgrammeResponse> response = history.stream()
                .map(CampaignConfigurationDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------------------------------
    // Questionnaire Assignment Endpoints (2)
    // ---------------------------------------------------------------------------------------------

    @PostMapping("/{id}/questionnaire-assignments")
    @PreAuthorize("hasRole('HEALTHCARE_OFFICER') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<CampaignQuestionnaireAssignmentResponse> assignQuestionnaireVersion(
            @PathVariable Long id,
            @Valid @RequestBody AssignQuestionnaireRequest request
    ) {
        CampaignQuestionnaireAssignment created = questionnaireAssignmentService.assignQuestionnaireVersion(
                id,
                request.questionnaireVersionId(),
                request.effectiveStartDate()
        );

        URI location = URI.create("/api/v1/campaigns/" + id + "/questionnaire-assignments");
        return ResponseEntity.created(location).body(CampaignConfigurationDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}/questionnaire-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'HEALTHCARE_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CampaignQuestionnaireAssignmentResponse>> getQuestionnaireAssignmentHistory(
            @PathVariable Long id
    ) {
        campaignService.getRequiredCampaignById(id);
        List<CampaignQuestionnaireAssignment> history = questionnaireAssignmentService.getAssignmentHistory(id);
        List<CampaignQuestionnaireAssignmentResponse> response = history.stream()
                .map(CampaignConfigurationDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------------------------------
    // Center Assignment Endpoints (5)
    // ---------------------------------------------------------------------------------------------

    @PostMapping("/{id}/center-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignCenterAssignmentResponse> createCenterAssignment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCenterAssignmentRequest request
    ) {
        CampaignCenterAssignment created = centerAssignmentService.createAssignment(
                id,
                request.centerId(),
                request.activeFrom(),
                request.activeTo()
        );

        URI location = URI.create("/api/v1/campaigns/" + id + "/center-assignments/" + created.getId());
        return ResponseEntity.created(location).body(CampaignConfigurationDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}/center-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'MANAGEMENT_USER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CampaignCenterAssignmentResponse>> getCenterAssignments(@PathVariable Long id) {
        campaignService.getRequiredCampaignById(id);
        List<CampaignCenterAssignment> assignments = centerAssignmentService.getAssignmentsForCampaign(id);
        List<CampaignCenterAssignmentResponse> response = assignments.stream()
                .map(CampaignConfigurationDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/center-assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignCenterAssignmentResponse> updateFutureCenterAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @Valid @RequestBody UpdateCenterAssignmentRequest request
    ) {
        CampaignCenterAssignment updated = centerAssignmentService.updateFutureAssignment(
                id,
                assignmentId,
                request.activeFrom(),
                request.activeTo()
        );
        return ResponseEntity.ok(CampaignConfigurationDtoMapper.toResponse(updated));
    }

    @PostMapping("/{id}/center-assignments/{assignmentId}/end-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignCenterAssignmentResponse> endCenterAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @Valid @RequestBody EndCenterAssignmentRequest request
    ) {
        CampaignCenterAssignment ended = centerAssignmentService.endAssignment(
                id,
                assignmentId,
                request.activeTo()
        );
        return ResponseEntity.ok(CampaignConfigurationDtoMapper.toResponse(ended));
    }

    @PostMapping("/{id}/center-assignments/{assignmentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignCenterAssignmentResponse> cancelFutureCenterAssignment(
            @PathVariable Long id,
            @PathVariable Long assignmentId
    ) {
        CampaignCenterAssignment cancelled = centerAssignmentService.cancelFutureAssignment(id, assignmentId);
        return ResponseEntity.ok(CampaignConfigurationDtoMapper.toResponse(cancelled));
    }

    // ---------------------------------------------------------------------------------------------
    // Location Target Endpoints (2)
    // ---------------------------------------------------------------------------------------------

    @PostMapping("/{id}/location-targets")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<CampaignLocationTargetResponse> addLocationTarget(
            @PathVariable Long id,
            @Valid @RequestBody CreateLocationTargetRequest request
    ) {
        CampaignLocationTarget created = locationTargetService.addTarget(
                id,
                request.targetType(),
                request.targetCode()
        );

        URI location = URI.create("/api/v1/campaigns/" + id + "/location-targets");
        return ResponseEntity.created(location).body(CampaignConfigurationDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}/location-targets")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CampaignLocationTargetResponse>> getLocationTargets(
            @PathVariable Long id,
            @RequestParam(required = false) LocationTargetType type
    ) {
        campaignService.getRequiredCampaignById(id);
        List<CampaignLocationTarget> targets = type != null
                ? locationTargetService.getTargetsForCampaignAndType(id, type)
                : locationTargetService.getTargetsForCampaign(id);
        List<CampaignLocationTargetResponse> response = targets.stream()
                .map(CampaignConfigurationDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
