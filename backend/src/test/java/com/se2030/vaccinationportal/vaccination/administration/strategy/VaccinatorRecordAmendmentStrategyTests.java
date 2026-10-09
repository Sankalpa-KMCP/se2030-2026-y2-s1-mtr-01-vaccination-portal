package com.se2030.vaccinationportal.vaccination.administration.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

class VaccinatorRecordAmendmentStrategyTests {

    private final VaccinatorRecordAmendmentStrategy strategy = new VaccinatorRecordAmendmentStrategy();

    @Test
    void supportsOnlyVaccinatorRecordAmendments() {
        assertTrue(strategy.supports(VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT));
        assertFalse(strategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doseNumber", "administrationTime", "nextDoseDate", "authorizedNotes"})
    void allowsOnlyTheApprovedSafeAmendmentFields(String fieldName) {
        CreateVaccinationCorrectionRequest prepared = strategy.prepareCreate(
                record(),
                new CreateVaccinationCorrectionRequest(fieldName, "changed", "Verified amendment"),
                91L
        );

        assertEquals(fieldName, prepared.fieldName());
        assertEquals(91L, prepared.correctedBy());
        assertEquals(VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT.name(), prepared.requestType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"vaccineBatchId", "appointmentId", "centerId", "citizenId"})
    void rejectsInventoryOwnershipAndOtherUnsupportedFields(String fieldName) {
        assertThrows(
                UnsupportedVaccinationCorrectionFieldException.class,
                () -> strategy.prepareCreate(
                        record(),
                        new CreateVaccinationCorrectionRequest(fieldName, "999", "Unsafe amendment"),
                        91L
                )
        );
    }

    @Test
    void rejectsVaccinatorWhoDidNotAdministerTheRecord() {
        assertThrows(
                AccessDeniedException.class,
                () -> strategy.prepareCreate(
                        record(),
                        new CreateVaccinationCorrectionRequest("doseNumber", "2", "Verified amendment"),
                        92L
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
