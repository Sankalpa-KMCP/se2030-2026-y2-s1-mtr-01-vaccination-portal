package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for session staff assignments and temporal overlap validation.
 */
@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    List<StaffAssignment> findBySessionIdOrderByStartAtAscIdAsc(Long sessionId);

    List<StaffAssignment> findByUserIdOrderByStartAtAscIdAsc(Long userId);

    List<StaffAssignment> findByStaffRosterIdOrderByStartAtAscIdAsc(Long staffRosterId);

    /**
     * Tests half-open interval overlap [startAt, endAt) for a staff user across active sessions.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM StaffAssignment a " +
            "WHERE a.userId = :userId " +
            "AND (:excludeAssignmentId IS NULL OR a.id <> :excludeAssignmentId) " +
            "AND a.startAt < :endAt " +
            "AND :startAt < a.endAt " +
            "AND a.session.lifecycleStatus <> :cancelledStatus")
    boolean existsOverlappingAssignment(
            @Param("userId") Long userId,
            @Param("excludeAssignmentId") Long excludeAssignmentId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );

    default boolean existsOverlappingAssignment(Long userId, Long excludeAssignmentId, Instant startAt, Instant endAt) {
        return existsOverlappingAssignment(userId, excludeAssignmentId, startAt, endAt, SessionLifecycleStatus.CANCELLED);
    }
}
