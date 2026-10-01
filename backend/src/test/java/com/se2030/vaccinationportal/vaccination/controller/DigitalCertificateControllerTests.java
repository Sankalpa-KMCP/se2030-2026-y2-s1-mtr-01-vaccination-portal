package com.se2030.vaccinationportal.vaccination.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.dto.AvailableVaccineBatchResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.RevokeDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinatorVaccinationRecordResponse;
import com.se2030.vaccinationportal.vaccination.exception.CertificateAlreadyRevokedException;
import com.se2030.vaccinationportal.vaccination.exception.CertificateVerificationNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateVerificationTokenException;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.service.DigitalCertificateService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

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
                .andExpect(jsonPath("$.doseNumber", is(2)))
                .andExpect(jsonPath("$.administrationTime", is(ISSUED_AT.toString())))
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

    @Test
    void revokeCertificateReturnsSafeLifecycleResponse() throws Exception {
        certificate.revoke(ISSUED_AT.plusSeconds(60), 501L, "Incorrect certificate details");
        when(certificateService.revokeCertificate(
                201L,
                new RevokeDigitalCertificateRequest("Incorrect certificate details", 501L)
        )).thenReturn(certificate);

        mockMvc.perform(post("/api/v1/vaccination/certificates/201/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Incorrect certificate details","revokedBy":501}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVOKED")))
                .andExpect(jsonPath("$.revokedBy", is(501)))
                .andExpect(jsonPath("$.revocationReason", is("Incorrect certificate details")))
                .andExpect(jsonPath("$.verificationToken").doesNotExist())
                .andExpect(jsonPath("$.lockVersion").doesNotExist());
    }

    @Test
    void alreadyRevokedCertificateReturnsConflict() throws Exception {
        when(certificateService.revokeCertificate(
                201L,
                new RevokeDigitalCertificateRequest("Again", 501L)
        )).thenThrow(new CertificateAlreadyRevokedException("Digital certificate is already revoked"));

        mockMvc.perform(post("/api/v1/vaccination/certificates/201/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Again","revokedBy":501}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void blankRevokeReasonReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/vaccination/certificates/201/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":" ","revokedBy":501}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("reason")));
    }

    @Test
    void reissueReturnsBothSafeLinkedCertificates() throws Exception {
        DigitalCertificate oldCertificate = new DigitalCertificate(
                101L, "CERT-OLD", "old-token", ISSUED_AT
        );
        ReflectionTestUtils.setField(oldCertificate, "id", 201L);
        oldCertificate.supersede(ISSUED_AT.plusSeconds(60), 501L, "Correction approved");
        DigitalCertificate newCertificate = new DigitalCertificate(
                101L, "CERT-NEW", "new-token", ISSUED_AT.plusSeconds(60), 201L
        );
        ReflectionTestUtils.setField(newCertificate, "id", 202L);
        oldCertificate.markSupersededBy(202L);
        CertificateReissueResponse response = new CertificateReissueResponse(
                DigitalCertificateMapper.toResponse(oldCertificate),
                DigitalCertificateMapper.toResponse(newCertificate)
        );
        when(certificateService.reissueCertificate(
                101L,
                new ReissueDigitalCertificateRequest("Correction approved", 501L)
        )).thenReturn(response);

        mockMvc.perform(post("/api/v1/vaccination/records/101/certificates/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Correction approved","reissuedBy":501}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supersededCertificate.id", is(201)))
                .andExpect(jsonPath("$.supersededCertificate.status", is("SUPERSEDED")))
                .andExpect(jsonPath("$.supersededCertificate.supersededByCertificateId", is(202)))
                .andExpect(jsonPath("$.issuedCertificate.id", is(202)))
                .andExpect(jsonPath("$.issuedCertificate.status", is("VALID")))
                .andExpect(jsonPath("$.issuedCertificate.reissuedFromCertificateId", is(201)))
                .andExpect(jsonPath("$..verificationToken").doesNotExist())
                .andExpect(jsonPath("$..lockVersion").doesNotExist());
    }

    @Test
    void historyReturnsDeterministicSafeResponses() throws Exception {
        DigitalCertificateResponse first = DigitalCertificateMapper.toResponse(certificate);
        DigitalCertificate secondEntity = new DigitalCertificate(
                101L, "CERT-SECOND", "second-token", ISSUED_AT.plusSeconds(60), 201L
        );
        ReflectionTestUtils.setField(secondEntity, "id", 202L);
        DigitalCertificateResponse second = DigitalCertificateMapper.toResponse(secondEntity);
        when(certificateService.getCertificateHistory(101L)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/vaccination/records/101/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(201)))
                .andExpect(jsonPath("$[1].id", is(202)))
                .andExpect(jsonPath("$..verificationToken").doesNotExist())
                .andExpect(jsonPath("$..lockVersion").doesNotExist());
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
                2,
                ISSUED_AT,
                valid
        );
    }

    @Test
    void getMyVaccinatorRecordsReturnsRecords() throws Exception {
        UserPrincipal principal = new UserPrincipal(401L, "vaccinator@portal.test", "hash", true, List.of(new SimpleGrantedAuthority("ROLE_VACCINATOR")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        VaccinatorVaccinationRecordResponse resp = new VaccinatorVaccinationRecordResponse(
                10L, 1001L, 501L, "Citizen A", 201L, "Campaign 1", 301L, "Center 1",
                401L, "BATCH-1", "Pfizer", 1, ISSUED_AT, null, "Notes", ISSUED_AT,
                201L, "CERT-REF-1", CertificateStatus.VALID
        );
        when(certificateService.getVaccinatorVaccinationHistory(401L)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/vaccination/records/vaccinator/mine").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].certificateReference", is("CERT-REF-1")));
    }

    @Test
    void getRecordByIdReturnsDetails() throws Exception {
        UserPrincipal principal = new UserPrincipal(401L, "vaccinator@portal.test", "hash", true, List.of(new SimpleGrantedAuthority("ROLE_VACCINATOR")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        VaccinatorVaccinationRecordResponse resp = new VaccinatorVaccinationRecordResponse(
                10L, 1001L, 501L, "Citizen A", 201L, "Campaign 1", 301L, "Center 1",
                401L, "BATCH-1", "Pfizer", 1, ISSUED_AT, null, "Notes", ISSUED_AT,
                201L, "CERT-REF-1", CertificateStatus.VALID
        );
        when(certificateService.getVaccinationRecordDetails(10L)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/vaccination/records/10").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.citizenName", is("Citizen A")));
    }

    @Test
    void getAvailableBatchesReturnsCompatibleBatches() throws Exception {
        AvailableVaccineBatchResponse batch = new AvailableVaccineBatchResponse(
                501L, "BATCH-A", "Pfizer", java.time.LocalDate.parse("2026-12-31"), 50
        );
        when(certificateService.getAvailableBatchesForAppointment(1001L)).thenReturn(List.of(batch));

        mockMvc.perform(get("/api/v1/vaccination/batches/available").param("appointmentId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(501)))
                .andExpect(jsonPath("$[0].batchNumber", is("BATCH-A")));
    }
}
