package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.model.LocationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the CampaignLocationTarget entity.
 * Handles persistence and query methods for administrative service-area targeting.
 */
@Repository
public interface CampaignLocationTargetRepository extends JpaRepository<CampaignLocationTarget, Long> {

    /**
     * List all administrative location targets defined for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @return List of CampaignLocationTarget entities
     */
    List<CampaignLocationTarget> findByCampaignId(Long campaignId);

    /**
     * List all location targets for a campaign filtered by target type (e.g. all targeted districts).
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @return List of matching CampaignLocationTarget entities
     */
    List<CampaignLocationTarget> findByCampaignIdAndTargetType(Long campaignId, LocationTargetType targetType);

    /**
     * Check whether a specific administrative location target already exists for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @param targetCode Normalized administrative target code
     * @return true if target already exists
     */
    boolean existsByCampaignIdAndTargetTypeAndTargetCode(Long campaignId, LocationTargetType targetType, String targetCode);

    /**
     * Find a specific administrative location target for a campaign by type and code.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @param targetCode Normalized administrative target code
     * @return Optional containing the CampaignLocationTarget if found
     */
    Optional<CampaignLocationTarget> findByCampaignIdAndTargetTypeAndTargetCode(
            Long campaignId,
            LocationTargetType targetType,
            String targetCode
    );

    /**
     * List all campaign location targets across campaigns for a given target type and code.
     *
     * @param targetType LocationTargetType enum
     * @param targetCode Normalized administrative target code
     * @return List of matching CampaignLocationTarget entities
     */
    List<CampaignLocationTarget> findByTargetTypeAndTargetCode(LocationTargetType targetType, String targetCode);

    /**
     * Count the total number of location targets configured for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @return Total count of location targets
     */
    long countByCampaignId(Long campaignId);
}
