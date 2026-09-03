package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/** Persistence operations for bounded recurring-session definitions. */
@Repository
public interface SessionRecurrenceRuleRepository extends JpaRepository<SessionRecurrenceRule, Long> {

    Optional<SessionRecurrenceRule> findByGenerationKey(String generationKey);

    List<SessionRecurrenceRule> findByCampaignIdOrderByStartDateAscStartTimeAscIdAsc(Long campaignId);

    List<SessionRecurrenceRule> findByCenterIdOrderByStartDateAscStartTimeAscIdAsc(Long centerId);

    @Query("SELECT DISTINCT r FROM SessionRecurrenceRule r JOIN r.weekdays weekday " +
            "WHERE r.campaign.id = :campaignId AND r.center.id = :centerId " +
            "AND r.status = :status " +
            "AND r.startDate <= :date AND :date <= r.endDate " +
            "AND weekday = :dayOfWeek " +
            "ORDER BY r.startTime, r.endTime, r.id")
    List<SessionRecurrenceRule> findActiveRulesForCampaignCenterOn(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") SessionRecurrenceRuleStatus status,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SessionRecurrenceRule r WHERE r.id = :id")
    Optional<SessionRecurrenceRule> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM SessionRecurrenceRule r " +
            "WHERE (:campaignId IS NULL OR r.campaign.id = :campaignId) " +
            "AND (:centerId IS NULL OR r.center.id = :centerId) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.startDate ASC, r.startTime ASC, r.id ASC")
    List<SessionRecurrenceRule> findWithFilters(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") SessionRecurrenceRuleStatus status
    );
}
