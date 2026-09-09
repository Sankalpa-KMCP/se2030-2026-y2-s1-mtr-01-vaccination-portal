package com.se2030.vaccinationportal.vaccination.controller;

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
    public ResponseEntity<CertificateVerificationResponse> verifyCertificate(@PathVariable String token) {
        return ResponseEntity.ok(certificateService.verifyCertificate(token));
    }

    @GetMapping("/certificates/{id}")
    public ResponseEntity<DigitalCertificateResponse> getCertificateById(@PathVariable Long id) {
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(certificateService.getCertificateById(id))
        );
    }

    @GetMapping("/certificates/reference/{certificateReference}")
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
    public ResponseEntity<DigitalCertificateResponse> revokeCertificate(
            @PathVariable Long certificateId,
            @Valid @RequestBody RevokeDigitalCertificateRequest request
    ) {
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(
                        certificateService.revokeCertificate(certificateId, request)
                )
        );
    }

    @PostMapping("/records/{recordId}/certificates/reissue")
    public ResponseEntity<CertificateReissueResponse> reissueCertificate(
            @PathVariable Long recordId,
            @Valid @RequestBody ReissueDigitalCertificateRequest request
    ) {
        return ResponseEntity.ok(certificateService.reissueCertificate(recordId, request));
    }

    @GetMapping("/records/{recordId}/certificates")
    public ResponseEntity<List<DigitalCertificateResponse>> getCertificateHistory(
            @PathVariable Long recordId
    ) {
        return ResponseEntity.ok(certificateService.getCertificateHistory(recordId));
    }
}
