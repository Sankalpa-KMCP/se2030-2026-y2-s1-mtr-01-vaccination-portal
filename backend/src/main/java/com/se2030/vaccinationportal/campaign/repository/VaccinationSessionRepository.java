package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence operations for concrete vaccination sessions. */
@Repository
public interface VaccinationSessionRepository extends JpaRepository<VaccinationSession, Long> {

    Optional<VaccinationSession> findByOccurrenceKey(String occurrenceKey);

    List<VaccinationSession> findByCampaignId(Long campaignId);

    List<VaccinationSession> findByCenterId(Long centerId);

    List<VaccinationSession> findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(Long recurrenceRuleId);

    boolean existsByRecurrenceRuleId(Long recurrenceRuleId);

    boolean existsByRecurrenceRuleIdAndOriginOccurrenceDateAfter(Long recurrenceRuleId, LocalDate date);

    @Query(value = "SELECT campaign_id FROM vaccination_sessions WHERE id = :id", nativeQuery = true)
    Optional<Long> findCampaignIdBySessionId(@Param("id") Long id);

    @Query(value = "SELECT center_id FROM vaccination_sessions WHERE id = :id", nativeQuery = true)
    Optional<Long> findCenterIdBySessionId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM VaccinationSession s WHERE s.id = :id")
    Optional<VaccinationSession> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM VaccinationSession s " +
            "WHERE s.center.id = :centerId AND s.lifecycleStatus IN :statuses " +
            "ORDER BY s.id ASC")
    List<VaccinationSession> findOperationalSessionsForCenterForUpdate(
            @Param("centerId") Long centerId,
            @Param("statuses") Collection<SessionLifecycleStatus> statuses
    );

    @Query("SELECT s FROM VaccinationSession s WHERE s.campaign.id = :campaignId " +
            "AND s.lifecycleStatus IN :statuses AND s.endAt > :now")
    List<VaccinationSession> findBlockingSessionsForCampaign(
            @Param("campaignId") Long campaignId,
            @Param("statuses") Collection<SessionLifecycleStatus> statuses,
            @Param("now") Instant now
    );

    @Query("SELECT s FROM VaccinationSession s WHERE s.campaign.id = :campaignId " +
            "AND s.center.id = :centerId AND s.lifecycleStatus IN :statuses AND s.endAt > :now")
    List<VaccinationSession> findBlockingSessionsForCampaignAndCenter(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("statuses") Collection<SessionLifecycleStatus> statuses,
            @Param("now") Instant now
    );

    @Query("SELECT s FROM VaccinationSession s WHERE s.center.id = :centerId " +
            "AND s.lifecycleStatus IN :statuses AND s.endAt > :now")
    List<VaccinationSession> findBlockingSessionsForCenter(
            @Param("centerId") Long centerId,
            @Param("statuses") Collection<SessionLifecycleStatus> statuses,
            @Param("now") Instant now
    );

    @Query("SELECT s FROM VaccinationSession s " +
            "JOIN FETCH s.campaign c " +
            "JOIN FETCH s.center cnt " +
            "LEFT JOIN FETCH s.recurrenceRule rr " +
            "WHERE s.id = :id")
    Optional<VaccinationSession> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM VaccinationSession s " +
            "JOIN FETCH s.campaign c " +
            "JOIN FETCH s.center cnt " +
            "LEFT JOIN FETCH s.recurrenceRule rr " +
            "WHERE " +
            "(:campaignId IS NULL OR c.id = :campaignId) AND " +
            "(:centerId IS NULL OR cnt.id = :centerId) AND " +
            "(:status IS NULL OR s.lifecycleStatus = :status) " +
            "ORDER BY s.startAt ASC, s.id ASC")
    List<VaccinationSession> findWithFilters(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") SessionLifecycleStatus status
    );

    /**
     * Aggregate session count grouped by lifecycle status, optionally filtered by campaignId and centerId.
     */
    @Query("SELECT s.lifecycleStatus, COUNT(s) FROM VaccinationSession s " +
            "WHERE (:campaignId IS NULL OR s.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR s.center.id = :centerId) " +
            "GROUP BY s.lifecycleStatus")
    List<Object[]> countSessionsByLifecycleStatus(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId
    );

    /**
     * Count cancelled sessions, optionally filtered by campaignId and centerId.
     */
    @Query("SELECT COUNT(s) FROM VaccinationSession s " +
            "WHERE s.lifecycleStatus = :cancelledStatus " +
            "AND (:campaignId IS NULL OR s.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR s.center.id = :centerId)")
    long countCancelledSessions(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("cancelledStatus") SessionLifecycleStatus cancelledStatus
    );

    /**
     * Aggregate distinct campaign count grouped by campaign status represented by sessions at a specific center, optionally filtered by campaignId.
     */
    @Query("SELECT s.campaign.status, COUNT(DISTINCT s.campaign.id) FROM VaccinationSession s " +
            "WHERE s.center.id = :centerId " +
            "AND (:campaignId IS NULL OR s.campaign.id = :campaignId) " +
            "GROUP BY s.campaign.status")
    List<Object[]> countDistinctCampaignsByStatusForCenter(
            @Param("centerId") Long centerId,
            @Param("campaignId") Long campaignId
    );

    /**
     * Count distinct cancelled campaigns represented by sessions at a specific center, optionally filtered by campaignId.
     */
    @Query("SELECT COUNT(DISTINCT s.campaign.id) FROM VaccinationSession s " +
            "WHERE s.center.id = :centerId " +
            "AND s.campaign.status = :cancelledStatus " +
            "AND (:campaignId IS NULL OR s.campaign.id = :campaignId)")
    long countDistinctCancelledCampaignsForCenter(
            @Param("centerId") Long centerId,
            @Param("campaignId") Long campaignId,
            @Param("cancelledStatus") CampaignStatus cancelledStatus
    );
}
