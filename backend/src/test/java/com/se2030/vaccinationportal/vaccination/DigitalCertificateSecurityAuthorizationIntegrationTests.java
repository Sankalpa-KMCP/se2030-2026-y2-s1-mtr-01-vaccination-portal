package com.se2030.vaccinationportal.vaccination;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DigitalCertificateSecurityAuthorizationIntegrationTests {

    private static final String BASE = "/api/v1/vaccination";

    private static final Long CITIZEN_A_ID = 501L;
    private static final Long CITIZEN_B_ID = 502L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    private VaccinationRecord recordA;
    private DigitalCertificate certA;

    private VaccinationRecord recordB;
    private DigitalCertificate certB;

    @BeforeEach
    void setUp() {
        certificateRepository.deleteAll();
        recordRepository.deleteAll();

        recordA = recordRepository.saveAndFlush(new VaccinationRecord(
                1001L,
                CITIZEN_A_ID,
                201L,
                301L,
                401L,
                501L,
                1,
                Instant.parse("2026-09-01T09:00:00Z"),
                null,
                "First dose",
                Instant.parse("2026-09-01T09:05:00Z")
        ));

        certA = certificateRepository.saveAndFlush(new DigitalCertificate(
                recordA.getId(),
                "CERT-CITIZEN-A",
                "token-citizen-a-secret",
                Instant.parse("2026-09-01T09:10:00Z"),
                null,
                501L,
                1,
                Instant.parse("2026-09-01T09:00:00Z")
        ));

        recordB = recordRepository.saveAndFlush(new VaccinationRecord(
                1002L,
                CITIZEN_B_ID,
                201L,
                301L,
                401L,
                501L,
                1,
                Instant.parse("2026-09-02T10:00:00Z"),
                null,
                "Citizen B dose",
                Instant.parse("2026-09-02T10:05:00Z")
        ));

        certB = certificateRepository.saveAndFlush(new DigitalCertificate(
                recordB.getId(),
                "CERT-CITIZEN-B",
                "token-citizen-b-secret",
                Instant.parse("2026-09-02T10:10:00Z"),
                null,
                501L,
                1,
                Instant.parse("2026-09-02T10:00:00Z")
        ));
    }

    @AfterEach
    void tearDown() {
        certificateRepository.deleteAll();
        recordRepository.deleteAll();
    }

    private String bearer(Long userId, String role) {
        return "Bearer " + jwtTokenService.generateToken(userId, "user" + userId + "@portal.test", List.of(role));
    }

    @Test
    @DisplayName("Citizen A can retrieve their own certificate by ID")
    void citizenCanRetrieveOwnCertificateById() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/{id}", certA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(certA.getId().intValue())))
                .andExpect(jsonPath("$.certificateReference", is("CERT-CITIZEN-A")))
                .andExpect(jsonPath("$.vaccinationRecordId", is(recordA.getId().intValue())))
                .andExpect(jsonPath("$.status", is("VALID")));
    }

    @Test
    @DisplayName("Citizen B cannot retrieve Citizen A's certificate by ID (IDOR forbidden)")
    void citizenCannotRetrieveOtherCitizenCertificateById() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/{id}", certA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_B_ID, "ROLE_CITIZEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A can retrieve their own certificate by reference")
    void citizenCanRetrieveOwnCertificateByReference() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/reference/{reference}", certA.getCertificateReference())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateReference", is("CERT-CITIZEN-A")))
                .andExpect(jsonPath("$.vaccinationRecordId", is(recordA.getId().intValue())));
    }

    @Test
    @DisplayName("Citizen B cannot retrieve Citizen A's certificate by reference (IDOR forbidden)")
    void citizenCannotRetrieveOtherCitizenCertificateByReference() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/reference/{reference}", certA.getCertificateReference())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_B_ID, "ROLE_CITIZEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A can retrieve certificate history for their own record")
    void citizenCanRetrieveOwnRecordCertificates() throws Exception {
        mockMvc.perform(get(BASE + "/records/{recordId}/certificates", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].certificateReference", is("CERT-CITIZEN-A")));
    }

    @Test
    @DisplayName("Citizen B cannot retrieve Citizen A's record certificate history (IDOR forbidden)")
    void citizenCannotRetrieveOtherCitizenRecordCertificates() throws Exception {
        mockMvc.perform(get(BASE + "/records/{recordId}/certificates", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_B_ID, "ROLE_CITIZEN")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_HEALTHCARE_OFFICER", "ROLE_SUPER_ADMIN"})
    @DisplayName("Authorized staff roles can access any citizen's certificate and history")
    void authorizedStaffCanAccessAnyCertificate(String role) throws Exception {
        String staffToken = bearer(999L, role);

        // Certificate by ID
        mockMvc.perform(get(BASE + "/certificates/{id}", certA.getId())
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateReference", is("CERT-CITIZEN-A")));

        // Certificate by reference
        mockMvc.perform(get(BASE + "/certificates/reference/{reference}", certA.getCertificateReference())
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateReference", is("CERT-CITIZEN-A")));

        // Certificate history by record ID
        mockMvc.perform(get(BASE + "/records/{recordId}/certificates", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].certificateReference", is("CERT-CITIZEN-A")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_VACCINATOR", "ROLE_CENTER_MANAGER", "ROLE_INVENTORY_OFFICER", "ROLE_CAMPAIGN_OFFICER"})
    @DisplayName("Unauthorized roles are forbidden from accessing certificate endpoints")
    void unauthorizedRolesCannotAccessCertificateEndpoints(String role) throws Exception {
        String unauthorizedToken = bearer(888L, role);

        mockMvc.perform(get(BASE + "/certificates/{id}", certA.getId())
                        .header(HttpHeaders.AUTHORIZATION, unauthorizedToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/certificates/reference/{reference}", certA.getCertificateReference())
                        .header(HttpHeaders.AUTHORIZATION, unauthorizedToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/records/{recordId}/certificates", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, unauthorizedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous users cannot access secured certificate endpoints")
    void anonymousCannotAccessSecuredCertificateEndpoints() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/{id}", certA.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/certificates/reference/{reference}", certA.getCertificateReference()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/records/{recordId}/certificates", recordA.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public certificate verification by token is accessible without authentication")
    void publicVerificationIsAccessibleAnonymously() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/verify/{token}", certA.getVerificationToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.certificateReference", is("CERT-CITIZEN-A")))
                .andExpect(jsonPath("$.status", is("VALID")))
                .andExpect(jsonPath("$.doseNumber", is(1)))
                .andExpect(jsonPath("$.verificationToken").doesNotExist())
                .andExpect(jsonPath("$.lockVersion").doesNotExist());
    }

    @Test
    @DisplayName("Citizen requesting non-existent certificate returns 404")
    void nonExistentCertificateReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(BASE + "/certificates/reference/{reference}", "NON-EXISTENT-REF")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Citizen A can retrieve their own vaccination history via /records/mine")
    void citizenCanRetrieveOwnVaccinationHistory() throws Exception {
        mockMvc.perform(get(BASE + "/records/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].citizenId", is(CITIZEN_A_ID.intValue())))
                .andExpect(jsonPath("$[0].certificateReference", is("CERT-CITIZEN-A")))
                .andExpect(jsonPath("$[0].certificateStatus", is("VALID")));
    }

    @Test
    @DisplayName("Citizen A can retrieve their own certificates via /certificates/mine")
    void citizenCanRetrieveOwnCertificates() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].certificateReference", is("CERT-CITIZEN-A")))
                .andExpect(jsonPath("$[0].status", is("VALID")));
    }

    @Test
    @DisplayName("Citizen A can download their own certificate PDF")
    void citizenCanDownloadOwnCertificatePdf() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/{id}/download", certA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(
                        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-CERT-CITIZEN-A.pdf\""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(org.springframework.http.MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("Citizen B cannot download Citizen A's certificate PDF (IDOR forbidden)")
    void citizenCannotDownloadOtherCitizenCertificatePdf() throws Exception {
        mockMvc.perform(get(BASE + "/certificates/{id}/download", certA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_B_ID, "ROLE_CITIZEN")))
                .andExpect(status().isForbidden());
    }
}
