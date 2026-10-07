package com.se2030.vaccinationportal.vaccination.certificate.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DigitalCertificateModelTests {

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

}
