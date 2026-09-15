package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
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
 * Persistence operations for session resource assignments and temporal overlap validation.
 */
@Repository
public interface SessionResourceAssignmentRepository extends JpaRepository<SessionResourceAssignment, Long> {

    List<SessionResourceAssignment> findBySessionIdOrderByStartAtAscIdAsc(Long sessionId);

    List<SessionResourceAssignment> findByResourceIdOrderByStartAtAscIdAsc(Long resourceId);

    /**
     * Finds overlapping assignments for an operational resource across active sessions using a pessimistic write lock (current read).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SessionResourceAssignment a " +
            "WHERE a.resource.id = :resourceId " +
            "AND (:excludeAssignmentId IS NULL OR a.id <> :excludeAssignmentId) " +
            "AND a.startAt < :endAt " +
            "AND :startAt < a.endAt " +
            "AND a.session.lifecycleStatus <> :cancelledStatus " +
            "ORDER BY a.id ASC")
    List<SessionResourceAssignment> findOverlappingAssignmentsForUpdate(
            @Param("resourceId") Long resourceId,
            @Param("excludeAssignmentId") Long excludeAssignmentId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );

    default List<SessionResourceAssignment> findOverlappingAssignmentsForUpdate(Long resourceId, Long excludeAssignmentId, Instant startAt, Instant endAt) {
        return findOverlappingAssignmentsForUpdate(resourceId, excludeAssignmentId, startAt, endAt, SessionLifecycleStatus.CANCELLED);
    }

    /**
     * Tests half-open interval overlap [startAt, endAt) for active (non-cancelled) session assignments.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM SessionResourceAssignment a " +
            "WHERE a.resource.id = :resourceId " +
            "AND (:excludeAssignmentId IS NULL OR a.id <> :excludeAssignmentId) " +
            "AND a.startAt < :endAt " +
            "AND :startAt < a.endAt " +
            "AND a.session.lifecycleStatus <> :cancelledStatus")
    boolean existsOverlappingAssignment(
            @Param("resourceId") Long resourceId,
            @Param("excludeAssignmentId") Long excludeAssignmentId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );

    default boolean existsOverlappingAssignment(Long resourceId, Long excludeAssignmentId, Instant startAt, Instant endAt) {
        return existsOverlappingAssignment(resourceId, excludeAssignmentId, startAt, endAt, SessionLifecycleStatus.CANCELLED);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SessionResourceAssignment a " +
            "WHERE a.resource.id = :resourceId " +
            "AND a.session.id IN (SELECT s.id FROM VaccinationSession s " +
            "WHERE s.lifecycleStatus IN :statuses AND s.endAt > :now) " +
            "ORDER BY a.id ASC")
    List<SessionResourceAssignment> findBlockingOperationalDependenciesForUpdate(
            @Param("resourceId") Long resourceId,
            @Param("statuses") Collection<SessionLifecycleStatus> statuses,
            @Param("now") Instant now
    );

    /**
     * Count distinct active vaccination station resources assigned to non-cancelled sessions.
     */
    @Query("SELECT COUNT(DISTINCT a.resource.id) FROM SessionResourceAssignment a " +
            "WHERE a.session.lifecycleStatus <> :cancelledStatus " +
            "AND a.resource.resourceType = :stationType " +
            "AND a.resource.status = :activeStatus " +
            "AND (:campaignId IS NULL OR a.session.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR a.session.center.id = :centerId)")
    long countAssignedStations(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("stationType") ResourceType stationType,
            @Param("activeStatus") OperationalResourceStatus activeStatus,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );

    /**
     * Count distinct active resources assigned to non-cancelled sessions grouped by ResourceType.
     */
    @Query("SELECT a.resource.resourceType, COUNT(DISTINCT a.resource.id) " +
            "FROM SessionResourceAssignment a " +
            "WHERE a.session.lifecycleStatus <> :cancelledStatus " +
            "AND a.resource.status = :activeStatus " +
            "AND (:campaignId IS NULL OR a.session.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR a.session.center.id = :centerId) " +
            "GROUP BY a.resource.resourceType")
    List<Object[]> countAssignedResourcesByType(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("activeStatus") OperationalResourceStatus activeStatus,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );
}
