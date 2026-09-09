package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.RevokeDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.exception.CertificateAlreadyRevokedException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateLifecycleException;
import com.se2030.vaccinationportal.vaccination.exception.NoValidDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
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
class DigitalCertificateLifecycleServiceTests {

    private static final Long RECORD_ID = 101L;
    private static final Instant NOW = Instant.parse("2026-09-09T10:00:00Z");

    @Mock
    private VaccinationRecordRepository vaccinationRecordRepository;

    @Mock
    private DigitalCertificateRepository certificateRepository;

    private DigitalCertificateService service;

    @BeforeEach
    void setUp() {
        service = new DigitalCertificateService(
                vaccinationRecordRepository,
                certificateRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void validCertificateCanBeRevokedWithoutChangingItsIdentity() {
        DigitalCertificate certificate = certificate(201L, "CERT-OLD", "old-token");
        when(certificateRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(certificate));
        when(certificateRepository.saveAndFlush(certificate)).thenReturn(certificate);

        DigitalCertificate result = service.revokeCertificate(
                201L,
                new RevokeDigitalCertificateRequest("Incorrect recipient name", 501L)
        );

        assertSame(certificate, result);
        assertEquals(CertificateStatus.REVOKED, result.getStatus());
        assertEquals(NOW, result.getRevokedAt());
        assertEquals(501L, result.getRevokedBy());
        assertEquals("Incorrect recipient name", result.getRevocationReason());
        assertEquals("CERT-OLD", result.getCertificateReference());
        assertEquals("old-token", result.getVerificationToken());
        assertNull(result.getSupersededByCertificateId());
        verify(certificateRepository, never()).delete(any(DigitalCertificate.class));
    }

    @Test
    void alreadyRevokedCertificateIsRejected() {
        DigitalCertificate certificate = certificate(201L, "CERT-OLD", "old-token");
        certificate.revoke(NOW.minusSeconds(60), 500L, "Earlier revocation");
        when(certificateRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(certificate));

        assertThrows(
                CertificateAlreadyRevokedException.class,
                () -> service.revokeCertificate(
                        201L,
                        new RevokeDigitalCertificateRequest("Second revocation", 501L)
                )
        );
        verify(certificateRepository, never()).saveAndFlush(any(DigitalCertificate.class));
    }

    @Test
    void blankReasonAndInvalidActorAreRejectedBeforeLocking() {
        assertThrows(
                InvalidCertificateLifecycleException.class,
                () -> service.revokeCertificate(201L, new RevokeDigitalCertificateRequest("  ", 501L))
        );
        assertThrows(
                InvalidCertificateLifecycleException.class,
                () -> service.revokeCertificate(201L, new RevokeDigitalCertificateRequest("Reason", 0L))
        );
        verify(certificateRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void reissueRevokesCurrentCertificateAndCreatesLinkedReplacement() {
        VaccinationRecord record = record();
        DigitalCertificate oldCertificate = certificate(201L, "CERT-OLD", "old-token");
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(certificateRepository.findCurrentByVaccinationRecordIdAndStatusForUpdate(
                RECORD_ID,
                CertificateStatus.VALID
        )).thenReturn(List.of(oldCertificate));
        when(certificateRepository.saveAndFlush(any(DigitalCertificate.class))).thenAnswer(invocation -> {
            DigitalCertificate saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", 202L);
            }
            return saved;
        });

        CertificateReissueResponse result = service.reissueCertificate(
                RECORD_ID,
                new ReissueDigitalCertificateRequest("Corrected certificate data", 501L)
        );

        assertEquals(201L, result.revokedCertificate().id());
        assertEquals(CertificateStatus.REVOKED, result.revokedCertificate().status());
        assertEquals(202L, result.revokedCertificate().supersededByCertificateId());
        assertEquals(202L, result.issuedCertificate().id());
        assertEquals(CertificateStatus.VALID, result.issuedCertificate().status());
        assertEquals(201L, result.issuedCertificate().reissuedFromCertificateId());
        assertFalse(result.revokedCertificate().certificateReference()
                .equals(result.issuedCertificate().certificateReference()));
        assertEquals(RECORD_ID, oldCertificate.getVaccinationRecordId());
    }

    @Test
    void reissueRejectsMissingRecordOrMissingCurrentCertificate() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());
        assertThrows(
                VaccinationRecordNotFoundException.class,
                () -> service.reissueCertificate(
                        RECORD_ID,
                        new ReissueDigitalCertificateRequest("Reason", 501L)
                )
        );

        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        when(certificateRepository.findCurrentByVaccinationRecordIdAndStatusForUpdate(
                RECORD_ID,
                CertificateStatus.VALID
        )).thenReturn(List.of());
        assertThrows(
                NoValidDigitalCertificateException.class,
                () -> service.reissueCertificate(
                        RECORD_ID,
                        new ReissueDigitalCertificateRequest("Reason", 501L)
                )
        );
    }

    @Test
    void invalidReissueRequestIsRejectedBeforeCertificateLocking() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        assertThrows(
                InvalidCertificateLifecycleException.class,
                () -> service.reissueCertificate(
                        RECORD_ID,
                        new ReissueDigitalCertificateRequest(" ", 501L)
                )
        );
        assertThrows(
                InvalidCertificateLifecycleException.class,
                () -> service.reissueCertificate(
                        RECORD_ID,
                        new ReissueDigitalCertificateRequest("Reason", null)
                )
        );

        verify(certificateRepository, never())
                .findCurrentByVaccinationRecordIdAndStatusForUpdate(any(), any());
    }

    @Test
    void historyIsMappedInRepositoryOrderWithoutSensitiveFields() {
        DigitalCertificate first = certificate(201L, "CERT-OLD", "old-token");
        first.revoke(NOW.minusSeconds(30), 501L, "Reissued");
        DigitalCertificate second = new DigitalCertificate(
                RECORD_ID,
                "CERT-NEW",
                "new-token",
                NOW,
                201L
        );
        ReflectionTestUtils.setField(second, "id", 202L);
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record()));
        when(certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(RECORD_ID))
                .thenReturn(List.of(first, second));

        List<DigitalCertificateResponse> history = service.getCertificateHistory(RECORD_ID);

        assertEquals(List.of(201L, 202L), history.stream().map(DigitalCertificateResponse::id).toList());
        assertEquals(CertificateStatus.REVOKED, history.get(0).status());
        assertEquals(CertificateStatus.VALID, history.get(1).status());
    }

    private DigitalCertificate certificate(Long id, String reference, String token) {
        DigitalCertificate certificate = new DigitalCertificate(RECORD_ID, reference, token, NOW.minusSeconds(60));
        ReflectionTestUtils.setField(certificate, "id", id);
        return certificate;
    }

    private VaccinationRecord record() {
        VaccinationRecord record = new VaccinationRecord(
                1L, 2L, 3L, 4L, 5L, 6L, 1, NOW.minusSeconds(3600), null, null, NOW
        );
        ReflectionTestUtils.setField(record, "id", RECORD_ID);
        return record;
    }
}
