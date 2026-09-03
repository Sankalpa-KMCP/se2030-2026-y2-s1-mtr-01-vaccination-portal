package com.se2030.vaccinationportal.vaccination.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.exception.CertificateVerificationNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateVerificationTokenException;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.service.DigitalCertificateService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DigitalCertificateControllerTests {

    private static final Instant ISSUED_AT = Instant.parse("2026-09-02T08:30:00Z");

    @Mock
    private DigitalCertificateService certificateService;

    private MockMvc mockMvc;
    private DigitalCertificate certificate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DigitalCertificateController(certificateService))
                .setControllerAdvice(new VaccinationExceptionHandler())
                .build();

        certificate = new DigitalCertificate(101L, "CERT-EXISTING", "secure-token", ISSUED_AT);
        ReflectionTestUtils.setField(certificate, "id", 201L);
    }

    @Test
    void verifyValidTokenReturnsSafeValidResponse() throws Exception {
        when(certificateService.verifyCertificate("secure-token"))
                .thenReturn(verificationResponse(CertificateStatus.VALID, true));

        mockMvc.perform(get("/api/v1/vaccination/certificates/verify/secure-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateReference", is("CERT-EXISTING")))
                .andExpect(jsonPath("$.vaccinationRecordId", is(101)))
                .andExpect(jsonPath("$.status", is("VALID")))
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.verificationToken").doesNotExist())
                .andExpect(jsonPath("$.lockVersion").doesNotExist());
    }

    @Test
    void verifyRevokedTokenReturnsInvalidResponse() throws Exception {
        when(certificateService.verifyCertificate("revoked-token"))
                .thenReturn(verificationResponse(CertificateStatus.REVOKED, false));

        mockMvc.perform(get("/api/v1/vaccination/certificates/verify/revoked-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVOKED")))
                .andExpect(jsonPath("$.valid", is(false)));
    }

    @Test
    void verifyUnknownTokenReturns404() throws Exception {
        when(certificateService.verifyCertificate("unknown-token"))
                .thenThrow(new CertificateVerificationNotFoundException(
                        "No digital certificate was found for the supplied verification token"
                ));

        mockMvc.perform(get("/api/v1/vaccination/certificates/verify/unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("No digital certificate")));
    }

    @Test
    void verifyBlankTokenReturns400() throws Exception {
        when(certificateService.verifyCertificate(" "))
                .thenThrow(new InvalidCertificateVerificationTokenException(
                        "Certificate verification token is required"
                ));

        mockMvc.perform(get("/api/v1/vaccination/certificates/verify/{token}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("token is required")));
    }

    @Test
    void getCertificateByIdReturnsSafeResponse() throws Exception {
        when(certificateService.getCertificateById(201L)).thenReturn(certificate);

        mockMvc.perform(get("/api/v1/vaccination/certificates/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.certificateReference", is("CERT-EXISTING")))
                .andExpect(jsonPath("$.verificationToken").doesNotExist())
                .andExpect(jsonPath("$.lockVersion").doesNotExist());
    }

    @Test
    void getCertificateByReferenceReturnsSafeResponse() throws Exception {
        when(certificateService.getCertificateByReference("CERT-EXISTING")).thenReturn(certificate);

        mockMvc.perform(get("/api/v1/vaccination/certificates/reference/CERT-EXISTING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.status", is("VALID")))
                .andExpect(jsonPath("$.verificationToken").doesNotExist());
    }

    @Test
    void getCertificateByIdUnknownCertificateReturns404() throws Exception {
        when(certificateService.getCertificateById(999L))
                .thenThrow(new DigitalCertificateNotFoundException(
                        "Digital certificate not found with ID: 999"
                ));

        mockMvc.perform(get("/api/v1/vaccination/certificates/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void getCertificateByReferenceUnknownCertificateReturns404() throws Exception {
        when(certificateService.getCertificateByReference("CERT-UNKNOWN"))
                .thenThrow(new DigitalCertificateNotFoundException(
                        "Digital certificate not found with reference: CERT-UNKNOWN"
                ));

        mockMvc.perform(get("/api/v1/vaccination/certificates/reference/CERT-UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    private CertificateVerificationResponse verificationResponse(
            CertificateStatus status,
            boolean valid
    ) {
        return new CertificateVerificationResponse(
                "CERT-EXISTING",
                101L,
                ISSUED_AT,
                status,
                valid
        );
    }
}
