package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        CampaignDoseProgramme programme = new CampaignDoseProgramme(
                campaign, 1, doseCategory, permittedDoseNumbers, minimumIntervalDays,
                previousEvidenceRequired, expectedServiceMinutes, activeFrom, null
        );

        if (doseProgrammeRepository.findTopByCampaignIdOrderByVersionNumberDesc(campaignId).isPresent()) {
            throw new IllegalStateException("Initial dose programme already exists for campaign ID: " + campaignId);
        }
        validateNoOverlap(campaignId, null, activeFrom, null);

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
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        LocalDate today = LocalDate.now(clock);
        if (effectiveStartDate.isBefore(today)) {
            throw new IllegalArgumentException(
                    "New dose programme version effective date cannot be in the past [MF02-BR-18]: " + effectiveStartDate
            );
        }

        Optional<CampaignDoseProgramme> latest = doseProgrammeRepository.findTopByCampaignIdOrderByVersionNumberDesc(campaignId);
        int nextVersion = latest.map(p -> p.getVersionNumber() + 1).orElse(1);

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

        Optional<CampaignDoseProgramme> current = doseProgrammeRepository.findByCampaignIdAndActiveToIsNull(campaignId);
        if (current.isPresent() && !effectiveStartDate.isAfter(current.get().getActiveFrom())) {
            throw new IllegalArgumentException(
                    "New version effective date (" + effectiveStartDate +
                    ") must be strictly after the previous version start date (" + current.get().getActiveFrom() + ")"
            );
        }

        Long currentId = current.map(CampaignDoseProgramme::getId).orElse(null);
        validateNoOverlap(campaignId, currentId, effectiveStartDate, null);
        current.ifPresent(existing -> {
            existing.closeAt(effectiveStartDate);
            doseProgrammeRepository.save(existing);
        });

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
     * Enforces effective period non-overlap (BR-028 / DEC-030) ensuring at most one active version at any date.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @param excludeId Programme ID to exclude from check (if updating)
     * @param activeFrom Period start date
     * @param activeTo Period end date (nullable)
     */
    private void validateNoOverlap(Long campaignId, Long excludeId, LocalDate activeFrom, LocalDate activeTo) {
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
