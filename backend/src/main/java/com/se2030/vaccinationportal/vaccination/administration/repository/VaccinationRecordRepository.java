package com.se2030.vaccinationportal.vaccination.administration.repository;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence operations for permanent vaccination history.
 */
@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM VaccinationRecord r WHERE r.id = :id")
    Optional<VaccinationRecord> findByIdForUpdate(@Param("id") Long id);

    Optional<VaccinationRecord> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<VaccinationRecord> findByCitizenIdOrderByAdministrationTimeDesc(Long citizenId);

    List<VaccinationRecord> findByVaccinatorIdOrderByAdministrationTimeDesc(Long vaccinatorId);
}
