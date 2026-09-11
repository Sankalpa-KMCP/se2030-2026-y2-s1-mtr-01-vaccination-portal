package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.AuthorizeRescreeningRequest;
import com.se2030.vaccinationportal.screening.dto.EligibilityDecisionRequest;
import com.se2030.vaccinationportal.screening.dto.RequestCorrectionsRequest;
import com.se2030.vaccinationportal.screening.dto.RevokeAssessmentRequest;
import com.se2030.vaccinationportal.screening.dto.response.EligibilityAssessmentResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningReviewDetailsResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.mapper.EligibilityAssessmentMapper;
import com.se2030.vaccinationportal.screening.mapper.ScreeningSubmissionMapper;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import com.se2030.vaccinationportal.screening.service.CitizenScreeningAccessService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final EligibilityAssessmentMapper assessmentMapper;
    private final ScreeningSubmissionMapper submissionMapper;
    private final CitizenScreeningAccessService accessService;

    public EligibilityReviewController(
            EligibilityReviewService reviewService,
            EligibilityAssessmentMapper assessmentMapper,
            ScreeningSubmissionMapper submissionMapper,
            CitizenScreeningAccessService accessService
    ) {
        this.reviewService = reviewService;
        this.assessmentMapper = assessmentMapper;
        this.submissionMapper = submissionMapper;
        this.accessService = accessService;
    }

    @GetMapping("/assessments/queue")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<List<ScreeningSubmissionResponse>> getReviewQueue(
            @RequestParam(name = "campaignId", required = false) Long campaignId
    ) {
        return ResponseEntity.ok(submissionMapper.toResponses(reviewService.getReviewQueue(campaignId)));
    }

    @GetMapping("/submissions/{submissionId}/review-details")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningReviewDetailsResponse> getReviewDetails(
            @PathVariable("submissionId") Long submissionId
    ) {
        return ResponseEntity.ok(reviewService.getReviewDetails(submissionId));
    }

    @GetMapping("/assessments/submissions/{submissionId}")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR", "CITIZEN"})
    public ResponseEntity<EligibilityAssessmentResponse> getAssessmentBySubmissionId(
            @PathVariable("submissionId") Long submissionId
    ) {
        accessService.requireSubmissionAccess(
                submissionId,
                "ROLE_HEALTHCARE_OFFICER",
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        return reviewService.getAssessmentBySubmissionId(submissionId)
                .map(assessmentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/citizens/{citizenId}/assessments")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR", "CITIZEN"})
    public ResponseEntity<List<EligibilityAssessmentResponse>> getAssessmentsForCitizen(
            @PathVariable("citizenId") Long citizenId
    ) {
        accessService.requireCitizenIdAccess(
                citizenId,
                "ROLE_HEALTHCARE_OFFICER",
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR"
        );
        return ResponseEntity.ok(assessmentMapper.toResponses(reviewService.getAssessmentsForCitizen(citizenId)));
    }

    @GetMapping("/citizens/{citizenId}/campaigns/{campaignId}/eligibility")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR", "CITIZEN", "CAMPAIGN_OFFICER", "RECEPTION_STAFF"})
    public ResponseEntity<Map<String, Object>> checkEligibility(
            @PathVariable("citizenId") Long citizenId,
            @PathVariable("campaignId") Long campaignId
    ) {
        accessService.requireCitizenIdAccess(
                citizenId,
                "ROLE_HEALTHCARE_OFFICER",
                "ROLE_ADMIN",
                "ROLE_SYSTEM_ADMINISTRATOR",
                "ROLE_CAMPAIGN_OFFICER",
                "ROLE_RECEPTION_STAFF"
        );
        boolean eligible = reviewService.isCitizenCurrentlyEligible(citizenId, campaignId);
        return ResponseEntity.ok(Map.of(
                "citizenId", citizenId,
                "campaignId", campaignId,
                "isEligible", eligible
        ));
    }

    @PostMapping("/assessments/submissions/{submissionId}/decision")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityAssessmentResponse> recordDecision(
            @PathVariable("submissionId") Long submissionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EligibilityDecisionRequest request
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Authenticated reviewer identity is unavailable");
        }
        EligibilityAssessment assessment = reviewService.recordDecision(
                submissionId,
                principal.getUserId(),
                request.status(),
                request.reason(),
                request.clinicalNotes(),
                request.validUntil()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentMapper.toResponse(assessment));
    }

    @PostMapping("/assessments/submissions/{submissionId}/corrections")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningSubmissionResponse> requestCorrections(
            @PathVariable("submissionId") Long submissionId,
            @Valid @RequestBody RequestCorrectionsRequest request
    ) {
        ScreeningSubmission submission = reviewService.requestCorrections(submissionId, request.instructions());
        return ResponseEntity.ok(submissionMapper.toResponse(submission));
    }

    @PostMapping("/assessments/{assessmentId}/revoke")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityAssessmentResponse> revokeAssessment(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody RevokeAssessmentRequest request
    ) {
        EligibilityAssessment assessment = reviewService.revokeAssessment(
                assessmentId,
                request.reviewerId(),
                request.revocationReason()
        );
        return ResponseEntity.ok(assessmentMapper.toResponse(assessment));
    }

    @PostMapping("/assessments/{assessmentId}/rescreening")
    @RequireRoles({"HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<EligibilityAssessmentResponse> authorizeRescreening(
            @PathVariable("assessmentId") Long assessmentId,
            @Valid @RequestBody AuthorizeRescreeningRequest request
    ) {
        EligibilityAssessment assessment = reviewService.authorizeRescreening(
                assessmentId,
                request.authorizedByReviewerId(),
                request.reason()
        );
        return ResponseEntity.ok(assessmentMapper.toResponse(assessment));
    }
}
