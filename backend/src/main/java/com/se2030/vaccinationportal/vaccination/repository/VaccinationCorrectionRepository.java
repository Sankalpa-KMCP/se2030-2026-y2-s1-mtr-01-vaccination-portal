package com.se2030.vaccinationportal.vaccination.repository;

import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for append-only vaccination correction history. */
@Repository
public interface VaccinationCorrectionRepository extends JpaRepository<VaccinationCorrection, Long> {

    List<VaccinationCorrection> findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(Long vaccinationRecordId);

    Optional<VaccinationCorrection> findFirstByVaccinationRecordIdAndFieldNameOrderByCorrectedAtDescIdDesc(
            Long vaccinationRecordId,
            VaccinationCorrectionField fieldName
    );
}
