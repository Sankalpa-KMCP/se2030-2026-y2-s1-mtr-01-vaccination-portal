package com.se2030.vaccinationportal.vaccination.administration.mapper;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionResponse;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import java.time.Instant;

/** Pure transformations for vaccination correction creation and API responses. */
public final class VaccinationCorrectionMapper {

    private VaccinationCorrectionMapper() {
    }

    public static VaccinationCorrection toEntity(
            Long vaccinationRecordId,
            VaccinationCorrectionField fieldName,
            String oldValue,
            String normalizedNewValue,
            CreateVaccinationCorrectionRequest request,
            Instant correctedAt
    ) {
        return new VaccinationCorrection(
                vaccinationRecordId,
                fieldName,
                oldValue,
                normalizedNewValue,
                request.reason(),
                request.correctedBy(),
                correctedAt
        );
    }

    public static VaccinationCorrectionResponse toResponse(VaccinationCorrection correction) {
        if (correction == null) {
            return null;
        }
        return new VaccinationCorrectionResponse(
                correction.getId(),
                correction.getVaccinationRecordId(),
                correction.getFieldName().getApiName(),
                correction.getOldValue(),
                correction.getNewValue(),
                correction.getReason(),
                correction.getCorrectedBy(),
                correction.getCorrectedAt(),
                correction.getStatus(),
                correction.getRequestType()
        );
    }
}
