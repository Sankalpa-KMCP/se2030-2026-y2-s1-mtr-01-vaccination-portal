package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.CreateCenterClosureRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateCenterRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateOperatingHoursRequest;
import com.se2030.vaccinationportal.campaign.dto.request.EndDateOperatingHoursRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateCenterClosureRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateCenterRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateOperatingHoursRequest;
import com.se2030.vaccinationportal.campaign.dto.response.CenterClosureResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CenterResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CenterSummaryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.OperatingHoursResponse;
import com.se2030.vaccinationportal.campaign.mapper.CenterDtoMapper;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.service.CenterClosureService;
import com.se2030.vaccinationportal.campaign.service.OperatingHoursService;
import com.se2030.vaccinationportal.campaign.service.VaccinationCenterService;
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
 * REST Controller exposing physical vaccination center management, operating hours, and center closures endpoints.
 */
@RestController
@RequestMapping("/api/v1/centers")
public class CenterController {

    private final VaccinationCenterService centerService;
    private final OperatingHoursService operatingHoursService;
    private final CenterClosureService closureService;

    public CenterController(
            VaccinationCenterService centerService,
            OperatingHoursService operatingHoursService,
            CenterClosureService closureService
    ) {
        this.centerService = centerService;
        this.operatingHoursService = operatingHoursService;
        this.closureService = closureService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterResponse> createCenter(@Valid @RequestBody CreateCenterRequest request) {
        VaccinationCenter created = centerService.createCenter(
                request.code(),
                request.name(),
                request.address(),
                request.contact(),
                request.safeConcurrentOccupancy(),
                request.sessionServiceCeiling(),
                request.timezone()
        );

        URI location = URI.create("/api/v1/centers/" + created.getId());
        return ResponseEntity.created(location).body(CenterDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'MANAGEMENT_USER')")
    public ResponseEntity<CenterResponse> getCenterById(@PathVariable Long id) {
        VaccinationCenter center = centerService.getRequiredCenterById(id);
        return ResponseEntity.ok(CenterDtoMapper.toResponse(center));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CenterSummaryResponse>> getCenters(
            @RequestParam(required = false) CenterStatus status
    ) {
        List<VaccinationCenter> centers = centerService.getCenters(status);
        List<CenterSummaryResponse> response = centers.stream()
                .map(CenterDtoMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterResponse> updateCenter(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCenterRequest request
    ) {
        VaccinationCenter updated = centerService.updateCenter(
                id,
                request.name(),
                request.address(),
                request.contact(),
                request.safeConcurrentOccupancy(),
                request.sessionServiceCeiling()
        );
        return ResponseEntity.ok(CenterDtoMapper.toResponse(updated));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterResponse> activateCenter(@PathVariable Long id) {
        VaccinationCenter activated = centerService.activateCenter(id);
        return ResponseEntity.ok(CenterDtoMapper.toResponse(activated));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterResponse> deactivateCenter(@PathVariable Long id) {
        VaccinationCenter deactivated = centerService.deactivateCenter(id);
        return ResponseEntity.ok(CenterDtoMapper.toResponse(deactivated));
    }

    @PostMapping("/{id}/operating-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<OperatingHoursResponse> createOperatingHours(
            @PathVariable Long id,
            @Valid @RequestBody CreateOperatingHoursRequest request
    ) {
        OperatingHours created = operatingHoursService.createOperatingHours(
                id,
                request.dayOfWeek(),
                request.opening(),
                request.closing(),
                request.effectiveFrom(),
                request.effectiveTo()
        );

        URI location = URI.create("/api/v1/centers/" + id + "/operating-hours/" + created.getId());
        return ResponseEntity.created(location).body(CenterDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}/operating-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<OperatingHoursResponse>> getOperatingHours(@PathVariable Long id) {
        List<OperatingHours> hours = operatingHoursService.getOperatingHoursByCenter(id);
        List<OperatingHoursResponse> response = hours.stream()
                .map(CenterDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{centerId}/operating-hours/{hoursId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<OperatingHoursResponse> updateOperatingHours(
            @PathVariable Long centerId,
            @PathVariable Long hoursId,
            @Valid @RequestBody UpdateOperatingHoursRequest request
    ) {
        OperatingHours updated = operatingHoursService.updateOperatingHours(
                centerId,
                hoursId,
                request.dayOfWeek(),
                request.opening(),
                request.closing(),
                request.effectiveFrom(),
                request.effectiveTo()
        );
        return ResponseEntity.ok(CenterDtoMapper.toResponse(updated));
    }

    @PostMapping("/{centerId}/operating-hours/{hoursId}/end-date")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<OperatingHoursResponse> endDateOperatingHours(
            @PathVariable Long centerId,
            @PathVariable Long hoursId,
            @Valid @RequestBody EndDateOperatingHoursRequest request
    ) {
        OperatingHours updated = operatingHoursService.endDateOperatingHours(
                centerId,
                hoursId,
                request.endDate()
        );
        return ResponseEntity.ok(CenterDtoMapper.toResponse(updated));
    }

    @PostMapping("/{id}/closures")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterClosureResponse> createClosure(
            @PathVariable Long id,
            @Valid @RequestBody CreateCenterClosureRequest request
    ) {
        CenterClosure created = closureService.createClosure(
                id,
                request.startAt(),
                request.endAt(),
                request.reason()
        );

        URI location = URI.create("/api/v1/centers/" + id + "/closures/" + created.getId());
        return ResponseEntity.created(location).body(CenterDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}/closures")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_OFFICER', 'CENTER_MANAGER', 'MANAGEMENT_USER')")
    public ResponseEntity<List<CenterClosureResponse>> getClosures(@PathVariable Long id) {
        List<CenterClosure> closures = closureService.getClosuresForCenter(id);
        List<CenterClosureResponse> response = closures.stream()
                .map(CenterDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{centerId}/closures/{closureId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterClosureResponse> updateClosure(
            @PathVariable Long centerId,
            @PathVariable Long closureId,
            @Valid @RequestBody UpdateCenterClosureRequest request
    ) {
        CenterClosure updated = closureService.updateFutureClosure(
                centerId,
                closureId,
                request.startAt(),
                request.endAt(),
                request.reason()
        );
        return ResponseEntity.ok(CenterDtoMapper.toResponse(updated));
    }

    @PostMapping("/{centerId}/closures/{closureId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<CenterClosureResponse> cancelClosure(
            @PathVariable Long centerId,
            @PathVariable Long closureId
    ) {
        CenterClosure cancelled = closureService.cancelFutureClosure(
                centerId,
                closureId
        );
        return ResponseEntity.ok(CenterDtoMapper.toResponse(cancelled));
    }
}
