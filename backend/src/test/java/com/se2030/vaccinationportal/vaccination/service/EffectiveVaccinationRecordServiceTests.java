package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationCorrectionValueCodec;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EffectiveVaccinationRecordServiceTests {

    private static final Long RECORD_ID = 101L;
    private static final Instant NOW = Instant.parse("2026-09-09T10:00:00Z");

    @Mock
    private VaccinationRecordRepository recordRepository;

    @Mock
    private VaccinationCorrectionRepository correctionRepository;

    private EffectiveVaccinationRecordService service;
    private VaccinationRecord record;

    @BeforeEach
    void setUp() {
        var codec = new VaccinationCorrectionValueCodec(Clock.fixed(NOW, ZoneOffset.UTC));
        service = new EffectiveVaccinationRecordService(recordRepository, correctionRepository, codec);
        record = record();
    }

    @Test
    void noCorrectionsReturnsOriginalTypedValues() {
        arrange(List.of());

        EffectiveVaccinationRecordSnapshot result = service.getEffectiveRecord(RECORD_ID);

        assertEquals(RECORD_ID, result.vaccinationRecordId());
        assertEquals(56L, result.vaccineBatchId());
        assertEquals(1, result.doseNumber());
        assertEquals(Instant.parse("2026-09-01T08:00:00Z"), result.administrationTime());
        assertEquals(LocalDate.parse("2026-10-01"), result.nextDoseDate());
        assertEquals("Original note", result.authorizedNotes());
    }

    @Test
    void latestCorrectionPerFieldOverlaysIndependentlyInDeterministicOrder() {
        arrange(List.of(
                correction(VaccinationCorrectionField.VACCINE_BATCH_ID, "56", "78", NOW.minusSeconds(4)),
                correction(VaccinationCorrectionField.DOSE_NUMBER, "1", "2", NOW.minusSeconds(3)),
                correction(VaccinationCorrectionField.VACCINE_BATCH_ID, "78", "90", NOW.minusSeconds(2)),
                correction(
                        VaccinationCorrectionField.ADMINISTRATION_TIME,
                        "2026-09-01T08:00:00Z",
                        "2026-09-01T09:30:00Z",
                        NOW.minusSeconds(1)
                )
        ));

        EffectiveVaccinationRecordSnapshot result = service.getEffectiveRecord(RECORD_ID);

        assertEquals(90L, result.vaccineBatchId());
        assertEquals(2, result.doseNumber());
        assertEquals(Instant.parse("2026-09-01T09:30:00Z"), result.administrationTime());
        assertEquals("Original note", result.authorizedNotes());
    }

    @Test
    void nullableCorrectionsCanClearNextDoseDateAndNotes() {
        arrange(List.of(
                correction(VaccinationCorrectionField.NEXT_DOSE_DATE, "2026-10-01", null, NOW.minusSeconds(2)),
                correction(VaccinationCorrectionField.AUTHORIZED_NOTES, "Original note", null, NOW.minusSeconds(1))
        ));

        EffectiveVaccinationRecordSnapshot result = service.getEffectiveRecord(RECORD_ID);

        assertNull(result.nextDoseDate());
        assertNull(result.authorizedNotes());
    }

    @Test
    void resolvingCorrectionsNeverMutatesPermanentRecord() {
        arrange(List.of(correction(VaccinationCorrectionField.DOSE_NUMBER, "1", "2", NOW)));

        EffectiveVaccinationRecordSnapshot result = service.getEffectiveRecord(RECORD_ID);

        assertEquals(2, result.doseNumber());
        assertEquals(1, record.getDoseNumber());
        assertEquals(56L, record.getVaccineBatchId());
    }

    @Test
    void missingRecordUsesExistingNotFoundException() {
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        assertThrows(
                VaccinationRecordNotFoundException.class,
                () -> service.getEffectiveRecord(RECORD_ID)
        );
    }

    private void arrange(List<VaccinationCorrection> corrections) {
        when(recordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID))
                .thenReturn(corrections);
    }

    private VaccinationCorrection correction(
            VaccinationCorrectionField field,
            String oldValue,
            String newValue,
            Instant correctedAt
    ) {
        return new VaccinationCorrection(
                RECORD_ID,
                field,
                oldValue,
                newValue,
                "Reason",
                91L,
                correctedAt,
                com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.APPLIED
        );
    }

    private VaccinationRecord record() {
        VaccinationRecord result = new VaccinationRecord(
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
        ReflectionTestUtils.setField(result, "id", RECORD_ID);
        return result;
    }
}
