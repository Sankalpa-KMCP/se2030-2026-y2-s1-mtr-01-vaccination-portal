package com.se2030.vaccinationportal.campaign.reference.web;

import com.se2030.vaccinationportal.campaign.reference.dto.request.CreateVaccineTypeRequest;
import com.se2030.vaccinationportal.campaign.reference.dto.request.UpdateVaccineTypeRequest;
import com.se2030.vaccinationportal.campaign.reference.dto.response.VaccineTypeSummaryResponse;
import com.se2030.vaccinationportal.campaign.core.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.service.VaccineTypeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles vaccine type CRUD requests and passes business rules to the service.
 */
@RestController
@RequestMapping("/api/v1/vaccine-types")
public class VaccineTypeController {

    private final VaccineTypeService vaccineTypeService;

    public VaccineTypeController(VaccineTypeService vaccineTypeService) {
        this.vaccineTypeService = vaccineTypeService;
    }

    /** Read active vaccine types, or include inactive types for management. */
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

    /** Create an active vaccine type; @Valid checks the submitted fields. */
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

    /** Update the vaccine name and dose unit; its code stays the same. */
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

    /** Make an existing vaccine type available for new campaigns. */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<VaccineTypeSummaryResponse> activateVaccineType(@PathVariable Long id) {
        VaccineType activated = vaccineTypeService.activateVaccineType(id);
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(activated));
    }

    /** Stop new campaigns from selecting this type; keep the record. */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<VaccineTypeSummaryResponse> deactivateVaccineType(@PathVariable Long id) {
        VaccineType deactivated = vaccineTypeService.deactivateVaccineType(id);
        return ResponseEntity.ok(CampaignDtoMapper.toSummaryResponse(deactivated));
    }

    /** Permanently delete an unused type; the service blocks types in use. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<Void> deleteVaccineType(@PathVariable Long id) {
        vaccineTypeService.deleteVaccineType(id);
        return ResponseEntity.noContent().build();
    }
}
