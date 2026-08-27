package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for session capacity plans and revision history.
 */
@Repository
public interface SessionCapacityPlanRepository extends JpaRepository<SessionCapacityPlan, Long> {

    Optional<SessionCapacityPlan> findTopBySessionIdOrderByRevisionDesc(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SessionCapacityPlan> findFirstBySessionIdOrderByRevisionDesc(Long sessionId);

    List<SessionCapacityPlan> findBySessionIdOrderByRevisionAsc(Long sessionId);

    Optional<SessionCapacityPlan> findBySessionIdAndRevision(Long sessionId, int revision);

    boolean existsBySessionIdAndRevision(Long sessionId, int revision);
}
