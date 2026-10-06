package com.se2030.vaccinationportal.vaccination.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.se2030.vaccinationportal.vaccination.administration.dto.CertificateDisplayDetails;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificatePdfServiceTest {

    private CertificatePdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new CertificatePdfService();
    }

    @Test
    void generatesValidPdfForValidCertificate() {
        VaccinationRecord record = new VaccinationRecord(
                101L, 202L, 303L, 404L, 505L, 606L, 1,
                Instant.parse("2026-09-01T10:00:00Z"),
                LocalDate.parse("2026-10-01"),
                "Authorized test notes",
                Instant.parse("2026-09-01T10:05:00Z")
        );
        DigitalCertificate cert = new DigitalCertificate(
                1L, "CERT-TEST-12345", "token-xyz-secret",
                Instant.parse("2026-09-01T10:10:00Z"), null,
                606L, 1, Instant.parse("2026-09-01T10:00:00Z")
        );

        byte[] pdf = pdfService.generateCertificatePdf(cert, record);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(100);

        String pdfString = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(pdfString).startsWith("%PDF-1.4");
        assertThat(pdfString).contains("%%EOF");
        assertThat(pdfString).contains("CERT-TEST-12345");
        assertThat(pdfString).contains("STATUS: VALID");
        assertThat(pdfString).contains("token-xyz-secret");
    }

    @Test
    void generatesValidPdfForRevokedCertificate() {
        DigitalCertificate cert = new DigitalCertificate(
                1L, "CERT-REVOKED-12345", "token-revoked-secret",
                Instant.parse("2026-09-01T10:10:00Z"), null,
                606L, 1, Instant.parse("2026-09-01T10:00:00Z")
        );
        cert.revoke(Instant.parse("2026-09-02T12:00:00Z"), 999L, "Safety recall");

        byte[] pdf = pdfService.generateCertificatePdf(cert, null);

        assertThat(pdf).isNotNull();
        String pdfString = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(pdfString).startsWith("%PDF-1.4");
        assertThat(pdfString).contains("STATUS: REVOKED");
    }

    @Test
    void generatesValidPdfWithDisplayDetails() {
        VaccinationRecord record = new VaccinationRecord(
                101L, 202L, 303L, 404L, 505L, 606L, 1,
                Instant.parse("2026-09-01T10:00:00Z"),
                LocalDate.parse("2026-10-01"),
                "Authorized test notes",
                Instant.parse("2026-09-01T10:05:00Z")
        );
        DigitalCertificate cert = new DigitalCertificate(
                1L, "CERT-TEST-12345", "token-xyz-secret",
                Instant.parse("2026-09-01T10:10:00Z"), null,
                606L, 1, Instant.parse("2026-09-01T10:00:00Z")
        );
        CertificateDisplayDetails details =
                new CertificateDisplayDetails(
                        "John Doe",
                        "Pfizer Comirnaty",
                        "BATCH-2026-A",
                        "Metro Vaccination Center"
                );

        byte[] pdf = pdfService.generateCertificatePdf(cert, record, details);

        assertThat(pdf).isNotNull();
        String pdfString = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(pdfString).startsWith("%PDF-1.4");
        assertThat(pdfString).contains("%%EOF");
        assertThat(pdfString).contains("CERT-TEST-12345");
        assertThat(pdfString).contains("John Doe");
        assertThat(pdfString).contains("Pfizer Comirnaty");
        assertThat(pdfString).contains("BATCH-2026-A");
        assertThat(pdfString).contains("Metro Vaccination Center");
        assertThat(pdfString).doesNotContain("Citizen Subject ID:");
        assertThat(pdfString).doesNotContain("Vaccine Batch ID:");
        assertThat(pdfString).doesNotContain("Vaccination Center ID:");
    }
}
