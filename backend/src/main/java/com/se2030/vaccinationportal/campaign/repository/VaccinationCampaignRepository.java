package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the VaccinationCampaign entity.
 */
@Repository
public interface VaccinationCampaignRepository extends JpaRepository<VaccinationCampaign, Long> {

    /**
     * Find a campaign by ID and acquire a pessimistic write lock (SELECT ... FOR UPDATE).
     * Serializes concurrent effective-dated configuration changes (dose programmes, questionnaire
     * assignments, campaign-center assignments) per WBVP-SE2030 Section 12.4.
     *
     * @param id Unique campaign identifier
     * @return Optional containing the locked VaccinationCampaign if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM VaccinationCampaign c WHERE c.id = :id")
    Optional<VaccinationCampaign> findByIdForUpdate(@Param("id") Long id);

    /**
     * Find a campaign by its unique business code.
     *
     * @param code Unique campaign code (e.g. "CMP-2026-COVID-01")
     * @return Optional containing the VaccinationCampaign if found
     */
    Optional<VaccinationCampaign> findByCode(String code);

    /**
     * Check whether a campaign with the specified code already exists.
     *
     * @param code Unique campaign code
     * @return true if an entity with this code exists
     */
    boolean existsByCode(String code);

    /**
     * List all campaigns with a specific lifecycle status.
     *
     * @param status CampaignStatus enum value
     * @return List of matching campaigns
     */
    List<VaccinationCampaign> findByStatus(CampaignStatus status);

    /**
     * List all campaigns configured with a specific vaccine type.
     *
     * @param vaccineTypeId VaccineType entity ID
     * @return List of matching campaigns
     */
    List<VaccinationCampaign> findByVaccineTypeId(Long vaccineTypeId);

    /**
     * List all campaigns configured for a specific target demographic group.
     *
     * @param targetGroupId TargetGroup entity ID
     * @return List of matching campaigns
     */
    List<VaccinationCampaign> findByTargetGroupId(Long targetGroupId);

    /**
     * Type-safe query for campaigns with specified statuses whose configured booking intake window
     * contains the given evaluation timestamp.
     *
     * @param statuses Collection of permitted CampaignStatus values
     * @param timestamp Current evaluation timestamp
     * @return List of matching campaigns
     */
    @Query("SELECT c FROM VaccinationCampaign c WHERE c.status IN :statuses " +
            "AND :timestamp >= c.bookingOpenAt AND :timestamp <= c.bookingCloseAt")
    List<VaccinationCampaign> findByStatusInAndBookingWindowOpenAt(
            @Param("statuses") Collection<CampaignStatus> statuses,
            @Param("timestamp") LocalDateTime timestamp
    );

    /**
     * Query campaigns that are published or active and currently within their citizen booking intake window (MF02-BR-03).
     * Type-safe convenience method delegating with CampaignStatus.PUBLISHED and CampaignStatus.ACTIVE.
     *
     * @param timestamp Current evaluation timestamp
     * @return List of campaigns currently open for booking intake
     */
    default List<VaccinationCampaign> findOpenForBookingAt(LocalDateTime timestamp) {
        return findByStatusInAndBookingWindowOpenAt(
                List.of(CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE),
                timestamp
        );
    }

    /**
     * Query campaigns whose service date range overlaps with the given date span.
     * Checks date range overlap regardless of campaign status.
     *
     * @param startDate Range start date
     * @param endDate Range end date
     * @return List of campaigns with overlapping service dates
     */
    @Query("SELECT c FROM VaccinationCampaign c WHERE c.serviceStartDate <= :endDate AND c.serviceEndDate >= :startDate")
    List<VaccinationCampaign> findOverlappingServiceDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
