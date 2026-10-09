package com.se2030.vaccinationportal.vaccination.administration.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

class CitizenCertificateIssueStrategyTests {

    private final CitizenCertificateIssueStrategy strategy = new CitizenCertificateIssueStrategy();

    @Test
    void supportsOnlyCitizenCertificateIssueRequests() {
        assertTrue(strategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE));
        assertFalse(strategy.supports(VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT));
    }

    @Test
    void preparesNonClinicalIssueWithSafePlaceholderAndCategory() {
        CreateVaccinationCorrectionRequest prepared = strategy.prepareCreate(
                record(),
                new CreateVaccinationCorrectionRequest(
                        null, null, "Name is misspelled", null, "Certificate Discrepancy", null
                ),
                55L
        );

        assertEquals("authorizedNotes", prepared.fieldName());
        assertEquals("[Certificate Discrepancy] Name is misspelled", prepared.reason());
        assertEquals(55L, prepared.correctedBy());
        assertEquals(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE.name(), prepared.requestType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"doseNumber", "administrationTime", "nextDoseDate", "vaccineBatchId"})
    void rejectsCitizenProposalsForClinicalOrInventoryFields(String fieldName) {
        assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> strategy.prepareCreate(
                        record(),
                        new CreateVaccinationCorrectionRequest(fieldName, "changed", "Please change it"),
                        55L
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"appointmentId", "centerId", "citizenId"})
    void rejectsOwnershipAndOtherUnsupportedFields(String fieldName) {
        assertThrows(
                UnsupportedVaccinationCorrectionFieldException.class,
                () -> strategy.prepareCreate(
                        record(),
                        new CreateVaccinationCorrectionRequest(fieldName, "999", "Please change it"),
                        55L
                )
        );
    }

    @Test
    void rejectsRequestForAnotherCitizensRecord() {
        assertThrows(
                AccessDeniedException.class,
                () -> strategy.prepareCreate(
                        record(),
                        new CreateVaccinationCorrectionRequest("Certificate issue"),
                        99L
                )
        );
    }

    private VaccinationRecord record() {
        return new VaccinationRecord(
                1001L, 55L, 3001L, 4001L, 91L, 56L, 1,
                Instant.parse("2026-09-01T08:00:00Z"), null, "Original note",
                Instant.parse("2026-09-01T08:05:00Z")
        );
    }
}
