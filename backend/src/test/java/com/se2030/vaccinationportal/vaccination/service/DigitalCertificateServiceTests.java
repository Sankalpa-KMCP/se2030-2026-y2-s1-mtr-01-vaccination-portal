package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.exception.CertificateVerificationNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateVerificationTokenException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DigitalCertificateServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-02T08:30:00Z");
    private static final Long RECORD_ID = 101L;

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
    void validVaccinationRecordIssuesValidCertificateWithRandomIdentifiers() {
        VaccinationRecord record = org.mockito.Mockito.mock(VaccinationRecord.class);
        when(record.getId()).thenReturn(RECORD_ID);
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(certificateRepository.existsByVaccinationRecordId(RECORD_ID)).thenReturn(false);
        when(certificateRepository.saveAndFlush(any(DigitalCertificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DigitalCertificate issued = service.issueCertificateForVaccinationRecord(RECORD_ID);

        assertEquals(RECORD_ID, issued.getVaccinationRecordId());
        assertEquals(CertificateStatus.VALID, issued.getStatus());
        assertEquals(NOW, issued.getIssuedAt());
        assertFalse(issued.getCertificateReference().isBlank());
        assertFalse(issued.getVerificationToken().isBlank());

        ArgumentCaptor<DigitalCertificate> certificateCaptor = ArgumentCaptor.forClass(DigitalCertificate.class);
        verify(certificateRepository).saveAndFlush(certificateCaptor.capture());
        assertSame(issued, certificateCaptor.getValue());
        verify(vaccinationRecordRepository, never()).save(any(VaccinationRecord.class));
    }

    @Test
    void duplicateCertificateForVaccinationRecordIsRejectedWithoutPersistence() {
        VaccinationRecord record = org.mockito.Mockito.mock(VaccinationRecord.class);
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));
        when(certificateRepository.existsByVaccinationRecordId(RECORD_ID)).thenReturn(true);

        assertThrows(
                DuplicateDigitalCertificateException.class,
                () -> service.issueCertificateForVaccinationRecord(RECORD_ID)
        );

        verify(certificateRepository, never()).saveAndFlush(any(DigitalCertificate.class));
    }

    @Test
    void unknownVaccinationRecordIsRejectedBeforeCertificateRepositoryAccess() {
        when(vaccinationRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        assertThrows(
                VaccinationRecordNotFoundException.class,
                () -> service.issueCertificateForVaccinationRecord(RECORD_ID)
        );

        verifyNoInteractions(certificateRepository);
    }

    @Test
    void getCertificateByIdReturnsExistingCertificate() {
        DigitalCertificate certificate = certificate();
        when(certificateRepository.findById(201L)).thenReturn(Optional.of(certificate));

        assertSame(certificate, service.getCertificateById(201L));
    }

    @Test
    void getCertificateByIdRejectsUnknownCertificate() {
        when(certificateRepository.findById(201L)).thenReturn(Optional.empty());

        assertThrows(
                DigitalCertificateNotFoundException.class,
                () -> service.getCertificateById(201L)
        );
    }

    @Test
    void getCertificateByReferenceReturnsExistingCertificate() {
        DigitalCertificate certificate = certificate();
        when(certificateRepository.findByCertificateReference("CERT-EXISTING"))
                .thenReturn(Optional.of(certificate));

        assertSame(certificate, service.getCertificateByReference("CERT-EXISTING"));
    }

    @Test
    void getCertificateByReferenceRejectsUnknownCertificate() {
        when(certificateRepository.findByCertificateReference("CERT-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                DigitalCertificateNotFoundException.class,
                () -> service.getCertificateByReference("CERT-UNKNOWN")
        );
    }

    @Test
    void validVerificationTokenReturnsValidResponseWithoutMutation() {
        DigitalCertificate certificate = certificate();
        when(certificateRepository.findByVerificationToken("secure-token"))
                .thenReturn(Optional.of(certificate));

        CertificateVerificationResponse response = service.verifyCertificate(" secure-token ");

        assertEquals("CERT-EXISTING", response.certificateReference());
        assertEquals(RECORD_ID, response.vaccinationRecordId());
        assertEquals(NOW, response.issuedAt());
        assertEquals(CertificateStatus.VALID, response.status());
        assertTrue(response.valid());
        assertEquals(CertificateStatus.VALID, certificate.getStatus());
        verify(certificateRepository, never()).save(any(DigitalCertificate.class));
        verify(certificateRepository, never()).saveAndFlush(any(DigitalCertificate.class));
    }

    @Test
    void revokedCertificateReturnsInvalidResponseWithoutChangingStatus() {
        DigitalCertificate certificate = certificate();
        ReflectionTestUtils.setField(certificate, "status", CertificateStatus.REVOKED);
        when(certificateRepository.findByVerificationToken("revoked-token"))
                .thenReturn(Optional.of(certificate));

        CertificateVerificationResponse response = service.verifyCertificate("revoked-token");

        assertFalse(response.valid());
        assertEquals(CertificateStatus.REVOKED, response.status());
        assertEquals(CertificateStatus.REVOKED, certificate.getStatus());
        verify(certificateRepository, never()).save(any(DigitalCertificate.class));
        verify(certificateRepository, never()).saveAndFlush(any(DigitalCertificate.class));
    }

    @Test
    void unknownVerificationTokenIsRejected() {
        when(certificateRepository.findByVerificationToken("unknown-token"))
                .thenReturn(Optional.empty());

        assertThrows(
                CertificateVerificationNotFoundException.class,
                () -> service.verifyCertificate("unknown-token")
        );
    }

    @Test
    void blankOrMissingVerificationTokenIsRejectedBeforeRepositoryAccess() {
        assertThrows(
                InvalidCertificateVerificationTokenException.class,
                () -> service.verifyCertificate("   ")
        );
        assertThrows(
                InvalidCertificateVerificationTokenException.class,
                () -> service.verifyCertificate(null)
        );

        verifyNoInteractions(certificateRepository);
    }

    private DigitalCertificate certificate() {
        return new DigitalCertificate(RECORD_ID, "CERT-EXISTING", "secure-token", NOW);
    }
}
