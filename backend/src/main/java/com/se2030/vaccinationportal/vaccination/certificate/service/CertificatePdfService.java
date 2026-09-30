package com.se2030.vaccinationportal.vaccination.certificate.service;

import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Clean, lightweight server-side PDF generator for official digital vaccination certificates.
 * Generates standards-compliant PDF 1.4 documents without requiring external third-party dependencies.
 */
@Service
public class CertificatePdfService {

    public byte[] generateCertificatePdf(DigitalCertificate certificate, VaccinationRecord record) {
        String ref = escapePdfText(certificate.getCertificateReference());
        String status = certificate.getStatus() != null ? certificate.getStatus().name() : "VALID";
        String dose = "Dose " + (certificate.getIssuedDoseNumber() != null ? certificate.getIssuedDoseNumber() : (record != null ? record.getDoseNumber() : 1));
        
        String adminTime = "N/A";
        if (certificate.getIssuedAdministrationTime() != null) {
            adminTime = DateTimeFormatter.ISO_INSTANT.format(certificate.getIssuedAdministrationTime());
        } else if (record != null && record.getAdministrationTime() != null) {
            adminTime = DateTimeFormatter.ISO_INSTANT.format(record.getAdministrationTime());
        }
        
        String issuedAt = certificate.getIssuedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(certificate.getIssuedAt())
                : "N/A";
        
        String citizenId = record != null && record.getCitizenId() != null
                ? String.valueOf(record.getCitizenId())
                : "N/A";
        String batchId = certificate.getIssuedVaccineBatchId() != null
                ? String.valueOf(certificate.getIssuedVaccineBatchId())
                : (record != null && record.getVaccineBatchId() != null ? String.valueOf(record.getVaccineBatchId()) : "N/A");
        String centerId = record != null && record.getCenterId() != null
                ? String.valueOf(record.getCenterId())
                : "N/A";
        String token = escapePdfText(certificate.getVerificationToken() != null ? certificate.getVerificationToken() : "N/A");

        StringBuilder content = new StringBuilder();

        // Background / Border
        content.append("q\n");
        content.append("0.85 0.85 0.85 RG 1.5 w\n");
        content.append("30 30 535 782 re S\n");
        content.append("0.06 0.43 0.36 RG 3 w\n");
        content.append("36 36 523 770 re S\n");
        content.append("Q\n");

        // Header Banner (dark teal)
        content.append("q\n");
        content.append("0.06 0.43 0.36 rg\n");
        content.append("36 716 523 90 re f\n");
        content.append("Q\n");

        // Header Text
        content.append("BT\n");
        content.append("/F1 20 Tf 1 1 1 rg\n");
        content.append("60 766 Td (NATIONAL VACCINATION PORTAL) Tj\n");
        content.append("/F2 12 Tf 0.9 0.95 0.93 rg\n");
        content.append("0 -22 Td (OFFICIAL DIGITAL VACCINATION CERTIFICATE) Tj\n");
        content.append("ET\n");

        // Status Banner
        content.append("q\n");
        if ("VALID".equalsIgnoreCase(status)) {
            content.append("0.85 0.95 0.88 rg 0.18 0.55 0.34 RG 1 w\n");
        } else if ("REVOKED".equalsIgnoreCase(status)) {
            content.append("0.98 0.88 0.88 rg 0.78 0.15 0.15 RG 1 w\n");
        } else {
            content.append("0.99 0.95 0.85 rg 0.78 0.55 0.15 RG 1 w\n");
        }
        content.append("50 645 495 45 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 14 Tf\n");
        if ("VALID".equalsIgnoreCase(status)) {
            content.append("0.1 0.45 0.25 rg\n");
            content.append("65 662 Td (STATUS: VALID / ACTIVE CREDENTIAL) Tj\n");
        } else if ("REVOKED".equalsIgnoreCase(status)) {
            content.append("0.7 0.1 0.1 rg\n");
            content.append("65 662 Td (STATUS: REVOKED - INVALID CREDENTIAL) Tj\n");
        } else {
            content.append("0.65 0.4 0.1 rg\n");
            content.append("65 662 Td (STATUS: SUPERSEDED - REPLACED BY NEW VERSION) Tj\n");
        }
        content.append("ET\n");

        // Certificate Details Box
        content.append("q\n");
        content.append("0.96 0.97 0.98 rg 0.8 0.85 0.9 RG 1 w\n");
        content.append("50 240 495 380 re B\n");
        content.append("Q\n");

        // Details content
        content.append("BT\n");
        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("70 585 Td (Certificate Reference:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + ref + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Citizen Subject ID:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + citizenId + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Vaccine Dose:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + dose + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Administration Date:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + adminTime + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Vaccine Batch ID:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + batchId + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Vaccination Center ID:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + centerId + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Issue Timestamp:) Tj\n");
        content.append("/F2 11 Tf 0.1 0.1 0.1 rg\n");
        content.append("150 0 Td (" + issuedAt + ") Tj\n");

        content.append("/F1 11 Tf 0.2 0.2 0.2 rg\n");
        content.append("-150 -32 Td (Verification Token:) Tj\n");
        content.append("/F2 9 Tf 0.3 0.3 0.3 rg\n");
        content.append("150 0 Td (" + token + ") Tj\n");
        content.append("ET\n");

        // Verification & Security Note Box
        content.append("q\n");
        content.append("0.93 0.95 0.94 rg 0.7 0.8 0.75 RG 1 w\n");
        content.append("50 100 495 110 re B\n");
        content.append("Q\n");

        content.append("BT\n");
        content.append("/F1 10 Tf 0.06 0.43 0.36 rg\n");
        content.append("65 180 Td (PUBLIC VERIFICATION AND AUTHENTICITY) Tj\n");
        content.append("/F2 9 Tf 0.3 0.3 0.3 rg\n");
        content.append("0 -18 Td (This official credential can be publicly verified via the National Portal at /verify-certificate.) Tj\n");
        content.append("0 -14 Td (Enter the certificate reference or verification token above to authenticate.) Tj\n");
        content.append("0 -14 Td (NFR-PRV-01 Privacy Compliant: Public verification displays minimal clinical details only.) Tj\n");
        content.append("/F2 8 Tf 0.5 0.5 0.5 rg\n");
        content.append("0 -20 Td (Issued under the authority of the Ministry of Health. Cryptographically generated.) Tj\n");
        content.append("ET\n");

        // Footer
        content.append("BT\n");
        content.append("/F2 8 Tf 0.5 0.5 0.5 rg\n");
        content.append("200 50 Td (Web-Based Vaccination Portal - Official Electronic Document) Tj\n");
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
