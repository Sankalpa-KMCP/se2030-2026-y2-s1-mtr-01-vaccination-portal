package com.se2030.vaccinationportal.vaccination.administration.repository;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for append-only vaccination correction history. */
@Repository
public interface VaccinationCorrectionRepository extends JpaRepository<VaccinationCorrection, Long> {

    List<VaccinationCorrection> findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(Long vaccinationRecordId);

    Optional<VaccinationCorrection> findByIdAndVaccinationRecordId(Long id, Long vaccinationRecordId);
}
