package com.se2030.vaccinationportal.vaccination.administration.web;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.DeleteVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationDraftResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.mapper.VaccinationDraftMapper;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationDraftService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.se2030.vaccinationportal.vaccination.administration.dto.FinalizeVaccinationResponse;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationFinalizationService;

/**
 * REST endpoints for temporary vaccination draft CRUD with ownership isolation and audited deletion.
 */
@RestController
@RequestMapping("/api/v1/vaccination/drafts")
@PreAuthorize("hasAnyRole('VACCINATOR', 'SUPER_ADMIN')")
public class VaccinationDraftController {

    private final VaccinationDraftService draftService;
    private final VaccinationFinalizationService finalizationService;

    public VaccinationDraftController(VaccinationDraftService draftService) {
        this(draftService, null);
    }

    @Autowired
    public VaccinationDraftController(
            VaccinationDraftService draftService,
            VaccinationFinalizationService finalizationService
    ) {
        this.draftService = draftService;
        this.finalizationService = finalizationService;
    }

    @PostMapping
    public ResponseEntity<VaccinationDraftResponse> createDraft(
            @Valid @RequestBody CreateVaccinationDraftRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long vaccinatorId = principal != null ? principal.getUserId() : null;
        if (vaccinatorId == null) {
            throw new AccessDeniedException("Authenticated vaccinator ID is required");
        }
        VaccinationDraft created = draftService.createDraft(request, vaccinatorId);
        URI location = URI.create("/api/v1/vaccination/drafts/" + created.getId());
        return ResponseEntity.created(location).body(VaccinationDraftMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<VaccinationDraftResponse>> getAllDrafts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<VaccinationDraft> drafts;
        if (principal != null && principal.hasRole("ROLE_SUPER_ADMIN")) {
            drafts = draftService.getAllDrafts();
        } else if (principal != null) {
            drafts = draftService.getAllDraftsByVaccinatorId(principal.getUserId());
        } else {
            drafts = draftService.getAllDrafts();
        }
        List<VaccinationDraftResponse> response = drafts.stream()
                .map(VaccinationDraftMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaccinationDraftResponse> getDraftById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VaccinationDraft draft = draftService.getDraftById(id);
        assertDraftOwnership(draft, principal);
        return ResponseEntity.ok(VaccinationDraftMapper.toResponse(draft));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VaccinationDraftResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVaccinationDraftRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VaccinationDraft existing = draftService.getDraftById(id);
        assertDraftOwnership(existing, principal);
        return ResponseEntity.ok(VaccinationDraftMapper.toResponse(draftService.updateDraft(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String paramReason,
            @Valid @RequestBody(required = false) DeleteVaccinationDraftRequest body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VaccinationDraft existing = draftService.getDraftById(id);
        assertDraftOwnership(existing, principal);

        String resolvedReason;
        if (body != null && body.reason() != null && !body.reason().isBlank()) {
            resolvedReason = body.reason().trim();
        } else if (paramReason != null && !paramReason.isBlank()) {
            try {
                resolvedReason = java.net.URLDecoder.decode(paramReason, java.nio.charset.StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                resolvedReason = paramReason.replace('+', ' ').trim();
            }
        } else {
            resolvedReason = null;
        }

        if (resolvedReason == null || resolvedReason.isEmpty()) {
            throw new InvalidVaccinationDraftException("Draft deletion reason is required and cannot be blank");
        }
        Long actorId = principal != null ? principal.getUserId() : null;
        if (actorId == null) {
            throw new AccessDeniedException("Authenticated actor ID is required");
        }
        draftService.deleteDraft(id, resolvedReason, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-administration")
    public ResponseEntity<VaccinationDraftResponse> confirmAdministration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VaccinationDraft existing = draftService.getDraftById(id);
        assertDraftOwnership(existing, principal);
        VaccinationDraft confirmed = draftService.confirmPhysicalAdministration(id);
        return ResponseEntity.ok(VaccinationDraftMapper.toResponse(confirmed));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<FinalizeVaccinationResponse> finalizeDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VaccinationDraft existing = draftService.getDraftById(id);
        assertDraftOwnership(existing, principal);
        Long vaccinatorId = principal != null ? principal.getUserId() : existing.getVaccinatorId();
        if (vaccinatorId == null) {
            throw new AccessDeniedException("Authenticated vaccinator ID is required");
        }
        if (finalizationService == null) {
            throw new IllegalStateException("Vaccination finalization service is not configured");
        }
        FinalizeVaccinationResponse response = finalizationService.finalizeDraft(id, vaccinatorId);
        return ResponseEntity.ok(response);
    }

    private void assertDraftOwnership(VaccinationDraft draft, UserPrincipal principal) {
        if (principal == null || principal.hasRole("ROLE_SUPER_ADMIN")) {
            return;
        }
        if (draft.getVaccinatorId() != null && !Objects.equals(draft.getVaccinatorId(), principal.getUserId())) {
            throw new AccessDeniedException("Access denied: vaccination draft belongs to another vaccinator");
        }
    }
}
