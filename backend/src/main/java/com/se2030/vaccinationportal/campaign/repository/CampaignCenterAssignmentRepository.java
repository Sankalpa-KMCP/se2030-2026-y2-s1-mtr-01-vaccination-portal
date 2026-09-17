package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence operations for retained campaign-center assignment history. */
@Repository
public interface CampaignCenterAssignmentRepository
        extends JpaRepository<CampaignCenterAssignment, Long> {

    @Query("SELECT a.campaign.id FROM CampaignCenterAssignment a WHERE a.id = :id")
    Optional<Long> findCampaignIdById(@Param("id") Long id);

    @Query("SELECT a FROM CampaignCenterAssignment a " +
            "JOIN FETCH a.center " +
            "JOIN FETCH a.campaign " +
            "WHERE a.campaign.id = :campaignId " +
            "ORDER BY a.activeFrom, CASE WHEN a.activeTo IS NULL THEN 1 ELSE 0 END, a.activeTo, a.id")
    List<CampaignCenterAssignment> findCampaignHistory(@Param("campaignId") Long campaignId);

    @Query("SELECT a FROM CampaignCenterAssignment a " +
            "JOIN FETCH a.center " +
            "JOIN FETCH a.campaign " +
            "WHERE a.center.id = :centerId " +
            "ORDER BY a.activeFrom, CASE WHEN a.activeTo IS NULL THEN 1 ELSE 0 END, a.activeTo, a.id")
    List<CampaignCenterAssignment> findCenterHistory(@Param("centerId") Long centerId);

    @Query("SELECT a FROM CampaignCenterAssignment a " +
            "JOIN FETCH a.center " +
            "JOIN FETCH a.campaign " +
            "WHERE a.campaign.id = :campaignId AND a.center.id = :centerId " +
            "AND a.status = :status " +
            "AND a.activeFrom <= :date " +
            "AND (a.activeTo IS NULL OR :date < a.activeTo)")
    Optional<CampaignCenterAssignment> findEffectiveAssignmentAt(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") CampaignCenterAssignmentStatus status,
            @Param("date") LocalDate date
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM CampaignCenterAssignment a " +
            "WHERE a.campaign.id = :campaignId AND a.center.id = :centerId " +
            "AND a.status = :status " +
            "AND (:excludeId IS NULL OR a.id <> :excludeId) " +
            "AND (:activeTo IS NULL OR a.activeFrom < :activeTo) " +
            "AND (a.activeTo IS NULL OR :activeFrom < a.activeTo)")
    boolean existsOverlappingAssignment(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") CampaignCenterAssignmentStatus status,
            @Param("excludeId") Long excludeId,
            @Param("activeFrom") LocalDate activeFrom,
            @Param("activeTo") LocalDate activeTo
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM CampaignCenterAssignment a " +
            "WHERE a.campaign.id = :campaignId AND a.center.id = :centerId " +
            "AND a.status = :status " +
            "AND (:excludeId IS NULL OR a.id <> :excludeId) " +
            "AND a.activeFrom <= :requiredFrom " +
            "AND (a.activeTo IS NULL OR :requiredToExclusive <= a.activeTo)")
    boolean existsActiveAssignmentCovering(
            @Param("campaignId") Long campaignId,
            @Param("centerId") Long centerId,
            @Param("status") CampaignCenterAssignmentStatus status,
            @Param("excludeId") Long excludeId,
            @Param("requiredFrom") LocalDate requiredFrom,
            @Param("requiredToExclusive") LocalDate requiredToExclusive
    );

    default boolean existsActiveAssignmentCovering(
            Long campaignId,
            Long centerId,
            CampaignCenterAssignmentStatus status,
            LocalDate requiredFrom,
            LocalDate requiredToExclusive
    ) {
        return existsActiveAssignmentCovering(campaignId, centerId, status, null, requiredFrom, requiredToExclusive);
    }
}
