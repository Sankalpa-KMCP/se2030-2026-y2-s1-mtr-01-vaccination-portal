package com.se2030.vaccinationportal.appointment.booking.web;

import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentBookingRequest;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentCancellationRequest;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentRescheduleRequest;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentResponse;
import com.se2030.vaccinationportal.appointment.booking.dto.AvailableSessionResponse;
import com.se2030.vaccinationportal.appointment.checkin.dto.CheckInRequest;
import com.se2030.vaccinationportal.appointment.checkin.dto.CheckInReversalRequest;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentRescheduleService;
import com.se2030.vaccinationportal.appointment.query.service.AppointmentSearchService;
import com.se2030.vaccinationportal.appointment.checkin.service.CheckInService;
import com.se2030.vaccinationportal.appointment.checkin.service.CheckInCenterAuthorizationService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing appointment booking, availability search, citizen listing, cancellation, rescheduling,
 * check-in, check-in reversal, and queue endpoints.
 * Thin presentation layer delegating all orchestration strictly to existing domain services.
 *
 * <p><b>User Stories:</b>
 * <ul>
 *   <li>US-03: Citizen appointment availability search, booking, cancellation, and rescheduling.</li>
 *   <li>US-26: Clinical vaccinator queue inspection, arrival check-in, reversal, and no-show marking.</li>
 * </ul>
 *
 * <p><b>Business Rules Enforced:</b>
 * <ul>
 *   <li>MF03-BR-01: Gating booking against screening eligibility.</li>
 *   <li>MF03-BR-02: Enforcing single active booking per citizen per campaign.</li>
 *   <li>MF03-BR-03: Atomic slot capacity allocation.</li>
 *   <li>MF03-BR-04: 24-hour notice window with staff override check for late cancellation/reschedule.</li>
 *   <li>MF03-BR-05: Atomic slot reallocation on reschedule.</li>
 *   <li>MF03-BR-09 &amp; MF03-BR-10: 30-minute check-in arrival window and CONFIRMED status check.</li>
 *   <li>MF03-BR-11: Immutability of COMPLETED appointments.</li>
 *   <li>MF03-BR-15: Authorized check-in reversal before completion.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentBookingService bookingService;
    private final AppointmentCancellationService cancellationService;
    private final AppointmentRescheduleService rescheduleService;
    private final CheckInService checkInService;
    private final AppointmentSearchService searchService;
    private final AppointmentRepository appointmentRepository;
    private final CheckInCenterAuthorizationService checkInAuthorization;

    public AppointmentController(
            AppointmentBookingService bookingService,
            AppointmentCancellationService cancellationService,
            AppointmentRescheduleService rescheduleService,
            CheckInService checkInService
    ) {
        this(bookingService, cancellationService, rescheduleService, checkInService, null, null, null);
    }

    @Autowired
    public AppointmentController(
            AppointmentBookingService bookingService,
            AppointmentCancellationService cancellationService,
            AppointmentRescheduleService rescheduleService,
            CheckInService checkInService,
            @Autowired(required = false) AppointmentSearchService searchService,
            @Autowired(required = false) AppointmentRepository appointmentRepository,
            CheckInCenterAuthorizationService checkInAuthorization
    ) {
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
        this.rescheduleService = rescheduleService;
        this.checkInService = checkInService;
        this.searchService = searchService;
        this.appointmentRepository = appointmentRepository;
        this.checkInAuthorization = checkInAuthorization;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @RequestParam(value = "citizenId", required = false) Long citizenId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        if (citizenId != null && !citizenId.equals(user.getUserId())) {
            throw new AccessDeniedException("Citizens may only access their own appointments");
        }
        Long effectiveCitizenId = user.getUserId();
        if (appointmentRepository == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Appointment> appointments = appointmentRepository.findByCitizenIdOrderByBookedAtDesc(effectiveCitizenId);
        List<AppointmentResponse> responseList = appointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<AvailableSessionResponse>> searchAvailableAppointments(
            @RequestParam(value = "citizenId", required = false) Long citizenId,
            @RequestParam("campaignId") Long campaignId,
            @RequestParam(value = "centerId", required = false) Long centerId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        if (citizenId != null && !citizenId.equals(user.getUserId())) {
            throw new AccessDeniedException("Citizens may only search with their own identity");
        }
        if (campaignId != null && campaignId <= 0) {
            throw new IllegalArgumentException("Campaign ID must be a positive integer");
        }
        if (centerId != null && centerId <= 0) {
            throw new IllegalArgumentException("Center ID must be a positive integer");
        }
        Long effectiveCitizenId = user.getUserId();
        if (searchService == null) {
            return ResponseEntity.ok(List.of());
        }
        List<AvailableSessionSummary> summaries = searchService.searchAvailableSessions(
                effectiveCitizenId, campaignId, centerId, fromDate, toDate
        );
        List<AvailableSessionResponse> responseList = summaries.stream()
                .map(AvailableSessionResponse::fromSummary)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody AppointmentBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        if (request.citizenId() != null && !request.citizenId().equals(user.getUserId())) {
            throw new AccessDeniedException("Citizens may only book appointments for themselves");
        }
        Long effectiveCitizenId = user.getUserId();
        Appointment appointment = bookingService.bookAppointment(
                effectiveCitizenId,
                request.campaignId(),
                request.slotId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.fromEntity(appointment));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AppointmentCancellationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        boolean isStaffOverride = (request != null && Boolean.TRUE.equals(request.isStaffOverride()));
        String staffReason = (request != null) ? request.staffReason() : null;

        Long effectiveCitizenId;
        if (isStaffOverride) {
            validateStaffOverrideAuthority();
            effectiveCitizenId = (request != null) ? request.requestingCitizenId() : null;
        } else {
            validateCitizenAuthority(user);
            Long requestingCitizenId = (request != null) ? request.requestingCitizenId() : null;
            if (requestingCitizenId != null && !requestingCitizenId.equals(user.getUserId())) {
                throw new AccessDeniedException("Citizens may only cancel their own appointments");
            }
            effectiveCitizenId = user.getUserId();
            if (appointmentRepository != null) {
                appointmentRepository.findById(id).ifPresent(appt -> {
                    if (!user.getUserId().equals(appt.getCitizenId())) {
                        throw new AccessDeniedException("Citizen ID " + user.getUserId() + " is not authorized to modify appointment " + id);
                    }
                });
            }
        }

        Appointment cancelled = cancellationService.cancelAppointment(
                id,
                effectiveCitizenId,
                isStaffOverride,
                staffReason,
                Instant.now()
        );
        return ResponseEntity.ok(AppointmentResponse.fromEntity(cancelled));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('CITIZEN', 'ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable("id") Long id,
            @Valid @RequestBody AppointmentRescheduleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPrincipal user = getAuthenticatedPrincipal(principal);
        boolean isStaffOverride = Boolean.TRUE.equals(request.isStaffOverride());

        Long effectiveCitizenId;
        if (isStaffOverride) {
            validateStaffOverrideAuthority();
            effectiveCitizenId = request.requestingCitizenId();
        } else {
            validateCitizenAuthority(user);
            if (request.requestingCitizenId() != null && !request.requestingCitizenId().equals(user.getUserId())) {
                throw new AccessDeniedException("Citizens may only reschedule their own appointments");
            }
            effectiveCitizenId = user.getUserId();
            if (appointmentRepository != null) {
                appointmentRepository.findById(id).ifPresent(appt -> {
                    if (!user.getUserId().equals(appt.getCitizenId())) {
                        throw new AccessDeniedException("Citizen ID " + user.getUserId() + " is not authorized to modify appointment " + id);
                    }
                });
            }
        }

        Appointment rescheduled = rescheduleService.rescheduleAppointment(
                id,
                effectiveCitizenId,
                request.newSlotId(),
                isStaffOverride,
                request.staffReason(),
                Instant.now()
        );
        return ResponseEntity.ok(AppointmentResponse.fromEntity(rescheduled));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<AppointmentResponse> checkIn(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CheckInRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireCheckInAppointmentAccess(principal, id);
        boolean isStaffOverride = (request != null && Boolean.TRUE.equals(request.isStaffOverride()));
        String staffReason = (request != null) ? request.staffReason() : null;

        Appointment checkedIn = checkInService.checkIn(id, isStaffOverride, staffReason, Instant.now());
        return ResponseEntity.ok(AppointmentResponse.fromEntity(checkedIn));
    }

    @PostMapping("/{id}/reverse-check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<AppointmentResponse> reverseCheckIn(
            @PathVariable("id") Long id,
            @Valid @RequestBody CheckInReversalRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireCheckInAppointmentAccess(principal, id);
        Appointment reversed = checkInService.reverseCheckIn(id, request.reason());
        return ResponseEntity.ok(AppointmentResponse.fromEntity(reversed));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER')")
    public ResponseEntity<AppointmentResponse> markNoShow(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireCheckInAppointmentAccess(principal, id);
        Appointment noShow = checkInService.markNoShow(id, Instant.now());
        return ResponseEntity.ok(AppointmentResponse.fromEntity(noShow));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'VACCINATOR')")
    public ResponseEntity<List<AppointmentResponse>> getVaccinatorQueue(
            @RequestParam("slotId") Long slotId,
            Authentication authentication,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireCheckInSlotAccess(principal, slotId);
        boolean canManageArrivals = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_CENTER_MANAGER".equals(authority.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        List<Appointment> queue = canManageArrivals
                ? checkInService.getArrivalQueueForSlot(slotId)
                : checkInService.getVaccinatorQueueForSlot(slotId);
        List<AppointmentResponse> responseList = queue.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    private void requireCheckInAppointmentAccess(UserPrincipal principal, Long appointmentId) {
        if (checkInAuthorization != null) {
            checkInAuthorization.requireAppointmentAccess(getAuthenticatedPrincipal(principal), appointmentId);
        }
    }

    private void requireCheckInSlotAccess(UserPrincipal principal, Long slotId) {
        if (checkInAuthorization != null) {
            checkInAuthorization.requireSlotAccess(getAuthenticatedPrincipal(principal), slotId);
        }
    }

    private UserPrincipal getAuthenticatedPrincipal(UserPrincipal principal) {
        if (principal != null) {
            return principal;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        throw new AccessDeniedException("Authentication required");
    }

    private void validateCitizenAuthority(UserPrincipal user) {
        if (user == null || (!user.hasRole("ROLE_CITIZEN") && !user.hasRole("CITIZEN"))) {
            throw new AccessDeniedException("Citizen role required for self-service appointment modification");
        }
    }

    private void validateStaffOverrideAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Staff override requires an authenticated staff role");
        }
        boolean hasStaffRole = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_CENTER_MANAGER".equals(a.getAuthority()) ||
                "ROLE_SUPER_ADMIN".equals(a.getAuthority()) ||
                "ADMIN".equals(a.getAuthority()) ||
                "CENTER_MANAGER".equals(a.getAuthority()) ||
                "SUPER_ADMIN".equals(a.getAuthority())
        );
        if (!hasStaffRole) {
            throw new AccessDeniedException("Staff override requires an authorized staff role (ADMIN, CENTER_MANAGER, SUPER_ADMIN)");
        }
    }
}
