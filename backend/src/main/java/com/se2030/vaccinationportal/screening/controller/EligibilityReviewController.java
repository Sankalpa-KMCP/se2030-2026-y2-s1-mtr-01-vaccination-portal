package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.AuthorizeRescreeningRequest;
import com.se2030.vaccinationportal.screening.dto.EligibilityDecisionRequest;
import com.se2030.vaccinationportal.screening.dto.RequestCorrectionsRequest;
import com.se2030.vaccinationportal.screening.dto.RevokeAssessmentRequest;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Healthcare Officer Eligibility Reviews & Decisions:
 * - Pending review queue
 * - Final clinical assessments (ELIGIBLE / INELIGIBLE)
 * - Correction requests
 * - Assessment revocations
 * - Rescreening authorizations
 * - Real-time active eligibility verification
 */
@RestController
@RequestMapping("/api/v1/screening")
public class EligibilityReviewController {

    private final EligibilityReviewService reviewService;

    public EligibilityReviewController(EligibilityReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/assessments/queue")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<List<ScreeningSubmission>> getReviewQueue(
            @RequestParam(name = "campaignId", required = false) Long campaignId
    ) {
        return ResponseEntity.ok(reviewService.getReviewQueue(campaignId));
    }

    @GetMapping("/assessments/submissions/{submissionId}")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR", "CITIZEN"})
    public ResponseEntity<EligibilityAssessment> getAssessmentBySubmissionId(
            @PathVariable("submissionId") Long submissionId
    ) {
        return reviewService.getAssessmentBySubmissionId(submissionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/citizens/{citizenId}/assessments")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR", "CITIZEN"})
    public ResponseEntity<List<EligibilityAssessment>> getAssessmentsForCitizen(
            @PathVariable("citizenId") Long citizenId
    ) {
        return ResponseEntity.ok(reviewService.getAssessmentsForCitizen(citizenId));
    }

    @GetMapping("/citizens/{citizenId}/campaigns/{campaignId}/eligibility")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR", "CITIZEN", "CAMPAIGN_OFFICER", "RECEPTION_STAFF"})
    public ResponseEntity<Map<String, Object>> checkEligibility(
            @PathVariable("citizenId") Long citizenId,
            @PathVariable("campaignId") Long campaignId
    ) {
        boolean eligible = reviewService.isCitizenCurrentlyEligible(citizenId, campaignId);
        return ResponseEntity.ok(Map.of(
                "citizenId", citizenId,
                "campaignId", campaignId,
                "isEligible", eligible
        ));
    }

    @PostMapping("/assessments/submissions/{submissionId}/decision")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityAssessment> recordDecision(
            @PathVariable("submissionId") Long submissionId,
            @Valid @RequestBody EligibilityDecisionRequest request
    ) {
        EligibilityAssessment assessment = reviewService.recordDecision(
                submissionId,
                request.reviewerId(),
                request.status(),
                request.reason(),
                request.clinicalNotes(),
                request.validUntil()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(assessment);
    }

    @PostMapping("/assessments/submissions/{submissionId}/corrections")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmission> requestCorrections(
            @PathVariable("submissionId") Long submissionId,
            @Valid @RequestBody RequestCorrectionsRequest request
    ) {
        ScreeningSubmission submission = reviewService.requestCorrections(submissionId, request.instructions());
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/assessments/{assessmentId}/revoke")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityAssessment> revokeAssessment(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody RevokeAssessmentRequest request
    ) {
        EligibilityAssessment assessment = reviewService.revokeAssessment(
                assessmentId,
                request.reviewerId(),
                request.revocationReason()
        );
        return ResponseEntity.ok(assessment);
    }

    @PostMapping("/assessments/{assessmentId}/rescreening")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityAssessment> authorizeRescreening(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody AuthorizeRescreeningRequest request
    ) {
        EligibilityAssessment assessment = reviewService.authorizeRescreening(
                assessmentId,
                request.authorizedByReviewerId(),
                request.reason()
        );
        return ResponseEntity.ok(assessment);
    }
}
