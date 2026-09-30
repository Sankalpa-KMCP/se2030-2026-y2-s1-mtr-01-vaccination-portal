package com.se2030.vaccinationportal.vaccination.query.service;

import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationCorrectionValueCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves immutable vaccination history with deterministic applied-correction overlays. */
@Service
@Transactional(readOnly = true)
public class EffectiveVaccinationRecordService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final VaccinationCorrectionRepository correctionRepository;
    private final VaccinationCorrectionValueCodec valueCodec;

    public EffectiveVaccinationRecordService(
            VaccinationRecordRepository vaccinationRecordRepository,
            VaccinationCorrectionRepository correctionRepository,
            VaccinationCorrectionValueCodec valueCodec
    ) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.correctionRepository = correctionRepository;
        this.valueCodec = valueCodec;
    }

    public EffectiveVaccinationRecordSnapshot getEffectiveRecord(Long vaccinationRecordId) {
        VaccinationRecord record = vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
        return resolve(record);
    }

    /** Resolves an already loaded record for other Vaccination capabilities without another record query. */
    public EffectiveVaccinationRecordSnapshot resolve(VaccinationRecord record) {
        EffectiveVaccinationRecordSnapshot effective = EffectiveVaccinationRecordSnapshot.from(record);
        var corrections = correctionRepository
                .findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(record.getId());
        for (var correction : corrections) {
            if (correction.getStatus() == VaccinationCorrectionStatus.APPLIED) {
                effective = valueCodec.apply(
                        effective,
                        correction.getFieldName(),
                        correction.getNewValue()
                );
            }
        }
        return effective;
    }
}
