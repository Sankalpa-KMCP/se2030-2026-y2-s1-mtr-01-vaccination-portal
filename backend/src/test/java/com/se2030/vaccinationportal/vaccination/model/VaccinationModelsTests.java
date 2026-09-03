package com.se2030.vaccinationportal.vaccination.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VaccinationModelsTests {

    @Test
    @DisplayName("Vaccination draft starts editable and receives audit timestamps")
    void draftStartsInProgress() {
        VaccinationDraft draft = new VaccinationDraft(101L);

        draft.onCreate();

        assertEquals(101L, draft.getAppointmentId());
        assertEquals(VaccinationDraftStatus.IN_PROGRESS, draft.getStatus());
        assertFalse(draft.isPhysicalAdministrationConfirmed());
        assertNotNull(draft.getCreatedAt());
        assertNotNull(draft.getUpdatedAt());
    }

    @Test
    @DisplayName("Vaccination record captures immutable finalized history")
    void recordCapturesFinalizedHistory() {
        Instant administeredAt = Instant.parse("2026-08-26T10:15:30Z");
        LocalDate nextDoseDate = LocalDate.of(2026, 9, 23);
        VaccinationRecord record = new VaccinationRecord(
                101L, 201L, 301L, 401L, 501L, 601L, 2,
                administeredAt, nextDoseDate, "Observed for 15 minutes", null
        );

        record.onCreate();

        assertEquals(101L, record.getAppointmentId());
        assertEquals(201L, record.getCitizenId());
        assertEquals(601L, record.getVaccineBatchId());
        assertEquals(2, record.getDoseNumber());
        assertEquals(administeredAt, record.getAdministrationTime());
        assertEquals(nextDoseDate, record.getNextDoseDate());
        assertNotNull(record.getFinalizedAt());
    }

    @Test
    @DisplayName("Digital certificate starts valid and normalizes identifiers")
    void certificateStartsValid() {
        DigitalCertificate certificate = new DigitalCertificate(
                701L, " CERT-2026-0001 ", " token-0001 ", null
        );

        certificate.onCreate();

        assertEquals(701L, certificate.getVaccinationRecordId());
        assertEquals("CERT-2026-0001", certificate.getCertificateReference());
        assertEquals("token-0001", certificate.getVerificationToken());
        assertEquals(CertificateStatus.VALID, certificate.getStatus());
        assertNotNull(certificate.getIssuedAt());
    }

    @Test
    @DisplayName("Domain constructors reject missing required identifiers")
    void constructorsRejectMissingIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new VaccinationDraft(null));
        assertThrows(IllegalArgumentException.class, () ->
                new DigitalCertificate(null, "CERT-1", "token-1", Instant.now()));
    }
}
