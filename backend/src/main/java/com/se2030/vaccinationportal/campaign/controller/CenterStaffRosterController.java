package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.RegisterStaffRosterRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateStaffRosterRequest;
import com.se2030.vaccinationportal.campaign.dto.response.OperationalStaffRosterResponse;
import com.se2030.vaccinationportal.campaign.mapper.OperationalStaffRosterDtoMapper;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.service.OperationalStaffRosterService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing operational staff roster management endpoints for vaccination centers.
 */
@RestController
@RequestMapping("/api/v1/centers/{centerId}/staff-roster")
public class CenterStaffRosterController {

    private final OperationalStaffRosterService rosterService;

    public CenterStaffRosterController(OperationalStaffRosterService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping
    public ResponseEntity<OperationalStaffRosterResponse> registerStaff(
            @PathVariable("centerId") Long centerId,
            @Valid @RequestBody RegisterStaffRosterRequest request
    ) {
        OperationalStaffRoster created = rosterService.registerStaffMember(
                centerId,
                request.userId(),
                request.role(),
                request.status()
        );
        OperationalStaffRosterResponse response = OperationalStaffRosterDtoMapper.toResponse(created);
        URI location = URI.create(String.format("/api/v1/centers/%d/staff-roster/%d", centerId, created.getId()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OperationalStaffRosterResponse>> listStaffRoster(
            @PathVariable("centerId") Long centerId,
            @RequestParam(value = "role", required = false) StaffRole role,
            @RequestParam(value = "status", required = false) OperationalStaffStatus status
    ) {
        List<OperationalStaffRoster> roster = rosterService.getStaffRosterForCenter(centerId, role, status);
        List<OperationalStaffRosterResponse> response = roster.stream()
                .map(OperationalStaffRosterDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rosterId}")
    public ResponseEntity<OperationalStaffRosterResponse> getRosterEntry(
            @PathVariable("centerId") Long centerId,
            @PathVariable("rosterId") Long rosterId
    ) {
        OperationalStaffRoster roster = rosterService.getRosterEntry(centerId, rosterId);
        return ResponseEntity.ok(OperationalStaffRosterDtoMapper.toResponse(roster));
    }

    @PutMapping("/{rosterId}")
    public ResponseEntity<OperationalStaffRosterResponse> updateStaffRoster(
            @PathVariable("centerId") Long centerId,
            @PathVariable("rosterId") Long rosterId,
            @Valid @RequestBody UpdateStaffRosterRequest request
    ) {
        OperationalStaffRoster updated = rosterService.updateStaffRoster(
                centerId,
                rosterId,
                request.role(),
                request.status()
        );
        return ResponseEntity.ok(OperationalStaffRosterDtoMapper.toResponse(updated));
    }
}
