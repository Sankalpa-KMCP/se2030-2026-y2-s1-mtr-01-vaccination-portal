package com.se2030.vaccinationportal.campaign.session.planning.web;

import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.campaign.session.planning.dto.response.VaccinatorAssignedSessionResponse;
import com.se2030.vaccinationportal.campaign.session.planning.dto.response.VaccinatorAssignedSlotResponse;
import com.se2030.vaccinationportal.campaign.session.planning.service.TimeSlotService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Supplies the MF05 workspace with only the authenticated vaccinator's current assignments. */
@RestController
@RequestMapping("/api/v1/staff/vaccination-sessions")
public class VaccinatorAssignedSessionController {

    private final StaffContextQuery staffContext;
    private final TimeSlotService timeSlotService;
    private final Clock clock;

    public VaccinatorAssignedSessionController(
            StaffContextQuery staffContext,
            TimeSlotService timeSlotService,
            Clock clock
    ) {
        this.staffContext = Objects.requireNonNull(staffContext, "staffContext must not be null");
        this.timeSlotService = Objects.requireNonNull(timeSlotService, "timeSlotService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @GetMapping
    @PreAuthorize("hasRole('VACCINATOR')")
    public ResponseEntity<List<VaccinatorAssignedSessionResponse>> listMyCurrentSessions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Instant now = clock.instant();
        List<VaccinatorAssignedSessionResponse> response = staffContext.scheduleForUser(principal, now, true).stream()
                .filter(summary -> "VACCINATOR".equals(summary.role()))
                .filter(summary -> staffContext.isAssignedToSession(principal, summary.sessionId(), now))
                .map(summary -> new VaccinatorAssignedSessionResponse(
                        summary.sessionId(),
                        summary.campaignCode(),
                        summary.campaignName(),
                        summary.centerCode(),
                        summary.centerName(),
                        summary.sessionStatus(),
                        summary.startAt(),
                        summary.endAt(),
                        summary.timezone(),
                        timeSlotService.getSlotsForSession(summary.sessionId()).stream()
                                .map(slot -> new VaccinatorAssignedSlotResponse(
                                        slot.getId(), slot.getStartTime(), slot.getEndTime()))
                                .toList()
                ))
                .filter(session -> !session.slots().isEmpty())
                .toList();
        return ResponseEntity.ok(response);
    }
}
