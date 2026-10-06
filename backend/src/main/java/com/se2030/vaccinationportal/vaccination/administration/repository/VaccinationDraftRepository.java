package com.se2030.vaccinationportal.vaccination.administration.repository;

import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for editable vaccination drafts.
 */
@Repository
public interface VaccinationDraftRepository extends JpaRepository<VaccinationDraft, Long> {

    Optional<VaccinationDraft> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<VaccinationDraft> findAllByVaccinatorIdOrderByCreatedAtDesc(Long vaccinatorId);

    Optional<VaccinationDraft> findByIdAndVaccinatorId(Long id, Long vaccinatorId);
}
