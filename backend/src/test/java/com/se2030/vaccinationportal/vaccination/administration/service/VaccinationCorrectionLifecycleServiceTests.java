package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.certificate.CertificateImpactPolicy;
import com.se2030.vaccinationportal.vaccination.certificate.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.certificate.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationCorrectionLifecycleServiceTests {

    private static final Instant ADMINISTERED_AT = Instant.parse("2026-09-01T08:00:00Z");

    @Mock
    private EffectiveVaccinationRecordService effectiveRecordService;

    @Mock
    private VaccinationCorrectionService correctionService;

    @Mock
    private DigitalCertificateService certificateService;

    private VaccinationCorrectionLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new VaccinationCorrectionLifecycleService(
                effectiveRecordService,
                correctionService,
                new CertificateImpactPolicy(),
                certificateService
        );
    }

    @Test
    void nonCertificateImpactingCorrectionDoesNotCheckOrReissueCertificate() {
        VaccinationCorrection correction = correction(VaccinationCorrectionField.AUTHORIZED_NOTES, "Original", "Updated note");
        correction.approve(92L, Instant.now(), "Approved");
        when(effectiveRecordService.getEffectiveRecord(101L))
                .thenReturn(snapshot(1, ADMINISTERED_AT, 56L, "Original"))
                .thenReturn(snapshot(1, ADMINISTERED_AT, 56L, "Updated note"));
        when(correctionService.getCorrectionById(101L, 1L)).thenReturn(correction);

        VaccinationCorrectionLifecycleResponse result = lifecycleService.applyApprovedCorrection(101L, 1L, 92L);

        assertFalse(result.certificateImpacted());
        assertFalse(result.certificateReissued());
        assertNull(result.certificateReissue());
        verify(certificateService, never()).hasCurrentValidCertificate(any());
        verify(certificateService, never()).reissueCertificate(any(), any());
    }

    @Test
    void impactingCorrectionWithValidCertificateUsesControlledReissue() {
        VaccinationCorrection correction = correction(VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        correction.approve(92L, Instant.now(), "Approved");
        CertificateReissueResponse reissue = reissueResponse();
        when(effectiveRecordService.getEffectiveRecord(101L))
                .thenReturn(snapshot(1, ADMINISTERED_AT, 56L, "Original"))
                .thenReturn(snapshot(2, ADMINISTERED_AT, 56L, "Original"));
        when(correctionService.getCorrectionById(101L, 1L)).thenReturn(correction);
        when(certificateService.hasCurrentValidCertificate(101L)).thenReturn(true);
        when(certificateService.reissueCertificate(eq(101L), any(ReissueDigitalCertificateRequest.class)))
                .thenReturn(reissue);

        VaccinationCorrectionLifecycleResponse result = lifecycleService.applyApprovedCorrection(101L, 1L, 92L);

        assertTrue(result.certificateImpacted());
        assertTrue(result.certificateReissued());
        assertSame(reissue, result.certificateReissue());
        verify(certificateService).reissueCertificate(
                101L,
                new ReissueDigitalCertificateRequest("Correction reason", 92L)
        );
    }

    @Test
    void impactingCorrectionWithoutValidCertificateDoesNotIssueInitialCertificate() {
        VaccinationCorrection correction = correction(VaccinationCorrectionField.VACCINE_BATCH_ID, "56", "78");
        correction.approve(92L, Instant.now(), "Approved");
        when(effectiveRecordService.getEffectiveRecord(101L))
                .thenReturn(snapshot(1, ADMINISTERED_AT, 56L, "Original"))
                .thenReturn(snapshot(1, ADMINISTERED_AT, 78L, "Original"));
        when(correctionService.getCorrectionById(101L, 1L)).thenReturn(correction);
        when(certificateService.hasCurrentValidCertificate(101L)).thenReturn(false);

        VaccinationCorrectionLifecycleResponse result = lifecycleService.applyApprovedCorrection(101L, 1L, 92L);

        assertTrue(result.certificateImpacted());
        assertFalse(result.certificateReissued());
        assertNull(result.certificateReissue());
        verify(certificateService, never()).issueCertificateForVaccinationRecord(any());
        verify(certificateService, never()).reissueCertificate(any(), any());
    }

    @Test
    void reissueFailureIsNotSuppressed() {
        VaccinationCorrection correction = correction(
                VaccinationCorrectionField.ADMINISTRATION_TIME,
                ADMINISTERED_AT.toString(),
                "2026-09-01T09:00:00Z"
        );
        correction.approve(92L, Instant.now(), "Approved");
        RuntimeException failure = new RuntimeException("reissue failed");
        when(effectiveRecordService.getEffectiveRecord(101L))
                .thenReturn(snapshot(1, ADMINISTERED_AT, 56L, "Original"))
                .thenReturn(snapshot(1, Instant.parse("2026-09-01T09:00:00Z"), 56L, "Original"));
        when(correctionService.getCorrectionById(101L, 1L)).thenReturn(correction);
        when(certificateService.hasCurrentValidCertificate(101L)).thenReturn(true);
        when(certificateService.reissueCertificate(eq(101L), any())).thenThrow(failure);

        assertSame(failure, assertThrows(
                RuntimeException.class,
                () -> lifecycleService.applyApprovedCorrection(101L, 1L, 92L)
        ));
    }

    @Test
    void unapprovedCorrectionCannotBeApplied() {
        VaccinationCorrection correction = correction(VaccinationCorrectionField.DOSE_NUMBER, "1", "2");
        when(correctionService.getCorrectionById(101L, 1L)).thenReturn(correction);

        com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException ex = assertThrows(
                com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException.class,
                () -> lifecycleService.applyApprovedCorrection(101L, 1L, 92L)
        );
        assertTrue(ex.getMessage().contains("Only APPROVED corrections can be applied"));
    }

    private VaccinationCorrection correction(VaccinationCorrectionField field, String oldValue, String newValue) {
        return new VaccinationCorrection(101L, field, oldValue, newValue, "Correction reason", 91L, ADMINISTERED_AT);
    }

    private EffectiveVaccinationRecordSnapshot snapshot(
            int dose,
            Instant administrationTime,
            long batchId,
            String notes
    ) {
        return new EffectiveVaccinationRecordSnapshot(
                101L, 1001L, 2001L, 3001L, 4001L, 91L, batchId, dose,
                administrationTime, null, notes, Instant.parse("2026-09-01T08:05:00Z")
        );
    }

    private CertificateReissueResponse reissueResponse() {
        return new CertificateReissueResponse(null, null);
    }
}
