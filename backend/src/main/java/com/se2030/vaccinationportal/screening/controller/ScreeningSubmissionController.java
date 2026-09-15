package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.ScreeningResubmissionRequest;
import com.se2030.vaccinationportal.screening.dto.ScreeningSubmissionRequest;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.service.ScreeningSubmissionService;
import com.se2030.vaccinationportal.screening.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Citizen Screening Submissions:
 * - Submit campaign-specific screening responses
 * - Retrieve citizen submission history
 * - Check active submission for a campaign
 */
@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningSubmissionController {

    private final ScreeningSubmissionService submissionService;
    private final ScreeningSubmissionMapper submissionMapper;
    private final CitizenScreeningAccessService accessService;

    public ScreeningSubmissionController(
            ScreeningSubmissionService submissionService,
            ScreeningSubmissionMapper submissionMapper,
            CitizenScreeningAccessService accessService
    ) {
        this.submissionService = submissionService;
        this.submissionMapper = submissionMapper;
        this.accessService = accessService;
    }

    @PostMapping("/submissions")
    @RequireRoles({"CITIZEN", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> submitScreening(
            @Valid @RequestBody ScreeningSubmissionRequest request
    ) {
        accessService.requireCitizenIdAccess(
                request.citizenId(),
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        ScreeningSubmission submission = submissionService.submitScreening(
                request.citizenId(),
                request.campaignId(),
                request.questionnaireVersionId(),
                request.consentNoticeVersionId(),
                request.answers()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionMapper.toResponse(submission));
    }

    @PostMapping("/submissions/{submissionId}/resubmit")
    @RequireRoles({"CITIZEN", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> resubmitCorrections(
            @PathVariable("submissionId") Long submissionId,
            @Valid @RequestBody ScreeningResubmissionRequest request
    ) {
        accessService.requireSubmissionAccess(
                submissionId,
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        ScreeningSubmission submission = submissionService.resubmitCorrections(
                submissionId,
                request.answers()
        );
        return ResponseEntity.ok(submissionMapper.toResponse(submission));
    }

    @GetMapping("/submissions/{id}")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> getSubmissionById(
            @PathVariable("id") Long id
    ) {
        ScreeningSubmission submission = submissionService.getSubmissionById(id);
        accessService.requireCitizenIdAccess(
                submission.getCitizenId(),
                "ROLE_HEALTHCARE_OFFICER",
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        return ResponseEntity.ok(submissionMapper.toResponse(submission));
    }

    @GetMapping("/citizens/{citizenId}/submissions")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<List<ScreeningSubmissionResponse>> getSubmissionsForCitizen(
            @PathVariable("citizenId") Long citizenId
    ) {
        accessService.requireCitizenIdAccess(
                citizenId,
                "ROLE_HEALTHCARE_OFFICER",
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        return ResponseEntity.ok(submissionMapper.toResponses(submissionService.getSubmissionsForCitizen(citizenId)));
    }

    @GetMapping("/citizens/{citizenId}/campaigns/{campaignId}/active-submission")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> getActiveSubmission(
            @PathVariable("citizenId") Long citizenId,
            @PathVariable("campaignId") Long campaignId
    ) {
        accessService.requireCitizenIdAccess(
                citizenId,
                "ROLE_HEALTHCARE_OFFICER",
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        return submissionService.getActiveSubmission(citizenId, campaignId)
                .map(submissionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
