package com.se2030.vaccinationportal.vaccination.dto;

/** Safe result of applying a correction and evaluating its certificate lifecycle impact. */
public record VaccinationCorrectionLifecycleResponse(
        VaccinationCorrectionResponse correction,
        boolean certificateImpacted,
        boolean certificateReissued,
        CertificateReissueResponse certificateReissue
) {
}
