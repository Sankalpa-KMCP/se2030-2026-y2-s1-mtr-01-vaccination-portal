package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain service managing center-owned physical operational resources.
 * Canonical Section 6.1, 6.4 and Table 12.1.
 */
@Service
@Transactional
public class OperationalResourceService {

    private final OperationalResourceRepository resourceRepository;
    private final VaccinationCenterRepository centerRepository;

    public OperationalResourceService(OperationalResourceRepository resourceRepository,
                                      VaccinationCenterRepository centerRepository) {
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository must not be null");
        this.centerRepository = Objects.requireNonNull(centerRepository, "centerRepository must not be null");
    }

    /**
     * Registers a new operational resource under a center.
     */
    public OperationalResource createResource(Long centerId,
                                              String code,
                                              ResourceType resourceType,
                                              String name,
                                              OperationalResourceStatus status) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource code is required");
        }
        String normalizedCode = code.trim();
        VaccinationCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found: " + centerId));

        if (resourceRepository.existsByCenterIdAndCode(centerId, normalizedCode)) {
            throw new IllegalStateException("Resource code '" + normalizedCode + "' already exists for center " + centerId);
        }

        OperationalResource resource = new OperationalResource(center, normalizedCode, resourceType, name, status);
        return resourceRepository.save(resource);
    }

    public OperationalResource createResource(Long centerId,
                                              String code,
                                              ResourceType resourceType,
                                              String name) {
        return createResource(centerId, code, resourceType, name, OperationalResourceStatus.ACTIVE);
    }

    /**
     * Updates the availability status of an operational resource.
     */
    public OperationalResource updateResourceStatus(Long resourceId, OperationalResourceStatus newStatus) {
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource ID is required");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Resource status is required");
        }
        OperationalResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Operational resource not found: " + resourceId));
        resource.setStatus(newStatus);
        return resourceRepository.save(resource);
    }

    /**
     * Retrieves an operational resource by ID.
     */
    @Transactional(readOnly = true)
    public OperationalResource getResource(Long resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource ID is required");
        }
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Operational resource not found: " + resourceId));
    }

    /**
     * Retrieves all operational resources for a center ordered by code.
     */
    @Transactional(readOnly = true)
    public List<OperationalResource> getResourcesForCenter(Long centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        return resourceRepository.findByCenterIdOrderByCodeAsc(centerId);
    }

    /**
     * Retrieves active operational resources for a center filtered by resource type.
     */
    @Transactional(readOnly = true)
    public List<OperationalResource> getActiveResourcesForCenter(Long centerId, ResourceType resourceType) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type is required");
        }
        return resourceRepository.findByCenterIdAndResourceTypeAndStatusOrderByCodeAsc(
                centerId,
                resourceType,
                OperationalResourceStatus.ACTIVE
        );
    }
}
