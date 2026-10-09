package com.se2030.vaccinationportal.vaccination.administration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.administration.strategy.CorrectionRequestStrategy;
import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationCorrectionValueCodec;
import java.time.Clock;
import java.time.Instant;
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
class VaccinationCorrectionStrategyDelegationTests {

    @Mock
    private VaccinationRecordRepository recordRepository;
    @Mock
    private VaccinationCorrectionRepository correctionRepository;
    @Mock
    private EffectiveVaccinationRecordService effectiveRecordService;
    @Mock
    private CorrectionRequestStrategy vaccinatorStrategy;
    @Mock
    private CorrectionRequestStrategy citizenStrategy;

    private VaccinationCorrectionService context;
    private VaccinationRecord record;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC);
        context = new VaccinationCorrectionService(
                recordRepository,
                correctionRepository,
                effectiveRecordService,
                new VaccinationCorrectionValueCodec(clock),
                clock,
                null,
                null,
                List.of(vaccinatorStrategy, citizenStrategy)
        );
        record = new VaccinationRecord(
                1001L, 55L, 3001L, 4001L, 91L, 56L, 1,
                Instant.parse("2026-09-01T08:00:00Z"), null, "Original note",
                Instant.parse("2026-09-01T08:05:00Z")
        );
        ReflectionTestUtils.setField(record, "id", 101L);
    }

    @Test
    void contextSelectsByRequestTypeAndDelegatesCreationPolicy() {
        CreateVaccinationCorrectionRequest original =
                new CreateVaccinationCorrectionRequest("Certificate issue");
        CreateVaccinationCorrectionRequest prepared =
                new CreateVaccinationCorrectionRequest("authorizedNotes", null, "Certificate issue", 55L);

        when(recordRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(record));
        when(vaccinatorStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(false);
        when(citizenStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(true);
        when(citizenStrategy.prepareCreate(record, original, 55L)).thenReturn(prepared);
        when(correctionRepository.findActiveByRecordForUpdate(101L)).thenReturn(List.of());
        when(effectiveRecordService.resolve(record)).thenReturn(snapshot());
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection created = context.createCitizenCorrection(101L, original, 55L);

        assertEquals(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE, created.getRequestType());
        verify(citizenStrategy).prepareCreate(record, original, 55L);
        verify(vaccinatorStrategy, never()).prepareCreate(any(), any(), any());
    }

    @Test
    void contextSelectsVaccinatorStrategyAndDelegatesAmendmentPolicy() {
        CreateVaccinationCorrectionRequest original =
                new CreateVaccinationCorrectionRequest("doseNumber", "2", "Administered dose 2");
        CreateVaccinationCorrectionRequest prepared =
                new CreateVaccinationCorrectionRequest("doseNumber", "2", "Administered dose 2", 91L, null,
                        VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT.name());

        when(recordRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(record));
        when(vaccinatorStrategy.supports(VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT)).thenReturn(true);
        when(citizenStrategy.supports(VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT)).thenReturn(false);
        when(vaccinatorStrategy.prepareCreate(record, original, 91L)).thenReturn(prepared);
        when(correctionRepository.findActiveByRecordForUpdate(101L)).thenReturn(List.of());
        when(effectiveRecordService.resolve(record)).thenReturn(snapshot());
        when(correctionRepository.save(any(VaccinationCorrection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VaccinationCorrection created = context.createVaccinatorAmendment(101L, original, 91L);

        assertEquals(VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT, created.getRequestType());
        verify(vaccinatorStrategy).prepareCreate(record, original, 91L);
        verify(citizenStrategy, never()).prepareCreate(any(), any(), any());
    }

    @Test
    void contextThrowsControlledErrorWhenNoStrategySupportsRequestType() {
        CreateVaccinationCorrectionRequest request = new CreateVaccinationCorrectionRequest("Certificate issue");
        when(recordRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(record));
        when(vaccinatorStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(false);
        when(citizenStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(false);

        InvalidVaccinationCorrectionException ex = assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> context.createCitizenCorrection(101L, request, 55L)
        );
        assertTrue(ex.getMessage().contains("No correction request strategy configured for type: CITIZEN_CERTIFICATE_ISSUE"));
        verify(citizenStrategy, never()).prepareCreate(any(), any(), any());
        verify(vaccinatorStrategy, never()).prepareCreate(any(), any(), any());
        verify(correctionRepository, never()).save(any());
    }

    @Test
    void contextRejectsAmbiguousConfigurationWhenMultipleStrategiesSupportSameType() {
        CreateVaccinationCorrectionRequest request = new CreateVaccinationCorrectionRequest("Certificate issue");
        when(recordRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(record));
        when(vaccinatorStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(true);
        when(citizenStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> context.createCitizenCorrection(101L, request, 55L)
        );
        assertTrue(ex.getMessage().contains("Multiple correction request strategies configured for type: CITIZEN_CERTIFICATE_ISSUE"));
        verify(citizenStrategy, never()).prepareCreate(any(), any(), any());
        verify(vaccinatorStrategy, never()).prepareCreate(any(), any(), any());
        verify(correctionRepository, never()).save(any());
    }

    @Test
    void invalidStrategyRequestIsNotPersisted() {
        CreateVaccinationCorrectionRequest original =
                new CreateVaccinationCorrectionRequest("doseNumber", "2", "Citizen tries clinical change");

        when(recordRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(record));
        when(vaccinatorStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(false);
        when(citizenStrategy.supports(VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE)).thenReturn(true);
        when(citizenStrategy.prepareCreate(record, original, 55L))
                .thenThrow(new InvalidVaccinationCorrectionException("Clinical changes blocked for citizen"));

        assertThrows(
                InvalidVaccinationCorrectionException.class,
                () -> context.createCitizenCorrection(101L, original, 55L)
        );

        verify(correctionRepository, never()).save(any());
    }

    private EffectiveVaccinationRecordSnapshot snapshot() {
        return new EffectiveVaccinationRecordSnapshot(
                101L, 1001L, 55L, 3001L, 4001L, 91L, 56L, 1,
                Instant.parse("2026-09-01T08:00:00Z"), null, "Original note",
                Instant.parse("2026-09-01T08:05:00Z")
        );
    }
}
