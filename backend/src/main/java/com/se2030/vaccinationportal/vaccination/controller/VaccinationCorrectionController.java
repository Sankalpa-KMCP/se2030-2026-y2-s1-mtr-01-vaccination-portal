package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.dto.ReviewVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationCorrectionResponse;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.service.VaccinationCorrectionLifecycleService;
import com.se2030.vaccinationportal.vaccination.service.VaccinationCorrectionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for canonical correction lifecycle (PENDING -> APPROVED/REJECTED -> APPLIED). */
@RestController
@RequestMapping("/api/v1/vaccination/records/{recordId}/corrections")
@PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER')")
public class VaccinationCorrectionController {

    private final VaccinationCorrectionService correctionService;
    private final VaccinationCorrectionLifecycleService lifecycleService;

    public VaccinationCorrectionController(
            VaccinationCorrectionService correctionService,
            VaccinationCorrectionLifecycleService lifecycleService
    ) {
        this.correctionService = correctionService;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping
    public ResponseEntity<VaccinationCorrectionResponse> createCorrection(
            @PathVariable Long recordId,
            @Valid @RequestBody CreateVaccinationCorrectionRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        Long actorId = resolveActorId(authentication, request != null ? request.correctedBy() : null);
        VaccinationCorrection created = correctionService.createCorrection(recordId, request, actorId);
        VaccinationCorrectionResponse response = VaccinationCorrectionMapper.toResponse(created);
        URI location = URI.create(
                "/api/v1/vaccination/records/" + recordId + "/corrections/" + response.id()
        );
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VaccinationCorrectionResponse>> getCorrectionHistory(
            @PathVariable Long recordId
    ) {
        List<VaccinationCorrectionResponse> response = correctionService.getCorrectionHistory(recordId)
                .stream()
                .map(VaccinationCorrectionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{correctionId}/approve")
    public ResponseEntity<VaccinationCorrectionResponse> approveCorrection(
            @PathVariable Long recordId,
            @PathVariable Long correctionId,
            @RequestBody(required = false) ReviewVaccinationCorrectionRequest body,
            org.springframework.security.core.Authentication authentication
    ) {
        Long reviewerId = resolveActorId(authentication, 1L);
        String reason = body != null ? body.reason() : null;
        VaccinationCorrection approved = correctionService.approveCorrection(recordId, correctionId, reviewerId, reason);
        return ResponseEntity.ok(VaccinationCorrectionMapper.toResponse(approved));
    }

    @PostMapping("/{correctionId}/reject")
    public ResponseEntity<VaccinationCorrectionResponse> rejectCorrection(
            @PathVariable Long recordId,
            @PathVariable Long correctionId,
            @Valid @RequestBody ReviewVaccinationCorrectionRequest body,
            org.springframework.security.core.Authentication authentication
    ) {
        Long reviewerId = resolveActorId(authentication, 1L);
        String reason = body != null ? body.reason() : null;
        VaccinationCorrection rejected = correctionService.rejectCorrection(recordId, correctionId, reviewerId, reason);
        return ResponseEntity.ok(VaccinationCorrectionMapper.toResponse(rejected));
    }

    @PostMapping("/{correctionId}/apply")
    public ResponseEntity<VaccinationCorrectionLifecycleResponse> applyCorrection(
            @PathVariable Long recordId,
            @PathVariable Long correctionId,
            org.springframework.security.core.Authentication authentication
    ) {
        Long actorId = resolveActorId(authentication, 1L);
        VaccinationCorrectionLifecycleResponse response =
                lifecycleService.applyApprovedCorrection(recordId, correctionId, actorId);
        return ResponseEntity.ok(response);
    }

    private Long resolveActorId(org.springframework.security.core.Authentication authentication, Long requestActorId) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return requestActorId != null ? requestActorId : 1L;
    }
}
