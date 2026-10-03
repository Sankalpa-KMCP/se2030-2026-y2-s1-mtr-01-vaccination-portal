package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
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
class VaccinationCorrectionIntegrationTests {

    @Autowired
    private VaccinationCorrectionService correctionService;

    @Autowired
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Autowired
    private VaccinationCorrectionRepository correctionRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void multipleCorrectionsPersistInChronologicalOrderWithoutMutatingRecordOrCertificate() {
        VaccinationRecord record = vaccinationRecordRepository.saveAndFlush(record());
        DigitalCertificate certificate = certificateRepository.saveAndFlush(new DigitalCertificate(
                record.getId(),
                "CERT-CORRECTION-INDEPENDENCE",
                "correction-independence-token",
                Instant.parse("2026-09-01T09:00:00Z")
        ));
        long recordCount = vaccinationRecordRepository.count();
        long certificateCount = certificateRepository.count();

        VaccinationCorrection first = correctionService.createCorrection(
                record.getId(),
                request("doseNumber", "2", "Dose number transcribed incorrectly")
        );
        VaccinationCorrection second = correctionService.createCorrection(
                record.getId(),
                request("authorizedNotes", "Corrected clinical note", "Clarify authorized note")
        );
        entityManager.flush();
        entityManager.clear();

        List<VaccinationCorrection> history = correctionService.getCorrectionHistory(record.getId());
        VaccinationRecord unchangedRecord = vaccinationRecordRepository.findById(record.getId()).orElseThrow();
        DigitalCertificate unchangedCertificate = certificateRepository.findById(certificate.getId()).orElseThrow();

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertEquals(2, correctionRepository.count());
        assertEquals(List.of(first.getId(), second.getId()), history.stream().map(VaccinationCorrection::getId).toList());
        assertEquals(VaccinationCorrectionField.DOSE_NUMBER, history.get(0).getFieldName());
        assertEquals("1", history.get(0).getOldValue());
        assertEquals("2", history.get(0).getNewValue());
        assertEquals(VaccinationCorrectionField.AUTHORIZED_NOTES, history.get(1).getFieldName());
        assertEquals("Original note", history.get(1).getOldValue());
        assertEquals("Corrected clinical note", history.get(1).getNewValue());
        assertNotNull(history.get(0).getCorrectedAt());
        assertEquals(91L, history.get(0).getCorrectedBy());
        assertEquals(VaccinationCorrectionStatus.PENDING, history.get(0).getStatus());

        assertEquals(recordCount, vaccinationRecordRepository.count());
        assertEquals(1, unchangedRecord.getDoseNumber());
        assertEquals(56L, unchangedRecord.getVaccineBatchId());
        assertEquals("Original note", unchangedRecord.getAuthorizedNotes());

        assertEquals(certificateCount, certificateRepository.count());
        assertEquals(CertificateStatus.VALID, unchangedCertificate.getStatus());
        assertEquals("CERT-CORRECTION-INDEPENDENCE", unchangedCertificate.getCertificateReference());
        assertEquals("correction-independence-token", unchangedCertificate.getVerificationToken());
    }

    @Test
    void sameFieldCorrectionsChainFromEffectiveValueAndPermitReturningToOriginal() {
        VaccinationRecord record = vaccinationRecordRepository.saveAndFlush(record());

        VaccinationCorrection doseCorrection = createAndApplyCorrection(
                record.getId(),
                request("doseNumber", "2", "Independent dose correction")
        );
        VaccinationCorrection first = createAndApplyCorrection(
                record.getId(),
                request("nextDoseDate", "2026-10-15", "First date correction")
        );
        VaccinationCorrection second = createAndApplyCorrection(
                record.getId(),
                request("nextDoseDate", "2026-10-30", "Second date correction")
        );

        assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> correctionService.createCorrection(
                        record.getId(),
                        request("nextDoseDate", "2026-10-30", "No effective change")
                )
        );

        VaccinationCorrection restored = createAndApplyCorrection(
                record.getId(),
                request("nextDoseDate", "2026-10-01", "Restore original date")
        );
        VaccinationCorrection noteOne = createAndApplyCorrection(
                record.getId(),
                request("authorizedNotes", "First corrected note", "First note correction")
        );
        VaccinationCorrection noteTwo = createAndApplyCorrection(
                record.getId(),
                request("authorizedNotes", null, "Clear corrected note")
        );

        entityManager.flush();
        entityManager.clear();

        List<VaccinationCorrection> history = correctionService.getCorrectionHistory(record.getId());
        VaccinationRecord unchangedRecord = vaccinationRecordRepository.findById(record.getId()).orElseThrow();

        assertEquals("2026-10-01", first.getOldValue());
        assertEquals("2026-10-15", first.getNewValue());
        assertEquals("1", doseCorrection.getOldValue());
        assertEquals("2", doseCorrection.getNewValue());
        assertEquals("2026-10-15", second.getOldValue());
        assertEquals("2026-10-30", second.getNewValue());
        assertEquals("2026-10-30", restored.getOldValue());
        assertEquals("2026-10-01", restored.getNewValue());
        assertEquals("Original note", noteOne.getOldValue());
        assertEquals("First corrected note", noteOne.getNewValue());
        assertEquals("First corrected note", noteTwo.getOldValue());
        assertNull(noteTwo.getNewValue());
        assertEquals(6, history.size());
        assertEquals(56L, unchangedRecord.getVaccineBatchId());
        assertEquals("Original note", unchangedRecord.getAuthorizedNotes());
    }

    @Test
    void vaccineBatchCorrectionIsRejected() {
        VaccinationRecord record = vaccinationRecordRepository.saveAndFlush(record());
        UnsupportedVaccinationCorrectionFieldException ex = assertThrows(
                UnsupportedVaccinationCorrectionFieldException.class,
                () -> correctionService.createCorrection(
                        record.getId(),
                        request("vaccineBatchId", "78", "Batch change")
                )
        );
        assertEquals(
                "Vaccine batch corrections are not supported through the vaccination correction workflow because batch changes require inventory reconciliation.",
                ex.getMessage()
        );
    }

    private CreateVaccinationCorrectionRequest request(String fieldName, String newValue, String reason) {
        return new CreateVaccinationCorrectionRequest(fieldName, newValue, reason, 91L);
    }

    private VaccinationCorrection createAndApplyCorrection(Long recordId, CreateVaccinationCorrectionRequest request) {
        VaccinationCorrection c = correctionService.createCorrection(recordId, request);
        c.approve(9999L, Instant.now(), "Approved for integration test");
        c.apply();
        return correctionRepository.saveAndFlush(c);
    }

    private VaccinationRecord record() {
        return new VaccinationRecord(
                1001L,
                2001L,
                3001L,
                4001L,
                91L,
                56L,
                1,
                Instant.parse("2026-09-01T08:00:00Z"),
                LocalDate.parse("2026-10-01"),
                "Original note",
                Instant.parse("2026-09-01T08:05:00Z")
        );
    }
}
