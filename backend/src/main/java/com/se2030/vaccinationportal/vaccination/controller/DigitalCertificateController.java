package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.CitizenCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.CitizenVaccinationRecordResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.RevokeDigitalCertificateRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.se2030.vaccinationportal.vaccination.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.service.DigitalCertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'SUPER_ADMIN', 'CITIZEN')")
    public ResponseEntity<DigitalCertificateResponse> getCertificateById(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        DigitalCertificate certificate = certificateService.getCertificateById(id);
        certificateService.assertCertificateAccess(certificate, resolvePrincipal(authentication));
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(certificate)
        );
    }

    @GetMapping("/certificates/reference/{certificateReference}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'SUPER_ADMIN', 'CITIZEN')")
    public ResponseEntity<DigitalCertificateResponse> getCertificateByReference(
            @PathVariable String certificateReference,
            org.springframework.security.core.Authentication authentication
    ) {
        DigitalCertificate certificate = certificateService.getCertificateByReference(certificateReference);
        certificateService.assertCertificateAccess(certificate, resolvePrincipal(authentication));
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(certificate)
        );
    }

    @PostMapping("/certificates/{certificateId}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'SUPER_ADMIN', 'CITIZEN')")
    public ResponseEntity<List<DigitalCertificateResponse>> getCertificateHistory(
            @PathVariable Long recordId,
            org.springframework.security.core.Authentication authentication
    ) {
        certificateService.assertRecordAccess(recordId, resolvePrincipal(authentication));
        return ResponseEntity.ok(certificateService.getCertificateHistory(recordId));
    }

    @GetMapping({"/records/mine", "/history/mine"})
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<CitizenVaccinationRecordResponse>> getMyVaccinationHistory(
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal == null) {
            throw new AccessDeniedException("Citizen authentication is required to access vaccination history");
        }
        return ResponseEntity.ok(certificateService.getCitizenVaccinationHistory(principal.getUserId()));
    }

    @GetMapping("/certificates/mine")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<CitizenCertificateResponse>> getMyCertificates(
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        if (principal == null) {
            throw new AccessDeniedException("Citizen authentication is required to access digital certificates");
        }
        return ResponseEntity.ok(certificateService.getCitizenCertificates(principal.getUserId()));
    }

    @GetMapping("/certificates/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER', 'SUPER_ADMIN', 'CITIZEN')")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        UserPrincipal principal = resolvePrincipal(authentication);
        DigitalCertificate certificate = certificateService.getCertificateById(id);
        certificateService.assertCertificateAccess(certificate, principal);
        byte[] pdfContent = certificateService.generateCertificatePdf(id, principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + certificate.getCertificateReference() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
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
}
