package com.se2030.vaccinationportal.vaccination.repository;

import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for append-only vaccination correction history. */
@Repository
public interface VaccinationCorrectionRepository extends JpaRepository<VaccinationCorrection, Long> {

    List<VaccinationCorrection> findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(Long vaccinationRecordId);
}
