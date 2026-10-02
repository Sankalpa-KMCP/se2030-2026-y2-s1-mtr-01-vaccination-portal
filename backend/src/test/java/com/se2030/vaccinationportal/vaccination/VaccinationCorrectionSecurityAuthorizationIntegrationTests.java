package com.se2030.vaccinationportal.vaccination;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationCorrectionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VaccinationCorrectionSecurityAuthorizationIntegrationTests {

    private static final String BASE = "/api/v1/vaccination";

    private static final Long CITIZEN_A_ID = 501L;
    private static final Long CITIZEN_B_ID = 502L;
    private static final Long VACCINATOR_A_ID = 401L;
    private static final Long VACCINATOR_B_ID = 402L;
    private static final Long ADMIN_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @Autowired
    private VaccinationCorrectionRepository correctionRepository;

    private VaccinationRecord recordA;
    private DigitalCertificate certA;

    private VaccinationRecord recordB;
    private DigitalCertificate certB;

    @BeforeEach
    void setUp() {
        correctionRepository.deleteAll();
        certificateRepository.deleteAll();
        recordRepository.deleteAll();

        recordA = recordRepository.saveAndFlush(new VaccinationRecord(
                1001L,
                CITIZEN_A_ID,
                201L,
                301L,
                VACCINATOR_A_ID,
                501L,
                1,
                Instant.parse("2026-09-01T09:00:00Z"),
                null,
                "First dose administered",
                Instant.parse("2026-09-01T09:05:00Z")
        ));

        certA = certificateRepository.saveAndFlush(new DigitalCertificate(
                recordA.getId(),
                "CERT-CITIZEN-A-01",
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
                VACCINATOR_B_ID,
                502L,
                1,
                Instant.parse("2026-09-02T10:00:00Z"),
                null,
                "Citizen B dose",
                Instant.parse("2026-09-02T10:05:00Z")
        ));

        certB = certificateRepository.saveAndFlush(new DigitalCertificate(
                recordB.getId(),
                "CERT-CITIZEN-B-01",
                "token-citizen-b-secret",
                Instant.parse("2026-09-02T10:10:00Z"),
                null,
                502L,
                1,
                Instant.parse("2026-09-02T10:00:00Z")
        ));
    }

    @AfterEach
    void tearDown() {
        correctionRepository.deleteAll();
        certificateRepository.deleteAll();
        recordRepository.deleteAll();
    }

    private String bearer(Long userId, String role) {
        return "Bearer " + jwtTokenService.generateToken(userId, "user" + userId + "@portal.test", List.of(role));
    }

    // --- CITIZEN TESTS ---

    @Test
    @DisplayName("1. Citizen can submit correction request for own record")
    void citizenCanSubmitCorrectionRequestForOwnRecord() throws Exception {
        mockMvc.perform(post(BASE + "/records/{recordId}/corrections", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"My dose number was entered as 1 but was actually dose 2\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vaccinationRecordId", is(recordA.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.reason", is("My dose number was entered as 1 but was actually dose 2")))
                .andExpect(jsonPath("$.correctedBy", is(CITIZEN_A_ID.intValue())));

        assertEquals(1, correctionRepository.count());
    }

    @Test
    @DisplayName("2. Citizen cannot submit correction request for another citizen's record")
    void citizenCannotSubmitCorrectionForAnotherCitizen() throws Exception {
        mockMvc.perform(post(BASE + "/records/{recordId}/corrections", recordB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Attempting to request correction on another citizen record\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Message/reason is required when submitting a correction request")
    void messageIsRequired() throws Exception {
        mockMvc.perform(post(BASE + "/records/{recordId}/corrections", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("reason")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject", "apply"})
    @DisplayName("4. Citizen cannot approve, reject, or apply correction requests")
    void citizenCannotReviewOrApplyCorrections(String action) throws Exception {
        VaccinationCorrection pending = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "First dose administered",
                null,
                "Citizen note",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/" + action, recordA.getId(), pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Unauthorized action\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Citizen can view own correction requests")
    void citizenCanViewOwnCorrectionRequests() throws Exception {
        correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "First dose administered",
                null,
                "Citizen note for record A",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(get(BASE + "/records/{recordId}/corrections", recordA.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        mockMvc.perform(get(BASE + "/corrections/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(CITIZEN_A_ID, "ROLE_CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vaccinationRecordId", is(recordA.getId().intValue())));
    }

    // --- ADMIN TESTS ---

    @Test
    @DisplayName("6. Admin can list all correction requests")
    void adminCanListCorrectionRequests() throws Exception {
        correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "First dose administered",
                null,
                "Citizen note for record A",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));
        correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordB.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Citizen B dose",
                null,
                "Citizen note for record B",
                CITIZEN_B_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(get(BASE + "/corrections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("7. Admin can approve a PENDING correction request")
    void adminCanApprovePendingCorrection() throws Exception {
        VaccinationCorrection pending = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "First dose administered",
                null,
                "Citizen note for record A",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/approve", recordA.getId(), pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Verified with clinic records\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        VaccinationCorrection updated = correctionRepository.findById(pending.getId()).orElseThrow();
        assertEquals(VaccinationCorrectionStatus.APPROVED, updated.getStatus());
        assertEquals(ADMIN_ID, updated.getReviewedBy());
    }

    @Test
    @DisplayName("8. Admin can reject a PENDING correction request")
    void adminCanRejectPendingCorrection() throws Exception {
        VaccinationCorrection pending = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "First dose administered",
                null,
                "Citizen note for record A",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/reject", recordA.getId(), pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Invalid reason provided\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        VaccinationCorrection updated = correctionRepository.findById(pending.getId()).orElseThrow();
        assertEquals(VaccinationCorrectionStatus.REJECTED, updated.getStatus());
    }

    @Test
    @DisplayName("9. Admin cannot apply clinical correction")
    void adminCannotApplyClinicalCorrection() throws Exception {
        VaccinationCorrection approved = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.DOSE_NUMBER,
                "1",
                "2",
                "Citizen note",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.APPROVED
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/apply", recordA.getId(), approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldName\":\"doseNumber\",\"newValue\":\"2\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10 & 11. Admin approval alone does NOT modify VaccinationRecord or issue certificate")
    void adminApprovalAloneDoesNotModifyRecordOrIssueCertificate() throws Exception {
        VaccinationCorrection pending = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.DOSE_NUMBER,
                "1",
                "2",
                "Dose correction",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/approve", recordA.getId(), pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_ID, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Approved for vaccinator action\"}"))
                .andExpect(status().isOk());

        // VaccinationRecord remains unchanged
        VaccinationRecord record = recordRepository.findById(recordA.getId()).orElseThrow();
        assertEquals(1, record.getDoseNumber());

        // DigitalCertificate remains exactly 1 and VALID
        List<DigitalCertificate> certs = certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(recordA.getId());
        assertEquals(1, certs.size());
        assertEquals(CertificateStatus.VALID, certs.get(0).getStatus());
    }

    // --- VACCINATOR TESTS ---

    @Test
    @DisplayName("12 & 13. Vaccinator can see requests for administered records only")
    void vaccinatorCanSeeAdministeredRequestsOnly() throws Exception {
        correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Note A",
                null,
                "Request A",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));
        correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordB.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Note B",
                null,
                "Request B",
                CITIZEN_B_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        // Vaccinator A sees only record A
        mockMvc.perform(get(BASE + "/corrections/administered")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vaccinationRecordId", is(recordA.getId().intValue())));

        // Vaccinator B sees only record B
        mockMvc.perform(get(BASE + "/corrections/administered")
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_B_ID, "ROLE_VACCINATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vaccinationRecordId", is(recordB.getId().intValue())));
    }

    @Test
    @DisplayName("14. PENDING request cannot be applied by Vaccinator")
    void vaccinatorCannotApplyPendingCorrection() throws Exception {
        VaccinationCorrection pending = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.DOSE_NUMBER,
                "1",
                "2",
                "Pending dose fix",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/apply", recordA.getId(), pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldName\":\"doseNumber\",\"newValue\":\"2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Only APPROVED")));
    }

    @Test
    @DisplayName("15. REJECTED request cannot be applied by Vaccinator")
    void vaccinatorCannotApplyRejectedCorrection() throws Exception {
        VaccinationCorrection rejected = new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.DOSE_NUMBER,
                "1",
                "2",
                "Rejected dose fix",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        );
        rejected.reject(ADMIN_ID, Instant.now(), "Rejected by Admin");
        rejected = correctionRepository.saveAndFlush(rejected);

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/apply", recordA.getId(), rejected.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldName\":\"doseNumber\",\"newValue\":\"2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Only APPROVED")));
    }

    @Test
    @DisplayName("16-21. Vaccinator can apply APPROVED correction: updates allowed fields, supersedes old cert, issues new VALID cert")
    void vaccinatorAppliesApprovedCorrectionLifecycle() throws Exception {
        VaccinationCorrection approved = new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.DOSE_NUMBER,
                "1",
                null,
                "Citizen requested dose number 2",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        );
        approved.approve(ADMIN_ID, Instant.now(), "Approved by Admin");
        approved = correctionRepository.saveAndFlush(approved);

        // Vaccinator applies with doseNumber = 2
        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/apply", recordA.getId(), approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fieldName\":\"doseNumber\",\"newValue\":\"2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correction.status", is("APPLIED")))
                .andExpect(jsonPath("$.certificateImpacted", is(true)))
                .andExpect(jsonPath("$.certificateReissued", is(true)))
                .andExpect(jsonPath("$.certificateReissue.supersededCertificate.status", is("SUPERSEDED")))
                .andExpect(jsonPath("$.certificateReissue.issuedCertificate.status", is("VALID")));

        // Check DB state:
        // 1. Correction is APPLIED
        VaccinationCorrection applied = correctionRepository.findById(approved.getId()).orElseThrow();
        assertEquals(VaccinationCorrectionStatus.APPLIED, applied.getStatus());

        // 2. Old certificate is SUPERSEDED
        DigitalCertificate oldCert = certificateRepository.findById(certA.getId()).orElseThrow();
        assertEquals(CertificateStatus.SUPERSEDED, oldCert.getStatus());

        // 3. New certificate is VALID and reference differs
        List<DigitalCertificate> allCerts = certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(recordA.getId());
        assertEquals(2, allCerts.size());
        DigitalCertificate newCert = allCerts.get(1);
        assertEquals(CertificateStatus.VALID, newCert.getStatus());
        assertNotEquals(oldCert.getCertificateReference(), newCert.getCertificateReference());
        assertEquals(oldCert.getId(), newCert.getReissuedFromCertificateId());
        assertEquals(newCert.getId(), oldCert.getSupersededByCertificateId());
    }

    @Test
    @DisplayName("22. Duplicate apply is prevented")
    void duplicateApplyIsPrevented() throws Exception {
        VaccinationCorrection approved = new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "First dose administered",
                "Updated note",
                "Note correction",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        );
        approved.approve(ADMIN_ID, Instant.now(), "Approved");
        approved = correctionRepository.saveAndFlush(approved);

        // First apply succeeds
        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/apply", recordA.getId(), approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR")))
                .andExpect(status().isOk());

        // Second apply fails
        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/apply", recordA.getId(), approved.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Only APPROVED")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    @DisplayName("17. Vaccinator cannot approve or reject correction requests")
    void vaccinatorCannotApproveOrReject(String action) throws Exception {
        VaccinationCorrection pending = correctionRepository.saveAndFlush(new VaccinationCorrection(
                recordA.getId(),
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Note",
                null,
                "Reason",
                CITIZEN_A_ID,
                Instant.now(),
                VaccinationCorrectionStatus.PENDING
        ));

        mockMvc.perform(post(BASE + "/records/{recordId}/corrections/{correctionId}/" + action, recordA.getId(), pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(VACCINATOR_A_ID, "ROLE_VACCINATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Attempting to review\"}"))
                .andExpect(status().isForbidden());
    }

    // --- PUBLIC & OTHER ROLES FORBIDDEN ---

    @ParameterizedTest
    @ValueSource(strings = {
            "ROLE_CENTER_MANAGER",
            "ROLE_CAMPAIGN_OFFICER",
            "ROLE_INVENTORY_OFFICER",
            "ROLE_HEALTHCARE_OFFICER",
            "ROLE_SUPER_ADMIN"
    })
    @DisplayName("23. Other roles are forbidden from correction endpoints")
    void otherRolesAreForbiddenFromCorrections(String role) throws Exception {
        mockMvc.perform(get(BASE + "/corrections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(999L, role)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/corrections/administered")
                        .header(HttpHeaders.AUTHORIZATION, bearer(999L, role)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE + "/corrections/mine")
                        .header(HttpHeaders.AUTHORIZATION, bearer(999L, role)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("24. Anonymous requests are rejected from correction endpoints")
    void anonymousCannotAccessCorrections() throws Exception {
        mockMvc.perform(get(BASE + "/corrections"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE + "/records/101/corrections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"anonymous request\"}"))
                .andExpect(status().isForbidden());
    }
}
