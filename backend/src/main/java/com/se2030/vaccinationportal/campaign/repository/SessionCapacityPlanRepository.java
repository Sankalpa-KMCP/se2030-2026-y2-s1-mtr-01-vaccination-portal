package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Sums approved capacity and recommended throughput for the latest revision of each non-cancelled session.
     */
    @Query("SELECT COALESCE(SUM(p.approvedCapacity), 0), COALESCE(SUM(p.recommendedThroughput), 0) " +
            "FROM SessionCapacityPlan p " +
            "WHERE p.session.lifecycleStatus <> :cancelledStatus " +
            "AND (:campaignId IS NULL OR p.session.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR p.session.center.id = :centerId) " +
            "AND p.revision = (" +
            "    SELECT MAX(p2.revision) FROM SessionCapacityPlan p2 WHERE p2.session.id = p.session.id" +
            ")")
    List<Object[]> sumLatestApprovedCapacityAndRecommendedThroughput(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );
}
