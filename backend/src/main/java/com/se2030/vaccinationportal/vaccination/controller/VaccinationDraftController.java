package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationDraftResponse;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationDraftMapper;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.service.VaccinationDraftService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for temporary vaccination draft CRUD.
 */
@RestController
@RequestMapping("/api/v1/vaccination/drafts")
@PreAuthorize("hasRole('VACCINATOR')")
public class VaccinationDraftController {

    private final VaccinationDraftService draftService;

    public VaccinationDraftController(VaccinationDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    public ResponseEntity<VaccinationDraftResponse> createDraft(
            @Valid @RequestBody CreateVaccinationDraftRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VaccinationDraft created = draftService.createDraft(request, principal.getUserId());
        URI location = URI.create("/api/v1/vaccination/drafts/" + created.getId());
        return ResponseEntity.created(location).body(VaccinationDraftMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<VaccinationDraftResponse>> getAllDrafts() {
        List<VaccinationDraftResponse> response = draftService.getAllDrafts().stream()
                .map(VaccinationDraftMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaccinationDraftResponse> getDraftById(@PathVariable Long id) {
        return ResponseEntity.ok(VaccinationDraftMapper.toResponse(draftService.getDraftById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VaccinationDraftResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVaccinationDraftRequest request
    ) {
        return ResponseEntity.ok(VaccinationDraftMapper.toResponse(draftService.updateDraft(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        draftService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }
}
