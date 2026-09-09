package com.se2030.vaccinationportal.vaccination.certificate;

import com.se2030.vaccinationportal.vaccination.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Defines the MF-05 fields that affect the currently supported certificate projection. */
@Component
public class CertificateImpactPolicy {

    public boolean isCertificateImpacting(VaccinationCorrectionField field) {
        return switch (field) {
            case VACCINE_BATCH_ID, DOSE_NUMBER, ADMINISTRATION_TIME -> true;
            case NEXT_DOSE_DATE, AUTHORIZED_NOTES -> false;
        };
    }

    public boolean hasCertificateVisibleChange(
            EffectiveVaccinationRecordSnapshot before,
            EffectiveVaccinationRecordSnapshot after
    ) {
        return !Objects.equals(before.vaccineBatchId(), after.vaccineBatchId())
                || !Objects.equals(before.doseNumber(), after.doseNumber())
                || !Objects.equals(before.administrationTime(), after.administrationTime());
    }
}
