package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationCorrectionResponse;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.service.VaccinationCorrectionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for append-only corrections to finalized vaccination records. */
@RestController
@RequestMapping("/api/v1/vaccination/records/{recordId}/corrections")
@PreAuthorize("hasAnyRole('ADMIN', 'HEALTHCARE_OFFICER')")
public class VaccinationCorrectionController {

    private final VaccinationCorrectionService correctionService;

    public VaccinationCorrectionController(VaccinationCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    @PostMapping
    public ResponseEntity<VaccinationCorrectionResponse> createCorrection(
            @PathVariable Long recordId,
            @Valid @RequestBody CreateVaccinationCorrectionRequest request
    ) {
        VaccinationCorrection created = correctionService.createCorrection(recordId, request);
        URI location = URI.create(
                "/api/v1/vaccination/records/" + recordId + "/corrections/" + created.getId()
        );
        return ResponseEntity.created(location).body(VaccinationCorrectionMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<VaccinationCorrectionResponse>> getCorrectionHistory(
            @PathVariable Long recordId
    ) {
        List<VaccinationCorrectionResponse> response = correctionService.getCorrectionHistory(recordId)
                .stream()
                .map(VaccinationCorrectionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
