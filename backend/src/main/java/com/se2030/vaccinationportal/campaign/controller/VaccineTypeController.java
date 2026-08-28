package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.response.VaccineTypeSummaryResponse;
import com.se2030.vaccinationportal.campaign.mapper.CampaignDtoMapper;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.service.VaccineTypeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing read-only lookup endpoints for active Vaccine Types.
 */
@RestController
@RequestMapping("/api/v1/vaccine-types")
public class VaccineTypeController {

    private final VaccineTypeService vaccineTypeService;

    public VaccineTypeController(VaccineTypeService vaccineTypeService) {
        this.vaccineTypeService = vaccineTypeService;
    }

    /**
     * List all currently active vaccine types.
     *
     * @return List of active VaccineTypeSummaryResponse objects
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER')")
    public ResponseEntity<List<VaccineTypeSummaryResponse>> getActiveVaccineTypes() {
        List<VaccineType> activeTypes = vaccineTypeService.getActiveVaccineTypes();
        List<VaccineTypeSummaryResponse> response = activeTypes.stream()
                .map(CampaignDtoMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
