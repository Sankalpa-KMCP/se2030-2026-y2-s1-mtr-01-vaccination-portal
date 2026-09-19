package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class VaccinationCorrectionLifecycleRollbackIntegrationTests {

    @Autowired
    private VaccinationCorrectionLifecycleService lifecycleService;

    @Autowired
    private VaccinationCorrectionService correctionService;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private VaccinationCorrectionRepository correctionRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @MockitoBean
    private DigitalCertificateService certificateService;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        certificateRepository.deleteAll();
        correctionRepository.deleteAll();
        recordRepository.deleteAll();
    }

    @Test
    void certificateReissueFailureRollsBackAppendedCorrection() {
        VaccinationRecord record = recordRepository.saveAndFlush(new VaccinationRecord(
                8201L, 9201L, 2001L, 3001L, 4001L, 5001L, 1,
                Instant.parse("2026-09-01T08:00:00Z"), null, null,
                Instant.parse("2026-09-01T08:05:00Z")
        ));
        DigitalCertificate original = certificateRepository.saveAndFlush(new DigitalCertificate(
                record.getId(),
                "CERT-ROLLBACK",
                "rollback-verification-token",
                Instant.parse("2026-09-01T09:00:00Z"),
                null,
                5001L,
                1,
                Instant.parse("2026-09-01T08:00:00Z")
        ));
        when(certificateService.hasCurrentValidCertificate(record.getId())).thenReturn(true);
        when(certificateService.reissueCertificate(eq(record.getId()), any()))
                .thenThrow(new IllegalStateException("Simulated certificate reissue failure"));

        VaccinationCorrection pending = correctionService.createCorrection(
                record.getId(),
                new CreateVaccinationCorrectionRequest(
                        "doseNumber", "2", "Correction requiring reissue", 91L
                ),
                91L
        );
        VaccinationCorrection approved = correctionService.approveCorrection(
                record.getId(),
                pending.getId(),
                92L,
                "Approved for application"
        );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycleService.applyApprovedCorrection(
                        record.getId(),
                        approved.getId(),
                        92L
                )
        );

        assertEquals(1, correctionRepository.count());
        VaccinationCorrection persistedCorrection = correctionRepository.findById(approved.getId()).orElseThrow();
        assertEquals(VaccinationCorrectionStatus.APPROVED, persistedCorrection.getStatus());
        assertEquals(1, certificateRepository.count());
        DigitalCertificate persisted = certificateRepository.findById(original.getId()).orElseThrow();
        assertEquals(CertificateStatus.VALID, persisted.getStatus());
        assertEquals(1, persisted.getIssuedDoseNumber());
    }
}
