package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 *
 * Persistence operations for session staff assignments and temporal overlap validation.
 */
@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    List<StaffAssignment> findBySessionIdOrderByStartAtAscIdAsc(Long sessionId);

    List<StaffAssignment> findByUserIdOrderByStartAtAscIdAsc(Long userId);

    List<StaffAssignment> findByStaffRosterIdOrderByStartAtAscIdAsc(Long staffRosterId);

    /**
     * Finds overlapping assignments for a staff user across active sessions using a pessimistic write lock (current read).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM StaffAssignment a " +
            "WHERE a.userId = :userId " +
            "AND (:excludeAssignmentId IS NULL OR a.id <> :excludeAssignmentId) " +
            "AND a.startAt < :endAt " +
            "AND :startAt < a.endAt " +
            "AND a.session.lifecycleStatus <> :cancelledStatus " +
            "ORDER BY a.id ASC")
    List<StaffAssignment> findOverlappingAssignmentsForUpdate(
            @Param("userId") Long userId,
            @Param("excludeAssignmentId") Long excludeAssignmentId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );

    default List<StaffAssignment> findOverlappingAssignmentsForUpdate(Long userId, Long excludeAssignmentId, Instant startAt, Instant endAt) {
        return findOverlappingAssignmentsForUpdate(userId, excludeAssignmentId, startAt, endAt, SessionLifecycleStatus.CANCELLED);
    }

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM StaffAssignment a " +
            "WHERE a.staffRoster.id = :staffRosterId " +
            "AND a.session.id IN (SELECT s.id FROM VaccinationSession s " +
            "WHERE s.lifecycleStatus IN :statuses AND s.endAt > :now) " +
            "ORDER BY a.id ASC")
    List<StaffAssignment> findBlockingOperationalDependenciesForUpdate(
            @Param("staffRosterId") Long staffRosterId,
            @Param("statuses") Collection<SessionLifecycleStatus> statuses,
            @Param("now") Instant now
    );

    /**
     * Count distinct active vaccinators assigned to non-cancelled sessions.
     */
    @Query("SELECT COUNT(DISTINCT a.userId) FROM StaffAssignment a " +
            "WHERE a.session.lifecycleStatus <> :cancelledStatus " +
            "AND a.role = :vaccinatorRole " +
            "AND a.staffRoster.status = :activeStatus " +
            "AND (:campaignId IS NULL OR a.session.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR a.session.center.id = :centerId)")
    long countAssignedVaccinators(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("vaccinatorRole") StaffRole vaccinatorRole,
            @Param("activeStatus") OperationalStaffStatus activeStatus,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );
}
