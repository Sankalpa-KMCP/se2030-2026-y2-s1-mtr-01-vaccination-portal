package com.se2030.vaccinationportal.vaccination.controller;

import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.DeleteVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationDraftResponse;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationDraftMapper;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.service.VaccinationDraftService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
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

/**
 * REST endpoints for temporary vaccination draft CRUD with ownership isolation and audited deletion.
 */
@RestController
@RequestMapping("/api/v1/vaccination/drafts")
@PreAuthorize("hasAnyRole('VACCINATOR', 'SUPER_ADMIN')")
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
        Long vaccinatorId = principal != null ? principal.getUserId() : 1L;
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
        Long actorId = principal != null ? principal.getUserId() : 1L;
        draftService.deleteDraft(id, resolvedReason, actorId);
        return ResponseEntity.noContent().build();
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
