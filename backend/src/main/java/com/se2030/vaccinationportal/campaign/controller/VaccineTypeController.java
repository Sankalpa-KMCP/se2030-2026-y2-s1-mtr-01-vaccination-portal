package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.CreateVaccineTypeRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateVaccineTypeRequest;
import com.se2030.vaccinationportal.campaign.dto.response.VaccineTypeSummaryResponse;
import com.se2030.vaccinationportal.campaign.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.service.VaccineTypeService;
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
 * REST Controller exposing master data endpoints for Vaccine Types.
 */
@RestController
@RequestMapping("/api/v1/vaccine-types")
public class VaccineTypeController {

    private final VaccineTypeService vaccineTypeService;

    public VaccineTypeController(VaccineTypeService vaccineTypeService) {
        this.vaccineTypeService = vaccineTypeService;
    }

    /**
     * List vaccine types, defaulting to active-only for selector lookups.
     *
     * @param includeInactive if true, includes inactive records for management views
     * @return List of VaccineTypeSummaryResponse objects
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<List<VaccineTypeSummaryResponse>> getVaccineTypes(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        List<VaccineType> types = vaccineTypeService.getVaccineTypes(includeInactive);
        List<VaccineTypeSummaryResponse> response = types.stream()
                .map(CampaignDtoMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<VaccineTypeSummaryResponse> createVaccineType(
            @Valid @RequestBody CreateVaccineTypeRequest request
    ) {
        VaccineType created = vaccineTypeService.createVaccineType(
                request.code(),
                request.name(),
                request.doseUnit()
        );
        URI location = URI.create("/api/v1/vaccine-types/" + created.getId());
        return ResponseEntity.created(location).body(CampaignDtoMapper.toSummaryResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<VaccineTypeSummaryResponse> updateVaccineType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVaccineTypeRequest request
    ) {
        VaccineType updated = vaccineTypeService.updateVaccineType(
                id,
                request.name(),
                request.doseUnit()
        );
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(updated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<VaccineTypeSummaryResponse> activateVaccineType(@PathVariable Long id) {
        VaccineType activated = vaccineTypeService.activateVaccineType(id);
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<VaccineTypeSummaryResponse> deactivateVaccineType(@PathVariable Long id) {
        VaccineType deactivated = vaccineTypeService.deactivateVaccineType(id);
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(deactivated));
    }
}
