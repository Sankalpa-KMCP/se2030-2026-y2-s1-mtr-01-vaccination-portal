package com.se2030.vaccinationportal.vaccination.administration.strategy;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;

/**
 * Strategy contract for request-type-specific correction creation rules.
 */
public interface CorrectionRequestStrategy {

    boolean supports(VaccinationCorrectionType type);

    CreateVaccinationCorrectionRequest prepareCreate(
            VaccinationRecord record,
            CreateVaccinationCorrectionRequest request,
            Long actorId
    );
}
