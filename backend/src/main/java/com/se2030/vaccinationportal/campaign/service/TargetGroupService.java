package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing read-only master data access for Target Groups.
 */
@Service
@Transactional(readOnly = true)
public class TargetGroupService {

    private final TargetGroupRepository targetGroupRepository;

    public TargetGroupService(TargetGroupRepository targetGroupRepository) {
        this.targetGroupRepository = targetGroupRepository;
    }

    /**
     * Retrieve all currently active target demographic groups.
     *
     * @return List of active TargetGroup entities
     */
    public List<TargetGroup> getActiveTargetGroups() {
        return targetGroupRepository.findByActiveTrue();
    }
}
