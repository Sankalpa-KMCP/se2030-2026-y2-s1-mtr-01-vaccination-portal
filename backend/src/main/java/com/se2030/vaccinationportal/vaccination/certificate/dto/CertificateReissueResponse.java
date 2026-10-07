package com.se2030.vaccinationportal.vaccination.certificate.dto;

/** Safe representation of both sides of an atomic certificate reissue. */
public record CertificateReissueResponse(
        DigitalCertificateResponse supersededCertificate,
        DigitalCertificateResponse issuedCertificate
) {
}
