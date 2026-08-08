package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the CampaignDoseProgramme entity.
 */
@Repository
public interface CampaignDoseProgrammeRepository extends JpaRepository<CampaignDoseProgramme, Long> {

    /**
     * List all dose programme versions for a campaign, ordered by version descending.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @return List of CampaignDoseProgramme records
     */
    List<CampaignDoseProgramme> findByCampaignIdOrderByVersionNumberDesc(Long campaignId);

    /**
     * List all dose programme versions for a campaign, ordered by activeFrom ascending.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @return List of CampaignDoseProgramme records
     */
    List<CampaignDoseProgramme> findByCampaignIdOrderByActiveFromAsc(Long campaignId);

    /**
     * Find a specific dose programme version for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param versionNumber Programme version number
     * @return Optional containing the matching CampaignDoseProgramme
     */
    Optional<CampaignDoseProgramme> findByCampaignIdAndVersionNumber(Long campaignId, int versionNumber);

    /**
     * Check if a specific version number already exists for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param versionNumber Version number
     * @return true if version exists
     */
    boolean existsByCampaignIdAndVersionNumber(Long campaignId, int versionNumber);

    /**
     * Query the effective dose programme version for a campaign on a given evaluation date.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param evaluationDate Evaluation date
     * @return Optional containing the effective CampaignDoseProgramme if one exists
     */
    @Query("SELECT p FROM CampaignDoseProgramme p " +
            "WHERE p.campaign.id = :campaignId " +
            "AND p.activeFrom <= :evaluationDate " +
            "AND (p.activeTo IS NULL OR p.activeTo >= :evaluationDate)")
    Optional<CampaignDoseProgramme> findEffectiveProgrammeForDate(
            @Param("campaignId") Long campaignId,
            @Param("evaluationDate") LocalDate evaluationDate
    );

    /**
     * Query any dose programme versions for a campaign whose effective period overlaps with [activeFrom, activeTo].
     * An open-ended activeTo is treated as extending indefinitely.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param excludeId Optional programme ID to exclude (e.g. during update)
     * @param activeFrom Period start date
     * @param activeTo Period end date (nullable for open-ended)
     * @return List of overlapping CampaignDoseProgramme records
     */
    @Query("SELECT p FROM CampaignDoseProgramme p " +
            "WHERE p.campaign.id = :campaignId " +
            "AND (:excludeId IS NULL OR p.id <> :excludeId) " +
            "AND (:activeTo IS NULL OR p.activeFrom <= :activeTo) " +
            "AND (p.activeTo IS NULL OR p.activeTo >= :activeFrom)")
    List<CampaignDoseProgramme> findOverlappingProgrammes(
            @Param("campaignId") Long campaignId,
            @Param("excludeId") Long excludeId,
            @Param("activeFrom") LocalDate activeFrom,
            @Param("activeTo") LocalDate activeTo
    );
}
