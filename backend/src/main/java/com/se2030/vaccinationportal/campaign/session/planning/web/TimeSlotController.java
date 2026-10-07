package com.se2030.vaccinationportal.campaign.session.planning.web;

import com.se2030.vaccinationportal.campaign.session.planning.dto.request.CreateTimeSlotRequest;
import com.se2030.vaccinationportal.campaign.session.planning.dto.request.UpdateTimeSlotCapacityRequest;
import com.se2030.vaccinationportal.campaign.session.planning.dto.response.TimeSlotResponse;
import com.se2030.vaccinationportal.campaign.session.planning.mapper.TimeSlotDtoMapper;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.service.TimeSlotService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing operational time-slot management endpoints for vaccination sessions.
 */
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/time-slots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = Objects.requireNonNull(timeSlotService, "timeSlotService must not be null");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<List<TimeSlotResponse>> listTimeSlots(
            @PathVariable("sessionId") Long sessionId
    ) {
        List<TimeSlot> slots = timeSlotService.getSlotsForSession(sessionId);
        List<TimeSlotResponse> response = slots.stream()
                .map(TimeSlotDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<TimeSlotResponse> createTimeSlot(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody CreateTimeSlotRequest request
    ) {
        TimeSlot created = timeSlotService.createSlot(
                sessionId,
                request.startTime(),
                request.endTime(),
                request.capacity()
        );
        TimeSlotResponse response = TimeSlotDtoMapper.toResponse(created);
        URI location = URI.create(String.format("/api/v1/sessions/%d/time-slots/%d", sessionId, created.getId()));
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{slotId}/capacity")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<TimeSlotResponse> updateSlotCapacity(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("slotId") Long slotId,
            @Valid @RequestBody UpdateTimeSlotCapacityRequest request
    ) {
        TimeSlot updated = timeSlotService.updateSlotCapacity(
                sessionId,
                slotId,
                request.capacity()
        );
        return ResponseEntity.ok(TimeSlotDtoMapper.toResponse(updated));
    }
}
