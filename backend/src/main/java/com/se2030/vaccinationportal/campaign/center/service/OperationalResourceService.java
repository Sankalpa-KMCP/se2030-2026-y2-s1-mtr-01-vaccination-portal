package com.se2030.vaccinationportal.campaign.center.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages center resources and checks how updates affect assigned sessions. */
@Service
@Transactional
public class OperationalResourceService {

    private final OperationalResourceRepository resourceRepository;
    private final VaccinationCenterRepository centerRepository;
    private final SessionResourceAssignmentRepository assignmentRepository;
    private final Clock clock;

    public OperationalResourceService(OperationalResourceRepository resourceRepository,
                                      VaccinationCenterRepository centerRepository,
                                      SessionResourceAssignmentRepository assignmentRepository,
                                      Clock clock) {
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository must not be null");
        this.centerRepository = Objects.requireNonNull(centerRepository, "centerRepository must not be null");
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository, "assignmentRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name is required");
        }

        String normalizedCode = code.trim();
        VaccinationCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));

        if (resourceRepository.existsByCenterIdAndCode(centerId, normalizedCode)) {
            throw new IllegalStateException("Resource code '" + normalizedCode + "' already exists for center " + centerId);
        }

        OperationalResourceStatus finalStatus = status != null ? status : OperationalResourceStatus.ACTIVE;
        try {
            OperationalResource resource = new OperationalResource(center, normalizedCode, resourceType, name, finalStatus);
            return resourceRepository.saveAndFlush(resource);
        } catch (DataIntegrityViolationException ex) {
            if (resourceRepository.existsByCenterIdAndCode(centerId, normalizedCode)) {
                throw new IllegalStateException("Resource code '" + normalizedCode + "' already exists for center " + centerId, ex);
            }
            throw ex;
        }
    }

    public OperationalResource createResource(Long centerId,
                                              String code,
                                              ResourceType resourceType,
                                              String name) {
        return createResource(centerId, code, resourceType, name, OperationalResourceStatus.ACTIVE);
    }

    /**
     * Updates mutable properties (name and status) of an operational resource.
     * Code and ResourceType are immutable.
     */
    public OperationalResource updateResource(Long centerId,
                                              Long resourceId,
                                              String name,
                                              OperationalResourceStatus status) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource ID is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Resource status is required");
        }

        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }

        OperationalResource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Operational resource not found with ID: " + resourceId));

        if (!resource.getCenter().getId().equals(centerId)) {
            throw new ResourceNotFoundException("Operational resource with ID " + resourceId + " does not belong to center " + centerId);
        }

        validateDeactivationImpact(resource, status);
        resource.setName(name.trim());
        resource.setStatus(status);
        return resourceRepository.saveAndFlush(resource);
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
        OperationalResource resource = resourceRepository.findByIdForUpdate(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Operational resource not found with ID: " + resourceId));
        validateDeactivationImpact(resource, newStatus);
        resource.setStatus(newStatus);
        return resourceRepository.saveAndFlush(resource);
    }

    /**
     * Retrieves an operational resource by ID with center ownership validation.
     */
    @Transactional(readOnly = true)
    public OperationalResource getResource(Long centerId, Long resourceId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource ID is required");
        }

        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }

        OperationalResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Operational resource not found with ID: " + resourceId));

        if (!resource.getCenter().getId().equals(centerId)) {
            throw new ResourceNotFoundException("Operational resource with ID " + resourceId + " does not belong to center " + centerId);
        }

        return resource;
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
                .orElseThrow(() -> new ResourceNotFoundException("Operational resource not found with ID: " + resourceId));
    }

    /**
     * Retrieves all operational resources for a center ordered by code.
     */
    @Transactional(readOnly = true)
    public List<OperationalResource> getResourcesForCenter(Long centerId) {
        return getResourcesForCenter(centerId, null);
    }

    /**
     * Retrieves operational resources for a center with optional resourceType filter.
     */
    @Transactional(readOnly = true)
    public List<OperationalResource> getResourcesForCenter(Long centerId, ResourceType resourceType) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }
        if (resourceType != null) {
            return resourceRepository.findByCenterIdAndResourceTypeOrderByCodeAsc(centerId, resourceType);
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
        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }
        return resourceRepository.findByCenterIdAndResourceTypeAndStatusOrderByCodeAsc(
                centerId,
                resourceType,
                OperationalResourceStatus.ACTIVE
        );
    }

    private void validateDeactivationImpact(OperationalResource resource, OperationalResourceStatus newStatus) {
        if (resource.getStatus() != OperationalResourceStatus.ACTIVE
                || newStatus != OperationalResourceStatus.INACTIVE) {
            return;
        }
        if (!assignmentRepository.findBlockingOperationalDependenciesForUpdate(
                resource.getId(),
                SessionLifecycleStatus.blockingOperationalDependencyStatuses(),
                clock.instant()
        ).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot deactivate operational resource " + resource.getId()
                            + " because a future or in-progress session depends on it"
            );
        }
    }
}
