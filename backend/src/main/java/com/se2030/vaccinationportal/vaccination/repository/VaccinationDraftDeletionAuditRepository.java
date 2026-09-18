package com.se2030.vaccinationportal.vaccination.repository;

import com.se2030.vaccinationportal.vaccination.model.VaccinationDraftDeletionAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for append-only vaccination draft deletion audit records.
 */
@Repository
public interface VaccinationDraftDeletionAuditRepository extends JpaRepository<VaccinationDraftDeletionAudit, Long> {

    List<VaccinationDraftDeletionAudit> findByDraftIdOrderByDeletedAtAsc(Long draftId);

    List<VaccinationDraftDeletionAudit> findByAppointmentIdOrderByDeletedAtAsc(Long appointmentId);
}
