package com.se2030.vaccinationportal.campaign.core.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.core.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLocationTargetRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Manages campaign service areas, separately from medical eligibility rules. */
@Service
@Transactional
public class CampaignLocationTargetService {

    private final CampaignLocationTargetRepository locationTargetRepository;
    private final VaccinationCampaignRepository campaignRepository;

    public CampaignLocationTargetService(CampaignLocationTargetRepository locationTargetRepository,
                                        VaccinationCampaignRepository campaignRepository) {
        this.locationTargetRepository = locationTargetRepository;
        this.campaignRepository = campaignRepository;
    }

    /**
     * Add an administrative location target to a campaign.
     * Normalizes the target code and rejects duplicate targets for the same campaign/type/code.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @param targetCode Administrative location code (e.g. "WP", "COLOMBO", "HR-NORTH")
     * @return Saved CampaignLocationTarget entity
     */
    public CampaignLocationTarget addTarget(Long campaignId, LocationTargetType targetType, String targetCode) {
        return addTargets(campaignId, List.of(new TargetEntry(targetType, targetCode))).get(0);
    }

    /**
     * Atomically add multiple location targets to a campaign in a single transaction.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetEntries List of TargetEntry records to add
     * @return List of persisted CampaignLocationTarget entities
     */
    public List<CampaignLocationTarget> addTargets(Long campaignId, List<TargetEntry> targetEntries) {
        if (targetEntries == null || targetEntries.isEmpty()) {
            return Collections.emptyList();
        }

        VaccinationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        Set<String> existingKeys = new HashSet<>();
        for (CampaignLocationTarget existing : locationTargetRepository.findByCampaignId(campaignId)) {
            existingKeys.add(key(existing.getTargetType(), existing.getTargetCode()));
        }

        List<CampaignLocationTarget> targets = new ArrayList<>(targetEntries.size());
        for (TargetEntry entry : targetEntries) {
            String normalizedCode = CampaignLocationTarget.normalizeTargetCode(entry.targetCode());
            if (!existingKeys.add(key(entry.targetType(), normalizedCode))) {
                throw new IllegalArgumentException(
                        "Location target already exists for campaign ID " + campaignId +
                        ": " + entry.targetType() + " - " + normalizedCode
                );
            }
            targets.add(new CampaignLocationTarget(campaign, entry.targetType(), normalizedCode));
        }
        return locationTargetRepository.saveAll(targets);
    }

    /**
     * List all administrative location targets for a campaign.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @return List of CampaignLocationTarget entities ordered deterministically
     */
    @Transactional(readOnly = true)
    public List<CampaignLocationTarget> getTargetsForCampaign(Long campaignId) {
        return locationTargetRepository.findByCampaignIdOrderByTargetTypeAscTargetCodeAscIdAsc(campaignId);
    }

    /**
     * List all location targets for a campaign filtered by target type.
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @return List of matching CampaignLocationTarget entities ordered deterministically
     */
    @Transactional(readOnly = true)
    public List<CampaignLocationTarget> getTargetsForCampaignAndType(Long campaignId, LocationTargetType targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type is required");
        }
        return locationTargetRepository.findByCampaignIdAndTargetTypeOrderByTargetCodeAscIdAsc(campaignId, targetType);
    }

    /**
     * Performs exact type/code matching to check if a campaign targets a specific location.
     * Does NOT infer administrative hierarchy (e.g. DISTRICT=COLOMBO does not imply PROVINCE=WP).
     *
     * @param campaignId VaccinationCampaign entity ID
     * @param targetType LocationTargetType enum
     * @param targetCode Administrative location code
     * @return true if exact target match exists
     */
    @Transactional(readOnly = true)
    public boolean isCampaignTargeting(Long campaignId, LocationTargetType targetType, String targetCode) {
        if (campaignId == null || targetType == null || targetCode == null || targetCode.trim().isEmpty()) {
            return false;
        }
        String normalizedCode = CampaignLocationTarget.normalizeTargetCode(targetCode);
        return locationTargetRepository.existsByCampaignIdAndTargetTypeAndTargetCode(campaignId, targetType, normalizedCode);
    }

    /**
     * Simple record representing a target entry for batch additions.
     */
    public record TargetEntry(LocationTargetType targetType, String targetCode) {
        public TargetEntry {
            if (targetType == null) {
                throw new IllegalArgumentException("Target type is required");
            }
            if (targetCode == null || targetCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Target code is required");
            }
        }
    }

    private static String key(LocationTargetType targetType, String targetCode) {
        return targetType.name() + '\0' + targetCode;
    }
}
