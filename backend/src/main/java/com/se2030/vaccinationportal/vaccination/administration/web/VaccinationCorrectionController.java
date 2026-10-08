package com.se2030.vaccinationportal.vaccination.administration.web;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.administration.dto.ApplyVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.ReviewVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionDetailResponse;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.administration.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationCorrectionLifecycleService;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationCorrectionService;
import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for canonical correction lifecycle (PENDING -> APPROVED/REJECTED -> APPLIED). */
@RestController
@RequestMapping("/api/v1/vaccination")
public class VaccinationCorrectionController {

    private final VaccinationCorrectionService correctionService;
    private final VaccinationCorrectionLifecycleService lifecycleService;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final DigitalCertificateService certificateService;

    public VaccinationCorrectionController(
            VaccinationCorrectionService correctionService,
            VaccinationCorrectionLifecycleService lifecycleService
    ) {
        this(correctionService, lifecycleService, null, null);
    }

    @Autowired
    public VaccinationCorrectionController(
            VaccinationCorrectionService correctionService,
            VaccinationCorrectionLifecycleService lifecycleService,
            VaccinationRecordRepository vaccinationRecordRepository,
            DigitalCertificateService certificateService
    ) {
        this.correctionService = correctionService;
        this.lifecycleService = lifecycleService;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.certificateService = certificateService;
    }

    @PostMapping("/records/{recordId}/corrections")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN', 'VACCINATOR') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<VaccinationCorrectionResponse> createCorrection(
            @PathVariable Long recordId,
            @Valid @RequestBody CreateVaccinationCorrectionRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal != null && isCitizen(principal)) {
            if (vaccinationRecordRepository != null) {
                VaccinationRecord record = vaccinationRecordRepository.findById(recordId)
                        .orElseThrow(() -> new VaccinationRecordNotFoundException(
                                "Vaccination record not found with ID: " + recordId
                        ));
                if (!record.getCitizenId().equals(principal.getUserId())) {
                    throw new AccessDeniedException("Citizens may only request corrections for their own vaccination records");
                }
            }
            if (request != null && request.fieldName() != null && !request.fieldName().trim().isEmpty()) {
                String fn = request.fieldName().trim();
                if ("doseNumber".equalsIgnoreCase(fn) || "administrationTime".equalsIgnoreCase(fn)
                        || "nextDoseDate".equalsIgnoreCase(fn) || "vaccineBatchId".equalsIgnoreCase(fn)) {
                    throw new InvalidVaccinationCorrectionException(
                            "Citizens may only request corrections for certificate display issues, not clinical vaccination data or inventory batches"
                    );
                }
            }
        }

        Long actorId = principal != null ? principal.getUserId() : (request != null ? request.correctedBy() : 1L);
        VaccinationCorrection created;
        if (principal != null && isVaccinator(principal)) {
            created = correctionService.createVaccinatorAmendment(recordId, request, actorId);
        } else if (principal != null && isCitizen(principal)) {
            created = correctionService.createCitizenCorrection(recordId, request, actorId);
        } else {
            created = correctionService.createCorrection(recordId, request, actorId);
        }

        VaccinationCorrectionResponse response = VaccinationCorrectionMapper.toResponse(created);
        URI location = URI.create(
                "/api/v1/vaccination/records/" + recordId + "/corrections/" + response.id()
        );
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/records/{recordId}/corrections")
    @PreAuthorize("hasAnyRole('ADMIN', 'CITIZEN', 'VACCINATOR') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<VaccinationCorrectionResponse>> getCorrectionHistory(
            @PathVariable Long recordId,
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (certificateService != null && principal != null) {
            certificateService.assertRecordAccess(recordId, principal);
        }
        List<VaccinationCorrectionResponse> response = correctionService.getCorrectionHistory(recordId)
                .stream()
                .map(VaccinationCorrectionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/corrections")
    @PreAuthorize("hasRole('ADMIN') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<VaccinationCorrectionDetailResponse>> getAdminCorrectionRequests() {
        return ResponseEntity.ok(correctionService.getAdminCorrectionRequests());
    }

    @GetMapping("/corrections/administered")
    @PreAuthorize("hasRole('VACCINATOR') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<VaccinationCorrectionDetailResponse>> getVaccinatorCorrectionRequests(
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal == null) {
            throw new AccessDeniedException("Vaccinator authentication is required to access administered correction requests");
        }
        return ResponseEntity.ok(correctionService.getVaccinatorCorrectionRequests(principal.getUserId()));
    }

    @GetMapping("/corrections/mine")
    @PreAuthorize("hasRole('CITIZEN') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<VaccinationCorrectionDetailResponse>> getCitizenCorrectionRequests(
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal == null) {
            throw new AccessDeniedException("Citizen authentication is required to access correction requests");
        }
        return ResponseEntity.ok(correctionService.getCitizenCorrectionRequests(principal.getUserId()));
    }

    @PostMapping("/records/{recordId}/corrections/{correctionId}/approve")
    @PreAuthorize("hasRole('ADMIN') and !hasRole('SUPER_ADMIN')")
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

    @PostMapping("/records/{recordId}/corrections/{correctionId}/reject")
    @PreAuthorize("hasRole('ADMIN') and !hasRole('SUPER_ADMIN')")
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

    @PostMapping("/records/{recordId}/corrections/{correctionId}/apply")
    @PreAuthorize("hasRole('VACCINATOR') and !hasRole('SUPER_ADMIN')")
    public ResponseEntity<VaccinationCorrectionLifecycleResponse> applyCorrection(
            @PathVariable Long recordId,
            @PathVariable Long correctionId,
            @RequestBody(required = false) ApplyVaccinationCorrectionRequest body,
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal != null && vaccinationRecordRepository != null) {
            VaccinationRecord record = vaccinationRecordRepository.findById(recordId)
                    .orElseThrow(() -> new VaccinationRecordNotFoundException(
                            "Vaccination record not found with ID: " + recordId
                    ));
            if (!record.getVaccinatorId().equals(principal.getUserId())) {
                throw new AccessDeniedException("Vaccinators may only apply corrections to vaccinations they administered");
            }
        }
        Long actorId = resolveActorId(authentication, 1L);
        VaccinationCorrectionLifecycleResponse response = (body != null && body.fieldName() != null && !body.fieldName().trim().isEmpty())
                ? lifecycleService.applyApprovedCorrection(recordId, correctionId, actorId, body)
                : lifecycleService.applyApprovedCorrection(recordId, correctionId, actorId);
        return ResponseEntity.ok(response);
    }

    private UserPrincipal resolvePrincipal(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private Long resolveActorId(org.springframework.security.core.Authentication authentication, Long requestActorId) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal != null) {
            return principal.getUserId();
        }
        return requestActorId != null ? requestActorId : 1L;
    }

    private boolean isCitizen(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CITIZEN"));
    }

    private boolean isVaccinator(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_VACCINATOR"));
    }
}
