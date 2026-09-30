package com.se2030.vaccinationportal.campaign.core.repository;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

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
     * List all administrative location targets defined for a campaign ordered deterministically.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @return List of CampaignLocationTarget entities
     */
    List<CampaignLocationTarget> findByCampaignIdOrderByTargetTypeAscTargetCodeAscIdAsc(Long campaignId);

    /**
     * List all location targets for a campaign filtered by target type (e.g. all targeted districts).
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @return List of matching CampaignLocationTarget entities
     */
    List<CampaignLocationTarget> findByCampaignIdAndTargetType(Long campaignId, LocationTargetType targetType);

    /**
     * List all location targets for a campaign filtered by target type ordered deterministically.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @return List of matching CampaignLocationTarget entities
     */
    List<CampaignLocationTarget> findByCampaignIdAndTargetTypeOrderByTargetCodeAscIdAsc(Long campaignId, LocationTargetType targetType);

    /**
     * Check whether a specific administrative location target already exists for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @param targetCode Normalized administrative target code
     * @return true if target already exists
     */
    boolean existsByCampaignIdAndTargetTypeAndTargetCode(Long campaignId, LocationTargetType targetType, String targetCode);

}
