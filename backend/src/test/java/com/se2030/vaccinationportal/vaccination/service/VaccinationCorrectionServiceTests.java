package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VaccinationCorrectionServiceTests {

    private static final long RECORD_ID = 101L;
    private static final Instant NOW = Instant.parse("2026-09-08T10:00:00Z");

    @Mock
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Mock
    private VaccinationCorrectionRepository correctionRepository;

    private VaccinationCorrectionService service;
    private VaccinationRecord record;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        VaccinationCorrectionValueCodec valueCodec = new VaccinationCorrectionValueCodec(clock);
        EffectiveVaccinationRecordService effectiveRecordService = new EffectiveVaccinationRecordService(
                vaccinationRecordRepository,
                correctionRepository,
                valueCodec
        );
        service = new VaccinationCorrectionService(
                vaccinationRecordRepository,
                correctionRepository,
                effectiveRecordService,
                valueCodec,
                clock
        );
        record = record();
    }

    @Test
    void correctionCapturesOriginalAndCorrectedValuesActorAndTimestampWithoutEditingRecord() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request("vaccineBatchId", "78", " Wrong batch recorded ", 91L)
        );

        assertEquals(RECORD_ID, correction.getVaccinationRecordId());
        assertEquals(VaccinationCorrectionField.VACCINE_BATCH_ID, correction.getFieldName());
        assertEquals("56", correction.getOldValue());
        assertEquals("78", correction.getNewValue());
        assertEquals("Wrong batch recorded", correction.getReason());
        assertEquals(91L, correction.getCorrectedBy());
        assertEquals(NOW, correction.getCorrectedAt());
        assertEquals(VaccinationCorrectionStatus.PENDING, correction.getStatus());
        assertEquals(56L, record.getVaccineBatchId());
        verify(vaccinationRecordRepository, never()).save(any(VaccinationRecord.class));
    }

    @Test
    void unknownVaccinationRecordIsRejectedBeforeCorrectionPersistence() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        assertThrows(
                VaccinationRecordNotFoundException.class,
                () -> service.createCorrection(
                        RECORD_ID,
                        request("doseNumber", "2", "Correct dose", 91L)
                )
        );

        verifyNoInteractions(correctionRepository);
    }

    @Test
    void unsupportedFieldIsRejected() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        assertThrows(
                UnsupportedVaccinationCorrectionFieldException.class,
                () -> service.createCorrection(
                        RECORD_ID,
                        request("citizenId", "999", "Incorrect citizen", 91L)
                )
        );

        verify(correctionRepository, never()).save(any());
    }

    @Test
    void blankReasonIsRejected() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> service.createCorrection(
                        RECORD_ID,
                        request("doseNumber", "2", "   ", 91L)
                )
        );

        verify(correctionRepository, never()).save(any());
    }

    @Test
    void valueEqualToOriginalAfterNormalizationIsRejected() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

        assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> service.createCorrection(
                        RECORD_ID,
                        request("vaccineBatchId", " 056 ", "No actual change", 91L)
                )
        );

        verify(correctionRepository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("supportedCorrectionCases")
    void everyApprovedFieldCapturesCanonicalOldAndNewValue(
            String fieldName,
            String newValue,
            VaccinationCorrectionField expectedField,
            String expectedOldValue,
            String expectedNewValue
    ) {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request(fieldName, newValue, "Verified correction", 91L)
        );

        assertEquals(expectedField, correction.getFieldName());
        assertEquals(expectedOldValue, correction.getOldValue());
        assertEquals(expectedNewValue, correction.getNewValue());
    }

    @Test
    void nullableApprovedFieldCanBeClearedAndRetainsOriginalValueInAudit() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request("authorizedNotes", "  ", "Remove inaccurate note", 91L)
        );

        assertEquals("Original note", correction.getOldValue());
        assertNull(correction.getNewValue());
    }

    @Test
    void chainedCorrectionUsesLatestSameFieldValueAsEffectiveOldValue() {
        VaccinationCorrection prior = correction(VaccinationCorrectionField.VACCINE_BATCH_ID, "56", "78");
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID))
                .thenReturn(List.of(prior));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request("vaccineBatchId", "90", "Second batch correction", 91L)
        );

        assertEquals("78", correction.getOldValue());
        assertEquals("90", correction.getNewValue());
        assertEquals(56L, record.getVaccineBatchId());
    }

    @Test
    void duplicateEffectiveValueIsRejected() {
        VaccinationCorrection prior = correction(VaccinationCorrectionField.VACCINE_BATCH_ID, "56", "78");
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID))
                .thenReturn(List.of(prior));

        assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> service.createCorrection(
                        RECORD_ID,
                        request("vaccineBatchId", "78", "Duplicate effective value", 91L)
                )
        );

        verify(correctionRepository, never()).save(any());
    }

    @Test
    void correctionBackToOriginalValueIsAllowedWhenEffectiveValueDiffers() {
        VaccinationCorrection prior = correction(VaccinationCorrectionField.VACCINE_BATCH_ID, "56", "78");
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID))
                .thenReturn(List.of(prior));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request("vaccineBatchId", "56", "Restore original batch", 91L)
        );

        assertEquals("78", correction.getOldValue());
        assertEquals("56", correction.getNewValue());
        assertEquals(56L, record.getVaccineBatchId());
    }

    @Test
    void latestCorrectionForAnotherFieldDoesNotAffectEffectiveValue() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request("vaccineBatchId", "78", "Batch correction", 91L)
        );

        assertEquals("56", correction.getOldValue());
        verify(correctionRepository).findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID);
    }

    @Test
    void nullableFieldChainUsesLatestNullAsEffectiveOldValue() {
        VaccinationCorrection prior = correction(
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Original note",
                null
        );
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID))
                .thenReturn(List.of(prior));
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection correction = service.createCorrection(
                RECORD_ID,
                request("authorizedNotes", "Restored note", "Restore note", 91L)
        );

        assertNull(correction.getOldValue());
        assertEquals("Restored note", correction.getNewValue());
    }

    @Test
    void historyRequiresExistingRecordAndUsesDeterministicRepositoryOrdering() {
        VaccinationCorrection first = correction(VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        VaccinationCorrection second = correction(
                VaccinationCorrectionField.AUTHORIZED_NOTES,
                "Original note",
                "Updated note"
        );
        List<VaccinationCorrection> ordered = List.of(first, second);
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID))
                .thenReturn(ordered);

        assertSame(ordered, service.getCorrectionHistory(RECORD_ID));
        verify(correctionRepository).findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(RECORD_ID);
    }

    private static Stream<Arguments> supportedCorrectionCases() {
        return Stream.of(
                Arguments.of(
                        "vaccineBatchId", "78", VaccinationCorrectionField.VACCINE_BATCH_ID, "56", "78"
                ),
                Arguments.of("doseNumber", "2", VaccinationCorrectionField.DOSE_NUMBER, "1", "2"),
                Arguments.of(
                        "administrationTime",
                        "2026-09-01T09:30:00Z",
                        VaccinationCorrectionField.ADMINISTRATION_TIME,
                        "2026-09-01T08:00:00Z",
                        "2026-09-01T09:30:00Z"
                ),
                Arguments.of(
                        "nextDoseDate",
                        "2026-10-15",
                        VaccinationCorrectionField.NEXT_DOSE_DATE,
                        "2026-10-01",
                        "2026-10-15"
                ),
                Arguments.of(
                        "authorizedNotes",
                        "Updated note",
                        VaccinationCorrectionField.AUTHORIZED_NOTES,
                        "Original note",
                        "Updated note"
                )
        );
    }

    private CreateVaccinationCorrectionRequest request(
            String fieldName,
            String newValue,
            String reason,
            Long correctedBy
    ) {
        return new CreateVaccinationCorrectionRequest(fieldName, newValue, reason, correctedBy);
    }

    private VaccinationCorrection correction(
            VaccinationCorrectionField field,
            String oldValue,
            String newValue
    ) {
        return new VaccinationCorrection(
                RECORD_ID,
                field,
                oldValue,
                newValue,
                "Reason",
                91L,
                NOW,
                VaccinationCorrectionStatus.APPLIED
        );
    }

    private VaccinationRecord record() {
        VaccinationRecord vaccinationRecord = new VaccinationRecord(
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
        ReflectionTestUtils.setField(vaccinationRecord, "id", RECORD_ID);
        return vaccinationRecord;
    }
}
