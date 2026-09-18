package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.RevokeDigitalCertificateRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.se2030.vaccinationportal.vaccination.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.service.DigitalCertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for digital vaccination certificate lifecycle management and verification.
 */
@RestController
@RequestMapping("/api/v1/vaccination")
public class DigitalCertificateController {

    private final DigitalCertificateService certificateService;

    public DigitalCertificateController(DigitalCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/certificates/verify/{token}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CertificateVerificationResponse> verifyCertificate(@PathVariable String token) {
        return ResponseEntity.ok(certificateService.verifyCertificate(token));
    }

    @GetMapping("/certificates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'CITIZEN')")
    public ResponseEntity<DigitalCertificateResponse> getCertificateById(@PathVariable Long id) {
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(certificateService.getCertificateById(id))
        );
    }

    @GetMapping("/certificates/reference/{certificateReference}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'CITIZEN')")
    public ResponseEntity<DigitalCertificateResponse> getCertificateByReference(
            @PathVariable String certificateReference
    ) {
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(
                        certificateService.getCertificateByReference(certificateReference)
                )
        );
    }

    @PostMapping("/certificates/{certificateId}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER')")
    public ResponseEntity<DigitalCertificateResponse> revokeCertificate(
            @PathVariable Long certificateId,
            @Valid @RequestBody RevokeDigitalCertificateRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        Long actorId = resolveActorId(authentication, request != null ? request.revokedBy() : null);
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(
                        certificateService.revokeCertificate(
                                certificateId,
                                new RevokeDigitalCertificateRequest(request.reason(), actorId)
                        )
                )
        );
    }

    @PostMapping("/records/{recordId}/certificates/reissue")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER')")
    public ResponseEntity<CertificateReissueResponse> reissueCertificate(
            @PathVariable Long recordId,
            @Valid @RequestBody ReissueDigitalCertificateRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        Long actorId = resolveActorId(authentication, request != null ? request.reissuedBy() : null);
        return ResponseEntity.ok(
                certificateService.reissueCertificate(
                        recordId,
                        new ReissueDigitalCertificateRequest(request.reason(), actorId)
                )
        );
    }

    @GetMapping("/records/{recordId}/certificates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'CITIZEN')")
    public ResponseEntity<List<DigitalCertificateResponse>> getCertificateHistory(
            @PathVariable Long recordId
    ) {
        return ResponseEntity.ok(certificateService.getCertificateHistory(recordId));
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
