package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.administration.dto.ApplyVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.certificate.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.certificate.dto.RevokeDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.certificate.repository.DigitalCertificateRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationCorrectionLifecycleIntegrationTests {

    private static final Instant ADMINISTERED_AT = Instant.parse("2026-09-01T08:00:00Z");

    @Autowired
    private VaccinationCorrectionLifecycleService lifecycleService;

    @Autowired
    private VaccinationCorrectionService correctionService;

    @Autowired
    private DigitalCertificateService certificateService;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private VaccinationCorrectionRepository correctionRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @Autowired
    private EntityManager entityManager;

    private VaccinationCorrectionLifecycleResponse applyThroughCanonicalLifecycle(
            Long recordId,
            CreateVaccinationCorrectionRequest request
    ) {
        VaccinationCorrection pending = correctionService.createCorrection(recordId, request, request.correctedBy());
        Long reviewerId = (request.correctedBy() != null ? request.correctedBy() : 91L) + 100L;
        VaccinationCorrection approved = correctionService.approveCorrection(
                recordId,
                pending.getId(),
                reviewerId,
                "Approved in canonical test workflow"
        );
        return lifecycleService.applyApprovedCorrection(recordId, approved.getId(), reviewerId);
    }

    @Test
    void notesAndNextDoseDateCorrectionsCommitWithoutReissue() {
        VaccinationRecord record = persistRecord(8101L);
        DigitalCertificate original = certificateService.issueCertificateForVaccinationRecord(record.getId());

        VaccinationCorrectionLifecycleResponse notes = applyThroughCanonicalLifecycle(
                record.getId(),
                request("authorizedNotes", "Updated clinical note", "Clarify note")
        );
        VaccinationCorrectionLifecycleResponse nextDose = applyThroughCanonicalLifecycle(
                record.getId(),
                request("nextDoseDate", "2026-11-01", "Correct next dose date")
        );

        entityManager.flush();
        entityManager.clear();
        DigitalCertificate persisted = certificateRepository.findById(original.getId()).orElseThrow();
        assertFalse(notes.certificateImpacted());
        assertFalse(notes.certificateReissued());
        assertNull(notes.certificateReissue());
        assertFalse(nextDose.certificateImpacted());
        assertFalse(nextDose.certificateReissued());
        assertEquals(2, correctionRepository.count());
        assertEquals(1, certificateRepository.count());
        assertEquals(CertificateStatus.VALID, persisted.getStatus());
    }

    @Test
    void allCertificateVisibleFieldsTriggerReissueAndCaptureEffectiveValues() {
        VaccinationRecord record = persistRecord(8102L);
        DigitalCertificate first = certificateService.issueCertificateForVaccinationRecord(record.getId());

        VaccinationCorrectionLifecycleResponse dose = applyThroughCanonicalLifecycle(
                record.getId(), request("doseNumber", "2", "Correct dose")
        );
        VaccinationCorrectionLifecycleResponse administration = applyThroughCanonicalLifecycle(
                record.getId(),
                request("administrationTime", "2026-09-01T09:30:00Z", "Correct administration time")
        );

        entityManager.flush();
        entityManager.clear();
        List<DigitalCertificate> history = certificateRepository
                .findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId());
        assertTrue(dose.certificateReissued());
        assertTrue(administration.certificateReissued());
        assertEquals(3, history.size());
        assertEquals(List.of(
                CertificateStatus.SUPERSEDED,
                CertificateStatus.SUPERSEDED,
                CertificateStatus.VALID
        ), history.stream().map(DigitalCertificate::getStatus).toList());
        assertEquals(first.getId(), history.get(0).getId());
        assertEquals(1, history.get(0).getIssuedDoseNumber());
        assertEquals(ADMINISTERED_AT, history.get(0).getIssuedAdministrationTime());
        assertEquals(5001L, history.get(0).getIssuedVaccineBatchId());
        assertEquals(2, history.get(1).getIssuedDoseNumber());
        assertEquals(ADMINISTERED_AT, history.get(1).getIssuedAdministrationTime());
        assertEquals(2, history.get(2).getIssuedDoseNumber());
        assertEquals(Instant.parse("2026-09-01T09:30:00Z"), history.get(2).getIssuedAdministrationTime());

        VaccinationRecord unchanged = recordRepository.findById(record.getId()).orElseThrow();
        assertEquals(1, unchanged.getDoseNumber());
        assertEquals(ADMINISTERED_AT, unchanged.getAdministrationTime());
        assertEquals(5001L, unchanged.getVaccineBatchId());
    }

    @Test
    void vaccineBatchCorrectionCannotBeAppliedAndDoesNotReissueCertificateOrAlterRecord() {
        VaccinationRecord record = persistRecord(8102L);
        DigitalCertificate cert = certificateService.issueCertificateForVaccinationRecord(record.getId());

        // 1. Citizen creates a request mentioning batch
        VaccinationCorrection pending = correctionService.createCorrection(
                record.getId(), new CreateVaccinationCorrectionRequest(null, null, "Citizen requested batch change: batch was 6001", 91L)
        );
        // 2. Admin approves request
        correctionService.approveCorrection(record.getId(), pending.getId(), 999L, "Approved for review");

        // 3. Vaccinator attempts to apply a batch correction - must be rejected
        UnsupportedVaccinationCorrectionFieldException ex = assertThrows(
                UnsupportedVaccinationCorrectionFieldException.class,
                () -> lifecycleService.applyApprovedCorrection(
                        record.getId(),
                        pending.getId(),
                        999L,
                        new ApplyVaccinationCorrectionRequest("vaccineBatchId", "6001")
                )
        );

        assertEquals(
                "Vaccine batch corrections are not supported through the vaccination correction workflow because batch changes require inventory reconciliation.",
                ex.getMessage()
        );

        // 4. Verify record remains untouched
        VaccinationRecord currentRecord = recordRepository.findById(record.getId()).orElseThrow();
        assertEquals(5001L, currentRecord.getVaccineBatchId());

        // 5. Verify certificate remains VALID with no reissue
        DigitalCertificate currentCert = certificateRepository.findById(cert.getId()).orElseThrow();
        assertEquals(CertificateStatus.VALID, currentCert.getStatus());
        assertEquals(1, certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId()).size());
    }

    @Test
    void impactingCorrectionWithoutCurrentValidCertificateDoesNotIssueCertificate() {
        VaccinationRecord neverIssued = persistRecord(8103L);
        VaccinationCorrectionLifecycleResponse noCertificate = applyThroughCanonicalLifecycle(
                neverIssued.getId(), request("doseNumber", "2", "Correct dose")
        );

        VaccinationRecord revokedRecord = persistRecord(8104L);
        DigitalCertificate superseded = certificateService.issueCertificateForVaccinationRecord(revokedRecord.getId());
        var explicitReissue = certificateService.reissueCertificate(
                revokedRecord.getId(),
                new ReissueDigitalCertificateRequest("Create historical replacement", 91L)
        );
        certificateService.revokeCertificate(
                explicitReissue.issuedCertificate().id(),
                new RevokeDigitalCertificateRequest("Administrative revocation", 91L)
        );
        VaccinationCorrectionLifecycleResponse historicalOnly = applyThroughCanonicalLifecycle(
                revokedRecord.getId(), request("doseNumber", "2", "Correct dose after revocation")
        );

        assertTrue(noCertificate.certificateImpacted());
        assertFalse(noCertificate.certificateReissued());
        assertNull(noCertificate.certificateReissue());
        assertTrue(historicalOnly.certificateImpacted());
        assertFalse(historicalOnly.certificateReissued());
        assertEquals(2, certificateRepository.count());
        assertEquals(CertificateStatus.SUPERSEDED,
                certificateRepository.findById(superseded.getId()).orElseThrow().getStatus());
        assertEquals(CertificateStatus.REVOKED,
                certificateRepository.findById(explicitReissue.issuedCertificate().id()).orElseThrow().getStatus());
    }

    @Test
    void chainedDoseCorrectionsAndReturnToOriginalPreserveEveryCertificateSnapshot() {
        VaccinationRecord record = persistRecord(8105L);
        certificateService.issueCertificateForVaccinationRecord(record.getId());

        applyThroughCanonicalLifecycle(record.getId(), request("doseNumber", "2", "First correction"));
        applyThroughCanonicalLifecycle(record.getId(), request("doseNumber", "3", "Second correction"));
        VaccinationCorrectionLifecycleResponse restored = applyThroughCanonicalLifecycle(
                record.getId(), request("doseNumber", "1", "Restore original value")
        );

        entityManager.flush();
        entityManager.clear();
        List<DigitalCertificate> history = certificateRepository
                .findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId());
        assertTrue(restored.certificateReissued());
        assertEquals(List.of(1, 2, 3, 1),
                history.stream().map(DigitalCertificate::getIssuedDoseNumber).toList());
        assertEquals(List.of(
                CertificateStatus.SUPERSEDED,
                CertificateStatus.SUPERSEDED,
                CertificateStatus.SUPERSEDED,
                CertificateStatus.VALID
        ), history.stream().map(DigitalCertificate::getStatus).toList());
        assertEquals(1, recordRepository.findById(record.getId()).orElseThrow().getDoseNumber());
        assertEquals(3, correctionRepository.count());
    }

    @Test
    void workflowResponseDoesNotExposeTokensOrPersistenceInternals() throws Exception {
        VaccinationRecord record = persistRecord(8106L);
        DigitalCertificate original = certificateService.issueCertificateForVaccinationRecord(record.getId());

        VaccinationCorrectionLifecycleResponse response = applyThroughCanonicalLifecycle(
                record.getId(), request("doseNumber", "2", "Correct dose")
        );
        String json = new JsonMapper().writeValueAsString(response);

        assertFalse(json.contains("verificationToken"));
        assertFalse(json.contains("lockVersion"));
        assertFalse(json.contains(original.getVerificationToken()));
        assertFalse(json.contains("issuedVaccineBatchId"));
        assertTrue(json.contains("certificateReissued"));
    }

    private CreateVaccinationCorrectionRequest request(String field, String value, String reason) {
        return new CreateVaccinationCorrectionRequest(field, value, reason, 91L);
    }

    private VaccinationRecord persistRecord(Long appointmentId) {
        return recordRepository.saveAndFlush(new VaccinationRecord(
                appointmentId,
                appointmentId + 1000,
                2001L,
                3001L,
                4001L,
                5001L,
                1,
                ADMINISTERED_AT,
                LocalDate.parse("2026-10-01"),
                "Original note",
                Instant.parse("2026-09-01T08:05:00Z")
        ));
    }
}
