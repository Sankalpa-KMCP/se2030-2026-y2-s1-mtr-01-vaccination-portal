package com.se2030.vaccinationportal.vaccination.administration.repository;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence operations for append-only vaccination correction history. */
@Repository
public interface VaccinationCorrectionRepository extends JpaRepository<VaccinationCorrection, Long> {

    List<VaccinationCorrection> findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(Long vaccinationRecordId);

    Optional<VaccinationCorrection> findByIdAndVaccinationRecordId(Long id, Long vaccinationRecordId);

    List<VaccinationCorrection> findAllByOrderByCorrectedAtDescIdDesc();

    @Query("SELECT c FROM VaccinationCorrection c WHERE c.vaccinationRecordId IN (SELECT r.id FROM VaccinationRecord r WHERE r.vaccinatorId = :vaccinatorId) ORDER BY c.correctedAt DESC, c.id DESC")
    List<VaccinationCorrection> findByVaccinatorIdOrderByCorrectedAtDescIdDesc(@Param("vaccinatorId") Long vaccinatorId);

    @Query("SELECT c FROM VaccinationCorrection c WHERE c.vaccinationRecordId IN (SELECT r.id FROM VaccinationRecord r WHERE r.citizenId = :citizenId) ORDER BY c.correctedAt DESC, c.id DESC")
    List<VaccinationCorrection> findByCitizenIdOrderByCorrectedAtDescIdDesc(@Param("citizenId") Long citizenId);

    @Query("SELECT COUNT(c) > 0 FROM VaccinationCorrection c WHERE c.vaccinationRecordId = :recordId AND c.status IN (com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus.PENDING, com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus.APPROVED)")
    boolean existsActiveByVaccinationRecordId(@Param("recordId") Long recordId);
}
