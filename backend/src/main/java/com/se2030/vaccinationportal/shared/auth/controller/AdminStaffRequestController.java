package com.se2030.vaccinationportal.shared.auth.controller;

import com.se2030.vaccinationportal.shared.auth.dto.StaffApprovalRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRejectionRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRequestResponse;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.StaffRequestReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/staff-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffRequestController {

    private final StaffRequestReviewService staffRequestReviewService;

    public AdminStaffRequestController(StaffRequestReviewService staffRequestReviewService) {
        this.staffRequestReviewService = staffRequestReviewService;
    }

    @GetMapping
    public List<StaffRequestResponse> listPendingRequests() {
        return staffRequestReviewService.listPendingRequests();
    }

    @GetMapping("/{id}")
    public StaffRequestResponse getStaffRequest(@PathVariable Long id) {
        return staffRequestReviewService.getStaffRequest(id);
    }

    @PostMapping("/{id}/approve")
    public StaffRequestResponse approve(
            @PathVariable Long id,
            @Valid @RequestBody StaffApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return staffRequestReviewService.approve(id, request, principal.getUserId());
    }

    @PostMapping("/{id}/reject")
    public StaffRequestResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody StaffRejectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return staffRequestReviewService.reject(id, request, principal.getUserId());
    }
}
