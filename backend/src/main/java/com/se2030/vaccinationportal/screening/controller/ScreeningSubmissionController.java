package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.ScreeningSubmissionRequest;
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

    public ScreeningSubmissionController(ScreeningSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/submissions")
    @RequireRoles({"CITIZEN", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmission> submitScreening(
            @Valid @RequestBody ScreeningSubmissionRequest request
    ) {
        ScreeningSubmission submission = submissionService.submitScreening(
                request.citizenId(),
                request.campaignId(),
                request.questionnaireVersionId(),
                request.consentNoticeVersionId(),
                request.answers()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(submission);
    }

    @GetMapping("/submissions/{id}")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmission> getSubmissionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    @GetMapping("/citizens/{citizenId}/submissions")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<List<ScreeningSubmission>> getSubmissionsForCitizen(@PathVariable("citizenId") Long citizenId) {
        return ResponseEntity.ok(submissionService.getSubmissionsForCitizen(citizenId));
    }

    @GetMapping("/citizens/{citizenId}/campaigns/{campaignId}/active-submission")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmission> getActiveSubmission(
            @PathVariable("citizenId") Long citizenId,
            @PathVariable("campaignId") Long campaignId
    ) {
        return submissionService.getActiveSubmission(citizenId, campaignId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
