package com.se2030.vaccinationportal.campaign.reference.service;

import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the target groups available for campaign configuration. */
@Service
@Transactional
public class TargetGroupService {

    private final TargetGroupRepository targetGroupRepository;
    private final VaccinationCampaignRepository campaignRepository;

    public TargetGroupService(TargetGroupRepository targetGroupRepository,
            VaccinationCampaignRepository campaignRepository) {
        this.targetGroupRepository = targetGroupRepository;
        this.campaignRepository = campaignRepository;
    }

    /**
     * Retrieve target groups, optionally including inactive records.
     *
     * @param includeInactive whether to return both active and inactive records
     * @return List of TargetGroup entities
     */
    @Transactional(readOnly = true)
    public List<TargetGroup> getTargetGroups(boolean includeInactive) {
        if (includeInactive) {
            return targetGroupRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        }
        return targetGroupRepository.findByActiveTrue();
    }

    /**
     * Retrieve all currently active target demographic groups (backward compatible helper).
     *
     * @return List of active TargetGroup entities
     */
    @Transactional(readOnly = true)
    public List<TargetGroup> getActiveTargetGroups() {
        return getTargetGroups(false);
    }

    /**
     * Create a new TargetGroup record.
     */
    public TargetGroup createTargetGroup(String code, String description) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Target group code is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        String normalizedCode = code.trim();
        if (targetGroupRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Target group already exists with code: " + normalizedCode);
        }

        TargetGroup targetGroup = new TargetGroup(normalizedCode, description.trim(), true);
        return targetGroupRepository.save(targetGroup);
    }

    /**
     * Update mutable fields of an existing TargetGroup.
     */
    public TargetGroup updateTargetGroup(Long id, String description) {
        if (id == null) {
            throw new IllegalArgumentException("Target group ID is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }

        TargetGroup targetGroup = targetGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TargetGroup not found with ID: " + id));

        targetGroup.setDescription(description.trim());
        return targetGroupRepository.save(targetGroup);
    }

    /**
     * Activate a TargetGroup record.
     */
    public TargetGroup activateTargetGroup(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Target group ID is required");
        }
        TargetGroup targetGroup = targetGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TargetGroup not found with ID: " + id));
        targetGroup.setActive(true);
        return targetGroupRepository.save(targetGroup);
    }

    /**
     * Deactivate a TargetGroup record.
     */
    public TargetGroup deactivateTargetGroup(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Target group ID is required");
        }
        TargetGroup targetGroup = targetGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TargetGroup not found with ID: " + id));
        targetGroup.setActive(false);
        return targetGroupRepository.save(targetGroup);
    }

    /** Permanently remove an unused target group, preserving all historical references. */
    public void deleteTargetGroup(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Target group ID is required");
        }
        TargetGroup entry = targetGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TargetGroup not found with ID: " + id));
        if (campaignRepository.existsByTargetGroup_Id(id)) {
            throw new IllegalStateException("This target group is used by a campaign. Deactivate it instead.");
        }
        try {
            targetGroupRepository.delete(entry);
            targetGroupRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("This target group is used by a campaign. Deactivate it instead.", ex);
        }
    }
}
