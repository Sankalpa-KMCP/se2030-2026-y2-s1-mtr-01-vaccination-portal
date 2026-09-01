package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DigitalCertificateLifecycleIntegrationTests {

    private static final Instant ADMINISTERED_AT = Instant.parse("2026-09-01T08:00:00Z");
    private static final Instant ISSUED_AT = Instant.parse("2026-09-02T08:30:00Z");

    @Autowired
    private DigitalCertificateService certificateService;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @Autowired
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void issuingForExistingRecordPersistsCertificateWithoutCreatingAnotherRecord() {
        VaccinationRecord record = persistRecord(1001L);
        long recordCountBefore = vaccinationRecordRepository.count();

        DigitalCertificate issued = certificateService.issueCertificateForVaccinationRecord(record.getId());

        assertNotNull(issued.getId());
        assertEquals(record.getId(), issued.getVaccinationRecordId());
        assertEquals(CertificateStatus.VALID, issued.getStatus());
        assertFalse(issued.getCertificateReference().isBlank());
        assertFalse(issued.getVerificationToken().isBlank());
        assertEquals(recordCountBefore, vaccinationRecordRepository.count());

        DigitalCertificate persisted = certificateRepository.findById(issued.getId()).orElseThrow();
        assertEquals(issued.getCertificateReference(), persisted.getCertificateReference());
        assertEquals(issued.getVerificationToken(), persisted.getVerificationToken());
    }

    @Test
    void duplicateServiceIssuanceForSameRecordIsRejected() {
        VaccinationRecord record = persistRecord(1002L);
        certificateService.issueCertificateForVaccinationRecord(record.getId());

        assertThrows(
                DuplicateDigitalCertificateException.class,
                () -> certificateService.issueCertificateForVaccinationRecord(record.getId())
        );
        assertEquals(1, certificateRepository.count());
    }

    @Test
    void databaseConstraintPreventsTwoCertificatesForSameVaccinationRecord() {
        VaccinationRecord record = persistRecord(1003L);
        certificateRepository.saveAndFlush(certificate(record.getId(), "CERT-RECORD-1", "token-record-1"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> certificateRepository.saveAndFlush(
                        certificate(record.getId(), "CERT-RECORD-2", "token-record-2")
                )
        );
    }

    @Test
    void databaseConstraintKeepsCertificateReferenceUnique() {
        VaccinationRecord firstRecord = persistRecord(1004L);
        VaccinationRecord secondRecord = persistRecord(1005L);
        certificateRepository.saveAndFlush(
                certificate(firstRecord.getId(), "CERT-UNIQUE-REFERENCE", "token-reference-1")
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> certificateRepository.saveAndFlush(
                        certificate(secondRecord.getId(), "CERT-UNIQUE-REFERENCE", "token-reference-2")
                )
        );
    }

    @Test
    void databaseConstraintKeepsVerificationTokenUnique() {
        VaccinationRecord firstRecord = persistRecord(1006L);
        VaccinationRecord secondRecord = persistRecord(1007L);
        certificateRepository.saveAndFlush(
                certificate(firstRecord.getId(), "CERT-TOKEN-1", "shared-verification-token")
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> certificateRepository.saveAndFlush(
                        certificate(secondRecord.getId(), "CERT-TOKEN-2", "shared-verification-token")
                )
        );
    }

    @Test
    void repeatedValidVerificationIsConsistentAndDoesNotMutateCertificate() {
        VaccinationRecord record = persistRecord(1008L);
        DigitalCertificate issued = certificateService.issueCertificateForVaccinationRecord(record.getId());
        Long originalLockVersion = issued.getLockVersion();

        CertificateVerificationResponse first =
                certificateService.verifyCertificate(issued.getVerificationToken());
        CertificateVerificationResponse second =
                certificateService.verifyCertificate(issued.getVerificationToken());

        assertTrue(first.valid());
        assertEquals(CertificateStatus.VALID, first.status());
        assertEquals(first, second);

        entityManager.flush();
        entityManager.clear();
        DigitalCertificate reloaded = certificateRepository.findById(issued.getId()).orElseThrow();
        assertEquals(CertificateStatus.VALID, reloaded.getStatus());
        assertEquals(originalLockVersion, reloaded.getLockVersion());
        assertEquals(issued.getVerificationToken(), reloaded.getVerificationToken());
    }

    @Test
    void persistedRevokedCertificateVerifiesInvalidWithoutMutation() {
        VaccinationRecord record = persistRecord(1009L);
        DigitalCertificate issued = certificateService.issueCertificateForVaccinationRecord(record.getId());
        jdbcTemplate.update(
                "UPDATE digital_certificates SET status = ? WHERE id = ?",
                CertificateStatus.REVOKED.name(),
                issued.getId()
        );
        entityManager.clear();

        CertificateVerificationResponse first =
                certificateService.verifyCertificate(issued.getVerificationToken());
        CertificateVerificationResponse second =
                certificateService.verifyCertificate(issued.getVerificationToken());

        assertFalse(first.valid());
        assertEquals(CertificateStatus.REVOKED, first.status());
        assertEquals(first, second);

        entityManager.clear();
        DigitalCertificate reloaded = certificateRepository.findById(issued.getId()).orElseThrow();
        assertEquals(CertificateStatus.REVOKED, reloaded.getStatus());
    }

    @Test
    void retrievalByIdAndReferenceReturnsTheSamePersistedCertificate() {
        VaccinationRecord record = persistRecord(1010L);
        DigitalCertificate issued = certificateService.issueCertificateForVaccinationRecord(record.getId());

        DigitalCertificate byId = certificateService.getCertificateById(issued.getId());
        DigitalCertificate byReference =
                certificateService.getCertificateByReference(issued.getCertificateReference());

        assertSame(byId, byReference);
        assertEquals(issued.getId(), byId.getId());
    }

    @Test
    void unknownCertificateIdAndReferenceFailWithModuleException() {
        assertThrows(
                DigitalCertificateNotFoundException.class,
                () -> certificateService.getCertificateById(Long.MAX_VALUE)
        );
        assertThrows(
                DigitalCertificateNotFoundException.class,
                () -> certificateService.getCertificateByReference("CERT-DOES-NOT-EXIST")
        );
    }

    @Test
    void serializedResponsesDoNotExposeVerificationTokenOrPersistenceInternals() throws Exception {
        VaccinationRecord record = persistRecord(1011L);
        DigitalCertificate issued = certificateService.issueCertificateForVaccinationRecord(record.getId());
        JsonMapper jsonMapper = new JsonMapper();

        DigitalCertificateResponse normalResponse = DigitalCertificateMapper.toResponse(issued);
        CertificateVerificationResponse verificationResponse =
                certificateService.verifyCertificate(issued.getVerificationToken());
        String normalJson = jsonMapper.writeValueAsString(normalResponse);
        String verificationJson = jsonMapper.writeValueAsString(verificationResponse);

        assertFalse(normalJson.contains("verificationToken"));
        assertFalse(normalJson.contains("lockVersion"));
        assertFalse(normalJson.contains(issued.getVerificationToken()));
        assertFalse(verificationJson.contains("verificationToken"));
        assertFalse(verificationJson.contains("lockVersion"));
        assertFalse(verificationJson.contains(issued.getVerificationToken()));
    }

    private VaccinationRecord persistRecord(Long appointmentId) {
        return vaccinationRecordRepository.saveAndFlush(new VaccinationRecord(
                appointmentId,
                appointmentId + 1000,
                2001L,
                3001L,
                4001L,
                5001L,
                1,
                ADMINISTERED_AT,
                null,
                null,
                ISSUED_AT
        ));
    }

    private DigitalCertificate certificate(
            Long vaccinationRecordId,
            String certificateReference,
            String verificationToken
    ) {
        return new DigitalCertificate(
                vaccinationRecordId,
                certificateReference,
                verificationToken,
                ISSUED_AT
        );
    }
}
