package com.se2030.vaccinationportal.vaccination.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationPersistenceIntegrationTests {

    @Autowired
    private VaccinationDraftRepository draftRepository;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @Test
    @DisplayName("Draft can be found and checked by appointment ID")
    void draftLookupUsesAppointmentId() {
        VaccinationDraft saved = draftRepository.saveAndFlush(new VaccinationDraft(8101L));

        assertNotNull(saved.getId());
        assertTrue(draftRepository.existsByAppointmentId(8101L));
        assertEquals(saved.getId(), draftRepository.findByAppointmentId(8101L).orElseThrow().getId());
    }

    @Test
    @DisplayName("Finalized records support appointment idempotency and citizen history")
    void recordLookupsSupportFinalizationAndHistory() {
        Instant firstAdministration = Instant.parse("2026-08-01T09:00:00Z");
        Instant secondAdministration = Instant.parse("2026-08-20T09:00:00Z");
        recordRepository.saveAndFlush(record(8201L, 9201L, firstAdministration));
        VaccinationRecord latest = recordRepository.saveAndFlush(record(8202L, 9201L, secondAdministration));

        assertTrue(recordRepository.existsByAppointmentId(8201L));
        assertEquals(8201L, recordRepository.findByAppointmentId(8201L).orElseThrow().getAppointmentId());

        List<VaccinationRecord> history =
                recordRepository.findByCitizenIdOrderByAdministrationTimeDesc(9201L);
        assertEquals(2, history.size());
        assertEquals(latest.getId(), history.get(0).getId());
    }

    @Test
    @DisplayName("Certificate can be found by public reference and verification token")
    void certificateLookupUsesStableIdentifiers() {
        DigitalCertificate saved = certificateRepository.saveAndFlush(
                new DigitalCertificate(8301L, "CERT-8301", "verify-8301", Instant.now())
        );

        assertEquals(
                saved.getId(),
                certificateRepository.findByCertificateReference("CERT-8301").orElseThrow().getId()
        );
        assertEquals(
                saved.getId(),
                certificateRepository.findByVerificationToken("verify-8301").orElseThrow().getId()
        );
    }

    private VaccinationRecord record(Long appointmentId, Long citizenId, Instant administrationTime) {
        return new VaccinationRecord(
                appointmentId,
                citizenId,
                9301L,
                9401L,
                9501L,
                9601L,
                1,
                administrationTime,
                null,
                null,
                Instant.now()
        );
    }
}
