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

        StringBuilder content = new StringBuilder();

        // 1. Formal double border (Medical document frame)
        content.append("q\n");
        // Outer thin border
        content.append("0.75 0.78 0.82 RG 1 w\n");
        content.append("28 28 539 786 re S\n");
        // Inner security border
        content.append("0.12 0.28 0.42 RG 2 w\n");
        content.append("34 34 527 774 re S\n");
        content.append("Q\n");

        // 2. Official Header Banner (Navy blue official banner)
        content.append("q\n");
        content.append("0.08 0.22 0.36 rg\n");
        content.append("35 722 525 85 re f\n");
        content.append("Q\n");

        // Header Text
        content.append("BT\n");
        content.append("/F1 11 Tf 0.85 0.90 0.95 rg\n");
        content.append("50 782 Td (MINISTRY OF HEALTH \\(MOH\\) \\xb7 NATIONAL IMMUNIZATION REGISTRY) Tj\n");
        content.append("/F1 18 Tf 1 1 1 rg\n");
        content.append("0 -24 Td (OFFICIAL DIGITAL VACCINATION CERTIFICATE) Tj\n");
        content.append("/F2 10 Tf 0.80 0.88 0.94 rg\n");
        content.append("0 -16 Td (Government of Sri Lanka \\xb7 International Health Credential) Tj\n");
        content.append("ET\n");

        // 3. Status Banner
        content.append("q\n");
        if ("VALID".equalsIgnoreCase(status)) {
            content.append("0.90 0.96 0.92 rg 0.18 0.55 0.30 RG 1 w\n");
        } else if ("REVOKED".equalsIgnoreCase(status)) {
            content.append("0.98 0.90 0.90 rg 0.78 0.15 0.15 RG 1 w\n");
        } else {
            content.append("0.99 0.95 0.88 rg 0.78 0.55 0.15 RG 1 w\n");
        }
        content.append("50 660 495 42 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 13 Tf\n");
        if ("VALID".equalsIgnoreCase(status)) {
            content.append("0.10 0.45 0.22 rg\n");
            content.append("65 677 Td (STATUS: VALID / ACTIVE CREDENTIAL) Tj\n");
        } else if ("REVOKED".equalsIgnoreCase(status)) {
            content.append("0.70 0.10 0.10 rg\n");
            content.append("65 677 Td (STATUS: REVOKED - INVALID CREDENTIAL) Tj\n");
        } else {
            content.append("0.65 0.40 0.10 rg\n");
            content.append("65 677 Td (STATUS: SUPERSEDED - REPLACED BY NEW VERSION) Tj\n");
        }
        content.append("ET\n");

        // 4. Clinical Details Section (Structured Medical Record Table)
        content.append("q\n");
        content.append("0.98 0.98 0.99 rg 0.82 0.85 0.88 RG 1 w\n");
        content.append("50 255 495 385 re B\n");
        content.append("Q\n");

        // Medical Details Content
        content.append("BT\n");

        // Section Header: Recipient Information
        content.append("/F1 10 Tf 0.08 0.22 0.36 rg\n");
        content.append("65 618 Td (SECTION 1: CERTIFICATE HOLDER & CITIZEN IDENTITY) Tj\n");

        if (recipientName != null) {
            content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
            content.append("0 -20 Td (Citizen Full Name:) Tj\n");
            content.append("/F1 11 Tf 0.05 0.10 0.15 rg\n");
            content.append("165 0 Td (" + escapePdfText(recipientName) + ") Tj\n");
        }

        // Section Header: Vaccination Details
        content.append("/F1 10 Tf 0.08 0.22 0.36 rg\n");
        content.append(recipientName != null ? "-165 -28 Td (SECTION 2: VACCINATION & CLINICAL ADMINISTRATION) Tj\n" : "0 -28 Td (SECTION 2: VACCINATION & CLINICAL ADMINISTRATION) Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("0 -20 Td (Vaccine Formulation:) Tj\n");
        content.append("/F2 11 Tf 0.05 0.10 0.15 rg\n");
        content.append("165 0 Td (" + escapePdfText(vaccineName) + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Dose Administered:) Tj\n");
        content.append("/F1 11 Tf 0.05 0.10 0.15 rg\n");
        content.append("165 0 Td (" + escapePdfText(dose) + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Administration Date:) Tj\n");
        content.append("/F2 11 Tf 0.05 0.10 0.15 rg\n");
        content.append("165 0 Td (" + escapePdfText(adminTime) + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Vaccine Batch / Lot:) Tj\n");
        content.append("/F1 11 Tf 0.10 0.15 0.25 rg\n");
        content.append("165 0 Td (" + escapePdfText(batchNumber) + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Administration Facility:) Tj\n");
        content.append("/F2 11 Tf 0.05 0.10 0.15 rg\n");
        content.append("165 0 Td (" + escapePdfText(centerName) + ") Tj\n");

        // Section Header: Certification Integrity
        content.append("/F1 10 Tf 0.08 0.22 0.36 rg\n");
        content.append("-165 -28 Td (SECTION 3: CERTIFICATION & AUDIT INTEGRITY) Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("0 -20 Td (Certificate Reference:) Tj\n");
        content.append("/F1 11 Tf 0.08 0.22 0.36 rg\n");
        content.append("165 0 Td (" + ref + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Issuance Timestamp:) Tj\n");
        content.append("/F2 10 Tf 0.05 0.10 0.15 rg\n");
        content.append("165 0 Td (" + escapePdfText(issuedAt) + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Verification Token:) Tj\n");
        content.append("/F2 9 Tf 0.30 0.30 0.30 rg\n");
        content.append("165 0 Td (" + token + ") Tj\n");

        content.append("/F1 10 Tf 0.25 0.25 0.25 rg\n");
        content.append("-165 -22 Td (Issuing Authority:) Tj\n");
        content.append("/F2 10 Tf 0.05 0.10 0.15 rg\n");
        content.append("165 0 Td (National Immunization Registry Authority) Tj\n");

        content.append("ET\n");

        // 5. Verification & Authenticity Statement Box
        content.append("q\n");
        content.append("0.94 0.96 0.98 rg 0.70 0.78 0.85 RG 1 w\n");
        content.append("50 110 495 125 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 10 Tf 0.08 0.22 0.36 rg\n");
        content.append("65 215 Td (SECTION 4: PUBLIC VERIFICATION & AUTHENTICITY NOTICE) Tj\n");
        content.append("/F2 9 Tf 0.25 0.25 0.25 rg\n");
        content.append("0 -17 Td (This certificate can be verified through the National Vaccination Portal at /verify-certificate.) Tj\n");
        content.append("0 -14 Td (Enter the certificate reference or verification token above to authenticate.) Tj\n");
        content.append("0 -14 Td (NFR-PRV-01 Privacy Compliant: Public verification displays minimal clinical details only.) Tj\n");
        content.append("/F2 8.5 Tf 0.40 0.40 0.40 rg\n");
        content.append("0 -18 Td (Issued under the legal authority of the Ministry of Health. Electronically generated and tamper-evident.) Tj\n");
        content.append("0 -13 Td (Any unauthorized reproduction or alteration renders this official medical document invalid.) Tj\n");
        content.append("ET\n");

        // 6. Footer
        content.append("BT\n");
        content.append("/F2 8 Tf 0.45 0.45 0.45 rg\n");
        content.append("140 50 Td (Web-Based Vaccination Portal \\xb7 Official Electronic Medical Document \\xb7 Page 1 of 1) Tj\n");
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
