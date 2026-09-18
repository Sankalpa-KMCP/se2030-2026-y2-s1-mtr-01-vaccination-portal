package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationCorrectionLifecycleFlowIntegrationTests {

    private static final Instant ADMINISTERED_AT = Instant.parse("2026-09-01T08:00:00Z");

    @Autowired
    private VaccinationCorrectionService correctionService;

    @Autowired
    private VaccinationCorrectionLifecycleService lifecycleService;

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

    @Test
    void fullCanonicalCorrectionLifecycleSucceedsWithSeparationOfDuties() {
        VaccinationRecord record = persistRecord(9101L);
        DigitalCertificate originalCert = certificateService.issueCertificateForVaccinationRecord(record.getId());
        assertEquals(CertificateStatus.VALID, originalCert.getStatus());

        Long submitterId = 101L;
        Long reviewerId = 202L;

        // 1. Submit correction -> PENDING
        VaccinationCorrection pending = correctionService.createCorrection(
                record.getId(),
                new CreateVaccinationCorrectionRequest("doseNumber", "2", "Administer second dose correction", submitterId)
        );
        assertNotNull(pending.getId());
        assertEquals(VaccinationCorrectionStatus.PENDING, pending.getStatus());
        assertEquals(submitterId, pending.getCorrectedBy());

        // 2. Submitter cannot approve their own correction (Separation of duties)
        assertThrows(InvalidVaccinationCorrectionException.class, () ->
                correctionService.approveCorrection(record.getId(), pending.getId(), submitterId, "Self approval"));

        // Cannot apply while still PENDING
        assertThrows(InvalidVaccinationCorrectionException.class, () ->
                lifecycleService.applyApprovedCorrection(record.getId(), pending.getId(), reviewerId));

        // 3. Independent reviewer approves -> APPROVED
        VaccinationCorrection approved = correctionService.approveCorrection(
                record.getId(), pending.getId(), reviewerId, "Clinical supervisor verification confirmed"
        );
        assertEquals(VaccinationCorrectionStatus.APPROVED, approved.getStatus());
        assertEquals(reviewerId, approved.getReviewedBy());
        assertNotNull(approved.getReviewedAt());
        assertEquals("Clinical supervisor verification confirmed", approved.getReviewReason());

        // 4. Apply approved correction -> APPLIED + certificate reissued
        VaccinationCorrectionLifecycleResponse appliedResponse = lifecycleService.applyApprovedCorrection(
                record.getId(), pending.getId(), reviewerId
        );

        entityManager.flush();
        entityManager.clear();

        assertTrue(appliedResponse.certificateImpacted());
        assertTrue(appliedResponse.certificateReissued());
        assertNotNull(appliedResponse.certificateReissue());

        VaccinationCorrection persistedCorrection = correctionRepository.findById(pending.getId()).orElseThrow();
        assertEquals(VaccinationCorrectionStatus.APPLIED, persistedCorrection.getStatus());

        // Verify certificate lifecycle
        List<DigitalCertificate> certificates = certificateRepository
                .findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId());
        assertEquals(2, certificates.size());
        assertEquals(CertificateStatus.SUPERSEDED, certificates.get(0).getStatus());
        assertEquals(CertificateStatus.VALID, certificates.get(1).getStatus());
        assertEquals(2, certificates.get(1).getIssuedDoseNumber());

        // Verify cannot re-apply an already APPLIED correction
        assertThrows(InvalidVaccinationCorrectionException.class, () ->
                lifecycleService.applyApprovedCorrection(record.getId(), pending.getId(), reviewerId));
    }

    @Test
    void rejectionFlowPreventsApplication() {
        VaccinationRecord record = persistRecord(9102L);
        Long submitterId = 101L;
        Long reviewerId = 202L;

        VaccinationCorrection correction = correctionService.createCorrection(
                record.getId(),
                new CreateVaccinationCorrectionRequest("authorizedNotes", "Incorrect batch used", "Typo note", submitterId)
        );

        // Rejection requires a non-blank reason
        assertThrows(InvalidVaccinationCorrectionException.class, () ->
                correctionService.rejectCorrection(record.getId(), correction.getId(), reviewerId, "   "));

        // Reject correction -> REJECTED
        VaccinationCorrection rejected = correctionService.rejectCorrection(
                record.getId(), correction.getId(), reviewerId, "Insufficient evidence attached"
        );
        assertEquals(VaccinationCorrectionStatus.REJECTED, rejected.getStatus());
        assertEquals(reviewerId, rejected.getReviewedBy());
        assertEquals("Insufficient evidence attached", rejected.getReviewReason());

        // Cannot approve or apply a rejected correction
        assertThrows(InvalidVaccinationCorrectionException.class, () ->
                correctionService.approveCorrection(record.getId(), correction.getId(), reviewerId, "Try approve"));
        assertThrows(InvalidVaccinationCorrectionException.class, () ->
                lifecycleService.applyApprovedCorrection(record.getId(), correction.getId(), reviewerId));
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
