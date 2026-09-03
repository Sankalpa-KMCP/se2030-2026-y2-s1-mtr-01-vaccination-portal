package com.se2030.vaccinationportal.vaccination.repository;

import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for permanent vaccination history.
 */
@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {

    Optional<VaccinationRecord> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<VaccinationRecord> findByCitizenIdOrderByAdministrationTimeDesc(Long citizenId);
}
