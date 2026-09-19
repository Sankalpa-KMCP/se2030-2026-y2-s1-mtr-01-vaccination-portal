package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.CreateOperationalResourceRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateOperationalResourceRequest;
import com.se2030.vaccinationportal.campaign.dto.response.OperationalResourceResponse;
import com.se2030.vaccinationportal.campaign.mapper.OperationalResourceDtoMapper;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.service.OperationalResourceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing physical operational resource endpoints for vaccination centers.
 */
@RestController
@RequestMapping("/api/v1/centers/{centerId}/resources")
public class CenterOperationalResourceController {

    private final OperationalResourceService resourceService;

    public CenterOperationalResourceController(OperationalResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<OperationalResourceResponse> createResource(
            @PathVariable("centerId") Long centerId,
            @Valid @RequestBody CreateOperationalResourceRequest request
    ) {
        OperationalResource created = resourceService.createResource(
                centerId,
                request.code(),
                request.resourceType(),
                request.name(),
                request.status()
        );
        OperationalResourceResponse response = OperationalResourceDtoMapper.toResponse(created);
        URI location = URI.create(String.format("/api/v1/centers/%d/resources/%d", centerId, created.getId()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<List<OperationalResourceResponse>> listResources(
            @PathVariable("centerId") Long centerId,
            @RequestParam(value = "resourceType", required = false) ResourceType resourceType
    ) {
        List<OperationalResource> resources = resourceService.getResourcesForCenter(centerId, resourceType);
        List<OperationalResourceResponse> response = resources.stream()
                .map(OperationalResourceDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<OperationalResourceResponse> getResource(
            @PathVariable("centerId") Long centerId,
            @PathVariable("resourceId") Long resourceId
    ) {
        OperationalResource resource = resourceService.getResource(centerId, resourceId);
        return ResponseEntity.ok(OperationalResourceDtoMapper.toResponse(resource));
    }

    @PutMapping("/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<OperationalResourceResponse> updateResource(
            @PathVariable("centerId") Long centerId,
            @PathVariable("resourceId") Long resourceId,
            @Valid @RequestBody UpdateOperationalResourceRequest request
    ) {
        OperationalResource updated = resourceService.updateResource(
                centerId,
                resourceId,
                request.name(),
                request.status()
        );
        return ResponseEntity.ok(OperationalResourceDtoMapper.toResponse(updated));
    }
}
