package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * Loads slots with their session, campaign, and center for bounded citizen appointment summaries.
     */
    @EntityGraph(attributePaths = {"session", "session.campaign", "session.center"})
    List<TimeSlot> findByIdIn(Collection<Long> ids);

    @Query(value = "SELECT session_id FROM time_slots WHERE id = :id", nativeQuery = true)
    Optional<Long> findSessionIdBySlotId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.session.id = :sessionId ORDER BY ts.id ASC")
    List<TimeSlot> findBySessionIdForUpdateOrderByIdAsc(@Param("sessionId") Long sessionId);

    List<TimeSlot> findBySessionIdOrderByStartTimeAsc(Long sessionId);

    List<TimeSlot> findBySessionId(Long sessionId);

    @Query("SELECT SUM(ts.capacity) FROM TimeSlot ts WHERE ts.session.id = :sessionId")
    Integer sumCapacityBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT SUM(ts.reservedCount) FROM TimeSlot ts WHERE ts.session.id = :sessionId")
    Integer sumReservedCountBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT new com.se2030.vaccinationportal.campaign.repository.SessionSlotCapacitySummary(" +
            "ts.session.id, COUNT(ts), COALESCE(SUM(ts.capacity), 0), COALESCE(SUM(ts.reservedCount), 0)) " +
            "FROM TimeSlot ts WHERE ts.session.id = :sessionId GROUP BY ts.session.id")
    Optional<SessionSlotCapacitySummary> summarizeCapacityBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT new com.se2030.vaccinationportal.campaign.repository.SessionSlotCapacitySummary(" +
            "ts.session.id, COUNT(ts), COALESCE(SUM(ts.capacity), 0), COALESCE(SUM(ts.reservedCount), 0)) " +
            "FROM TimeSlot ts WHERE ts.session.id IN :sessionIds GROUP BY ts.session.id")
    List<SessionSlotCapacitySummary> summarizeCapacityBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    void deleteBySessionId(Long sessionId);

    /**
     * Sums total configured capacity and total reserved count across time slots of non-cancelled sessions.
     */
    @Query("SELECT COALESCE(SUM(ts.capacity), 0), COALESCE(SUM(ts.reservedCount), 0) " +
            "FROM TimeSlot ts " +
            "WHERE ts.session.lifecycleStatus <> :cancelledStatus " +
            "AND (:campaignId IS NULL OR ts.session.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR ts.session.center.id = :centerId)")
    List<Object[]> sumCapacityAndReservedCount(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );
}
