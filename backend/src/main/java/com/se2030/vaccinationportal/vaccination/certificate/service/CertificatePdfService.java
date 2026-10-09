package com.se2030.vaccinationportal.vaccination.certificate.service;

import com.se2030.vaccinationportal.vaccination.administration.dto.CertificateDisplayDetails;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Server-side PDF generator for official digital vaccination certificates.
 * Generates standards-compliant PDF 1.4 documents formatted as a formal medical vaccination certificate.
 */
@Service
public class CertificatePdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    public byte[] generateCertificatePdf(DigitalCertificate certificate, VaccinationRecord record) {
        return generateCertificatePdf(certificate, record, null);
    }

    public byte[] generateCertificatePdf(
            DigitalCertificate certificate,
            VaccinationRecord record,
            CertificateDisplayDetails details
    ) {
        String ref = escapePdfText(certificate.getCertificateReference());
        String status = certificate.getStatus() != null ? certificate.getStatus().name() : "VALID";
        String dose = "Dose " + (certificate.getIssuedDoseNumber() != null ? certificate.getIssuedDoseNumber() : (record != null ? record.getDoseNumber() : 1));

        String adminTime = "N/A";
        if (certificate.getIssuedAdministrationTime() != null) {
            adminTime = DATE_FORMATTER.format(certificate.getIssuedAdministrationTime());
        } else if (record != null && record.getAdministrationTime() != null) {
            adminTime = DATE_FORMATTER.format(record.getAdministrationTime());
        }

        String issuedAt = certificate.getIssuedAt() != null
                ? DATE_TIME_FORMATTER.format(certificate.getIssuedAt())
                : "N/A";

        String vaccineName = (details != null && details.vaccineName() != null && !details.vaccineName().isBlank())
                ? details.vaccineName()
                : "COVID-19 / Standard Formulation";

        String batchNumber = (details != null && details.batchNumber() != null && !details.batchNumber().isBlank())
                ? details.batchNumber()
                : (certificate.getIssuedVaccineBatchId() != null
                        ? "Batch #" + certificate.getIssuedVaccineBatchId()
                        : (record != null && record.getVaccineBatchId() != null ? "Batch #" + record.getVaccineBatchId() : "Authorized Batch"));

        String recipientName = (details != null && details.citizenName() != null && !details.citizenName().isBlank())
                ? details.citizenName()
                : null;

        String centerName = (details != null && details.centerName() != null && !details.centerName().isBlank())
                ? details.centerName()
                : (record != null && record.getCenterId() != null
                        ? "Vaccination Center #" + record.getCenterId()
                        : "National Vaccination Center");

        String token = escapePdfText(certificate.getVerificationToken() != null ? certificate.getVerificationToken() : "N/A");

        String nextDose = (record != null && record.getNextDoseDate() != null)
                ? DATE_FORMATTER.format(record.getNextDoseDate())
                : null;

        String citizenRef = (record != null && record.getCitizenId() != null)
                ? "Citizen #" + record.getCitizenId()
                : null;

        StringBuilder content = new StringBuilder();

        // 1. Formal double border (Project-safe clean certificate frame)
        content.append("q\n");
        // Outer border
        content.append("0.70 0.75 0.80 RG 1.5 w\n");
        content.append("40 40 515 762 re S\n");
        // Inner security border
        content.append("0.15 0.25 0.38 RG 1 w\n");
        content.append("46 46 503 750 re S\n");
        content.append("Q\n");

        // 2. Header Banner (Clean Navy Portal Banner)
        content.append("q\n");
        content.append("0.10 0.22 0.36 rg\n");
        content.append("47 722 501 73 re f\n");
        content.append("Q\n");

        // Header Text
        content.append("BT\n");
        content.append("/F1 11 Tf 0.85 0.90 0.95 rg\n");
        content.append("58 764 Td (WEB-BASED VACCINATION PORTAL) Tj\n");
        content.append("/F1 18 Tf 1 1 1 rg\n");
        content.append("0 -24 Td (DIGITAL VACCINATION CERTIFICATE) Tj\n");
        content.append("ET\n");

        // 3. Status Banner
        content.append("q\n");
        if ("VALID".equalsIgnoreCase(status)) {
            content.append("0.92 0.97 0.93 rg 0.20 0.55 0.30 RG 1 w\n");
        } else if ("REVOKED".equalsIgnoreCase(status)) {
            content.append("0.98 0.92 0.92 rg 0.75 0.20 0.20 RG 1 w\n");
        } else {
            content.append("0.99 0.96 0.90 rg 0.75 0.50 0.15 RG 1 w\n");
        }
        content.append("50 666 495 40 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 13 Tf\n");
        if ("VALID".equalsIgnoreCase(status)) {
            content.append("0.12 0.45 0.22 rg\n");
            content.append("65 681 Td (STATUS: VALID) Tj\n");
        } else if ("REVOKED".equalsIgnoreCase(status)) {
            content.append("0.70 0.15 0.15 rg\n");
            content.append("65 681 Td (STATUS: REVOKED) Tj\n");
        } else if ("SUPERSEDED".equalsIgnoreCase(status)) {
            content.append("0.65 0.40 0.10 rg\n");
            content.append("65 681 Td (STATUS: SUPERSEDED) Tj\n");
        } else {
            content.append("0.40 0.40 0.40 rg\n");
            content.append("65 681 Td (STATUS: " + escapePdfText(status) + ") Tj\n");
        }
        content.append("ET\n");

        // 4. Section 1: CERTIFICATE HOLDER
        content.append("q\n");
        content.append("0.98 0.98 0.99 rg 0.82 0.85 0.88 RG 1 w\n");
        content.append("50 588 495 64 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 10 Tf 0.10 0.22 0.36 rg\n");
        content.append("65 634 Td (CERTIFICATE HOLDER) Tj\n");

        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg\n");
        content.append("0 -17 Td (Full Name:) Tj\n");
        content.append("/F1 10.5 Tf 0.05 0.10 0.15 rg\n");
        content.append("140 0 Td (" + escapePdfText(recipientName != null ? recipientName : "Registered Citizen") + ") Tj\n");

        if (citizenRef != null) {
            content.append("-140 -15 Td\n");
            content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Citizen Reference:) Tj\n");
            content.append("140 0 Td\n");
            content.append("/F2 9.5 Tf 0.15 0.15 0.15 rg (" + escapePdfText(citizenRef) + ") Tj\n");
        }
        content.append("ET\n");

        // 5. Section 2: VACCINATION DETAILS
        content.append("q\n");
        content.append("0.98 0.98 0.99 rg 0.82 0.85 0.88 RG 1 w\n");
        content.append("50 435 495 139 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 10 Tf 0.10 0.22 0.36 rg\n");
        content.append("65 556 Td (VACCINATION DETAILS) Tj\n");

        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg\n");
        content.append("0 -18 Td (Vaccine Name:) Tj\n");
        content.append("/F2 10 Tf 0.05 0.10 0.15 rg\n");
        content.append("140 0 Td (" + escapePdfText(vaccineName) + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Dose Administered:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F1 10 Tf 0.05 0.10 0.15 rg (" + escapePdfText(dose) + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Administration Date:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F2 10 Tf 0.05 0.10 0.15 rg (" + escapePdfText(adminTime) + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Vaccination Center:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F2 10 Tf 0.05 0.10 0.15 rg (" + escapePdfText(centerName) + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Vaccine Batch / Lot:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F1 10 Tf 0.10 0.15 0.25 rg (" + escapePdfText(batchNumber) + ") Tj\n");

        if (nextDose != null) {
            content.append("-140 -17 Td\n");
            content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Next Recommended Dose:) Tj\n");
            content.append("140 0 Td\n");
            content.append("/F2 10 Tf 0.05 0.10 0.15 rg (" + escapePdfText(nextDose) + ") Tj\n");
        }
        content.append("ET\n");

        // 6. Section 3: CERTIFICATE DETAILS
        content.append("q\n");
        content.append("0.98 0.98 0.99 rg 0.82 0.85 0.88 RG 1 w\n");
        content.append("50 305 495 115 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 10 Tf 0.10 0.22 0.36 rg\n");
        content.append("65 403 Td (CERTIFICATE DETAILS) Tj\n");

        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg\n");
        content.append("0 -18 Td (Certificate Reference:) Tj\n");
        content.append("/F1 10 Tf 0.08 0.22 0.36 rg\n");
        content.append("140 0 Td (" + ref + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Issue Date & Time:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F2 9.5 Tf 0.05 0.10 0.15 rg (" + escapePdfText(issuedAt) + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Certificate Status:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F1 9.5 Tf 0.05 0.10 0.15 rg (" + escapePdfText(status) + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Verification Token:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F2 9 Tf 0.30 0.30 0.30 rg (" + token + ") Tj\n");

        content.append("-140 -17 Td\n");
        content.append("/F1 9.5 Tf 0.30 0.30 0.30 rg (Issuing System:) Tj\n");
        content.append("140 0 Td\n");
        content.append("/F2 9.5 Tf 0.05 0.10 0.15 rg (Web-Based Vaccination Portal) Tj\n");
        content.append("ET\n");

        // 7. Section 4: PUBLIC VERIFICATION
        content.append("q\n");
        content.append("0.96 0.97 0.99 rg 0.75 0.80 0.88 RG 1 w\n");
        content.append("50 155 495 135 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 10 Tf 0.10 0.22 0.36 rg\n");
        content.append("65 272 Td (PUBLIC VERIFICATION) Tj\n");

        content.append("/F2 9 Tf 0.25 0.25 0.25 rg\n");
        content.append("0 -17 Td (This certificate can be verified online through the public portal at /verify-certificate.) Tj\n");
        content.append("0 -14 Td (Enter the Certificate Reference and Verification Token to check certificate status.) Tj\n");
        content.append("0 -14 Td (Public verification displays minimal privacy-masked details in accordance with privacy controls.) Tj\n");

        content.append("/F1 9 Tf 0.10 0.22 0.36 rg\n");
        content.append("0 -20 Td (Authenticity Statement:) Tj\n");
        content.append("/F2 8.5 Tf 0.20 0.20 0.20 rg\n");
        content.append("0 -14 Td (This certificate was generated from a finalized vaccination record maintained by the Web-Based Vaccination Portal.) Tj\n");
        content.append("ET\n");

        // 8. Footer
        content.append("BT\n");
        content.append("/F2 8 Tf 0.45 0.45 0.45 rg\n");
        content.append("140 60 Td (Web-Based Vaccination Portal \\xb7 Digital Vaccination Certificate \\xb7 Page 1 of 1) Tj\n");
        content.append("ET\n");

        byte[] streamBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        return buildPdf(streamBytes);
    }

    private byte[] buildPdf(byte[] contentStream) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Long> offsets = new ArrayList<>();

            // Header
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            out.write(new byte[]{'%', (byte) 0xe2, (byte) 0xe3, (byte) 0xcf, (byte) 0xd3, '\n'});

            // Obj 1: Catalog
            offsets.add((long) out.size());
            out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Obj 2: Pages
            offsets.add((long) out.size());
            out.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Obj 3: Page
            offsets.add((long) out.size());
            out.write(("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> "
                    + "/Contents 6 0 R >>\nendobj\n").getBytes(StandardCharsets.US_ASCII));

            // Obj 4: Font F1 (Helvetica-Bold)
            offsets.add((long) out.size());
            out.write("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Obj 5: Font F2 (Helvetica)
            offsets.add((long) out.size());
            out.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Obj 6: Contents Stream
            offsets.add((long) out.size());
            out.write(("6 0 obj\n<< /Length " + contentStream.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            out.write(contentStream);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // xref table
            long xrefOffset = (long) out.size();
            out.write(("xref\n0 " + (offsets.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
            for (Long offset : offsets) {
                out.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.US_ASCII));
            }

            // Trailer
            out.write(("trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.US_ASCII));
            out.write("startxref\n".getBytes(StandardCharsets.US_ASCII));
            out.write((String.valueOf(xrefOffset) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write("%%EOF\n".getBytes(StandardCharsets.US_ASCII));

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build certificate PDF", e);
        }
    }

    private static String escapePdfText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replaceAll("[^\\x20-\\x7E]", " ");
    }
}
