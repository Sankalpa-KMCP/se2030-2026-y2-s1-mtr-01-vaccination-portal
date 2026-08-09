package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing CampaignDoseProgramme lifecycle, effective-dated versioning,
 * and overlap prevention per canonical rules WBVP-SE2030 / MF02-BR-02 / MF02-BR-18.
 */
@Service
@Transactional
public class CampaignDoseProgrammeService {

    private final CampaignDoseProgrammeRepository doseProgrammeRepository;
    private final VaccinationCampaignRepository campaignRepository;
    private final Clock clock;

    public CampaignDoseProgrammeService(CampaignDoseProgrammeRepository doseProgrammeRepository,
                                       VaccinationCampaignRepository campaignRepository,
                                       Clock clock) {
        this.doseProgrammeRepository = doseProgrammeRepository;
        this.campaignRepository = campaignRepository;
        this.clock = clock;
    }

    /**
     * Create the initial dose programme version for a campaign.
     *
     * @param campaignId ID of the parent VaccinationCampaign
     * @param doseCategory PRIMARY or BOOSTER
     * @param permittedDoseNumbers Valid comma-separated dose numbers (e.g. "1,2", "3")
     * @param minimumIntervalDays Minimum spacing in days
     * @param previousEvidenceRequired Whether proof of previous dose is mandatory
     * @param expectedServiceMinutes Expected consultation/administration minutes per citizen
     * @param activeFrom Effective start date
     * @return Created CampaignDoseProgramme entity
     */
    public CampaignDoseProgramme createInitialProgramme(Long campaignId,
                                                        DoseCategory doseCategory,
                                                        String permittedDoseNumbers,
                                                        int minimumIntervalDays,
                                                        boolean previousEvidenceRequired,
                                                        Integer expectedServiceMinutes,
                                                        LocalDate activeFrom) {
        if (activeFrom == null) {
            throw new IllegalArgumentException("Active from date is required");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));

        validateNoOverlap(campaignId, null, activeFrom, null);

        CampaignDoseProgramme programme = new CampaignDoseProgramme(
                campaign,
                1,
                doseCategory,
                permittedDoseNumbers,
                minimumIntervalDays,
                previousEvidenceRequired,
                expectedServiceMinutes,
                activeFrom,
                null
        );

        return doseProgrammeRepository.save(programme);
    }

    /**
     * Create and publish a new dose programme version, superseding the active version
     * without in-place mutation of historical versions (MF02-BR-02 / MF02-BR-18).
     *
     * @param campaignId ID of the parent VaccinationCampaign
     * @param doseCategory PRIMARY or BOOSTER
     * @param permittedDoseNumbers Valid comma-separated dose numbers
     * @param minimumIntervalDays Minimum spacing in days
     * @param previousEvidenceRequired Whether proof of previous dose is mandatory
     * @param expectedServiceMinutes Expected consultation/administration minutes per citizen
     * @param effectiveStartDate Effective start date of the new version
     * @return Newly created CampaignDoseProgramme version
     */
    public CampaignDoseProgramme createNewVersion(Long campaignId,
                                                 DoseCategory doseCategory,
                                                 String permittedDoseNumbers,
                                                 int minimumIntervalDays,
                                                 boolean previousEvidenceRequired,
                                                 Integer expectedServiceMinutes,
                                                 LocalDate effectiveStartDate) {
        if (effectiveStartDate == null) {
            throw new IllegalArgumentException("Effective start date is required");
        }

        VaccinationCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found with ID: " + campaignId));

        LocalDate today = LocalDate.now(clock);
        if (effectiveStartDate.isBefore(today)) {
            throw new IllegalArgumentException(
                    "New dose programme version effective date cannot be in the past [MF02-BR-18]: " + effectiveStartDate
            );
        }

        List<CampaignDoseProgramme> existingHistory = doseProgrammeRepository.findByCampaignIdOrderByVersionNumberDesc(campaignId);

        int nextVersion = existingHistory.isEmpty() ? 1 : existingHistory.get(0).getVersionNumber() + 1;

        // Close out previous open-ended version at effectiveStartDate - 1 day to preserve history
        for (CampaignDoseProgramme existing : existingHistory) {
            if (existing.getActiveTo() == null) {
                if (effectiveStartDate.isBefore(existing.getActiveFrom()) || effectiveStartDate.isEqual(existing.getActiveFrom())) {
                    throw new IllegalArgumentException(
                            "New version effective date (" + effectiveStartDate +
                            ") must be strictly after the previous version start date (" + existing.getActiveFrom() + ")"
                    );
                }
                existing.setActiveTo(effectiveStartDate.minusDays(1));
                existing.validateConstraints();
                doseProgrammeRepository.save(existing);
            }
        }

        validateNoOverlap(campaignId, null, effectiveStartDate, null);

        CampaignDoseProgramme newProgramme = new CampaignDoseProgramme(
                campaign,
                nextVersion,
                doseCategory,
                permittedDoseNumbers,
                minimumIntervalDays,
                previousEvidenceRequired,
                expectedServiceMinutes,
                effectiveStartDate,
                null
        );

        return doseProgrammeRepository.save(newProgramme);
    }

    /**
     * Query the effective dose programme for a campaign on a specific date.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @param evaluationDate Date to evaluate
     * @return Optional containing the effective CampaignDoseProgramme if found
     */
    @Transactional(readOnly = true)
    public Optional<CampaignDoseProgramme> getEffectiveProgramme(Long campaignId, LocalDate evaluationDate) {
        return doseProgrammeRepository.findEffectiveProgrammeForDate(campaignId, evaluationDate);
    }

    /**
     * List all version history records for a campaign.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @return List of CampaignDoseProgramme records ordered by version descending
     */
    @Transactional(readOnly = true)
    public List<CampaignDoseProgramme> getProgrammeHistory(Long campaignId) {
        return doseProgrammeRepository.findByCampaignIdOrderByVersionNumberDesc(campaignId);
    }

    /**
     * Enforces the single effective version rule (MF02-BR-02) by checking for overlapping periods.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @param excludeId Programme ID to exclude from check (if updating)
     * @param activeFrom Period start date
     * @param activeTo Period end date (nullable)
     */
    public void validateNoOverlap(Long campaignId, Long excludeId, LocalDate activeFrom, LocalDate activeTo) {
        List<CampaignDoseProgramme> overlaps = doseProgrammeRepository.findOverlappingProgrammes(
                campaignId,
                excludeId,
                activeFrom,
                activeTo
        );
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException(
                    "Overlapping effective dose programme period detected for campaign ID: " + campaignId +
                    ". A campaign may have at most one effective programme version at any point in time (MF02-BR-02)."
            );
        }
    }
}
