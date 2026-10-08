package com.se2030.vaccinationportal.vaccination.administration.strategy;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Clinical amendment policy for the Vaccinator who administered the vaccination. */
@Component
public class VaccinatorRecordAmendmentStrategy implements CorrectionRequestStrategy {

    private static final Set<VaccinationCorrectionField> ALLOWED_AMENDMENT_FIELDS = EnumSet.of(
            VaccinationCorrectionField.DOSE_NUMBER,
            VaccinationCorrectionField.ADMINISTRATION_TIME,
            VaccinationCorrectionField.NEXT_DOSE_DATE,
            VaccinationCorrectionField.AUTHORIZED_NOTES
    );

    @Override
    public boolean supports(VaccinationCorrectionType type) {
        return type == VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT;
    }

    @Override
    public CreateVaccinationCorrectionRequest prepareCreate(
            VaccinationRecord record,
            CreateVaccinationCorrectionRequest request,
            Long actorId
    ) {
        if (record.getVaccinatorId() == null || !record.getVaccinatorId().equals(actorId)) {
            throw new AccessDeniedException(
                    "Vaccinators may only request amendments for vaccinations they administered"
            );
        }
        if (request == null || request.fieldName() == null || request.fieldName().trim().isEmpty()) {
            throw new InvalidVaccinationCorrectionException("Proposed amendment field name is required");
        }

        String fieldName = request.fieldName().trim();
        VaccinationCorrectionField field = VaccinationCorrectionField.fromApiName(fieldName);
        if (field == VaccinationCorrectionField.VACCINE_BATCH_ID
                || VaccinationCorrectionField.VACCINE_BATCH_ID.name().equalsIgnoreCase(fieldName)) {
            throw new UnsupportedVaccinationCorrectionFieldException(
                    "Vaccine batch corrections are not supported through the vaccination correction workflow because batch changes require inventory reconciliation."
            );
        }
        if (field == null || !ALLOWED_AMENDMENT_FIELDS.contains(field)) {
            throw new UnsupportedVaccinationCorrectionFieldException(
                    "Unsupported vaccination correction field: " + request.fieldName()
            );
        }

        return new CreateVaccinationCorrectionRequest(
                fieldName,
                request.newValue(),
                request.reason(),
                actorId,
                request.category(),
                VaccinationCorrectionType.VACCINATOR_RECORD_AMENDMENT.name()
        );
    }
}
