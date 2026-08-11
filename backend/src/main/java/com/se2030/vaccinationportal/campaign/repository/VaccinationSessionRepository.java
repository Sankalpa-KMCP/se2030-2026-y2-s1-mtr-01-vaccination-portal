package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for concrete vaccination sessions. */
@Repository
public interface VaccinationSessionRepository extends JpaRepository<VaccinationSession, Long> {

    Optional<VaccinationSession> findByOccurrenceKey(String occurrenceKey);
}
