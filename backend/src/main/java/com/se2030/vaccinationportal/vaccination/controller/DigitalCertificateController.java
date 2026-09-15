package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.service.DigitalCertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST endpoints for digital vaccination certificates and verification.
 */
@RestController
@RequestMapping("/api/v1/vaccination/certificates")
public class DigitalCertificateController {

    private final DigitalCertificateService certificateService;

    public DigitalCertificateController(DigitalCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<CertificateVerificationResponse> verifyCertificate(@PathVariable String token) {
        return ResponseEntity.ok(certificateService.verifyCertificate(token));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DigitalCertificateResponse> getCertificateById(@PathVariable Long id) {
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(certificateService.getCertificateById(id))
        );
    }

    @GetMapping("/reference/{certificateReference}")
    public ResponseEntity<DigitalCertificateResponse> getCertificateByReference(
            @PathVariable String certificateReference
    ) {
        return ResponseEntity.ok(
                DigitalCertificateMapper.toResponse(
                        certificateService.getCertificateByReference(certificateReference)
                )
        );
    }
}
