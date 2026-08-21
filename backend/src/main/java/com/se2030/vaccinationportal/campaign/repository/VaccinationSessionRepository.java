package com.se2030.vaccinationportal.campaign.repository;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM VaccinationSession s WHERE s.id = :id")
    Optional<VaccinationSession> findByIdForUpdate(@Param("id") Long id);

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

    @Query("SELECT s FROM VaccinationSession s WHERE " +
            "(:campaignId IS NULL OR s.campaign.id = :campaignId) AND " +
            "(:centerId IS NULL OR s.center.id = :centerId) AND " +
            "(:status IS NULL OR s.lifecycleStatus = :status) " +
            "ORDER BY s.startAt ASC, s.id ASC")
    List<VaccinationSession> findWithFilters(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") SessionLifecycleStatus status
    );
}
