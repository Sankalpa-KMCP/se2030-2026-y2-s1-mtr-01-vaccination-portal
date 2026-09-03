package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.ScreeningResubmissionRequest;
import com.se2030.vaccinationportal.screening.dto.ScreeningSubmissionRequest;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.service.ScreeningSubmissionService;
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

    public ScreeningSubmissionController(
            ScreeningSubmissionService submissionService,
            ScreeningSubmissionMapper submissionMapper
    ) {
        this.submissionService = submissionService;
        this.submissionMapper = submissionMapper;
    }

    @PostMapping("/submissions")
    @RequireRoles({"CITIZEN", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> submitScreening(
            @Valid @RequestBody ScreeningSubmissionRequest request
    ) {
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
        ScreeningSubmission submission = submissionService.resubmitCorrections(
                submissionId,
                request.answers()
        );
        return ResponseEntity.ok(submissionMapper.toResponse(submission));
    }

    @GetMapping("/submissions/{id}")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> getSubmissionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(submissionMapper.toResponse(submissionService.getSubmissionById(id)));
    }

    @GetMapping("/citizens/{citizenId}/submissions")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<List<ScreeningSubmissionResponse>> getSubmissionsForCitizen(@PathVariable("citizenId") Long citizenId) {
        return ResponseEntity.ok(submissionMapper.toResponses(submissionService.getSubmissionsForCitizen(citizenId)));
    }

    @GetMapping("/citizens/{citizenId}/campaigns/{campaignId}/active-submission")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> getActiveSubmission(
            @PathVariable("citizenId") Long citizenId,
            @PathVariable("campaignId") Long campaignId
    ) {
        return submissionService.getActiveSubmission(citizenId, campaignId)
                .map(submissionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
