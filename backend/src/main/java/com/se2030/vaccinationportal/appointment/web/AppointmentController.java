package com.se2030.vaccinationportal.appointment.web;

import com.se2030.vaccinationportal.appointment.dependency.AvailableSessionSummary;
import com.se2030.vaccinationportal.appointment.dto.AppointmentBookingRequest;
import com.se2030.vaccinationportal.appointment.dto.AppointmentCancellationRequest;
import com.se2030.vaccinationportal.appointment.dto.AppointmentRescheduleRequest;
import com.se2030.vaccinationportal.appointment.dto.AppointmentResponse;
import com.se2030.vaccinationportal.appointment.dto.AvailableSessionResponse;
import com.se2030.vaccinationportal.appointment.dto.CheckInRequest;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.service.AppointmentRescheduleService;
import com.se2030.vaccinationportal.appointment.service.AppointmentSearchService;
import com.se2030.vaccinationportal.appointment.service.CheckInService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public AppointmentController(
            AppointmentBookingService bookingService,
            AppointmentCancellationService cancellationService,
            AppointmentRescheduleService rescheduleService,
            CheckInService checkInService
    ) {
        this(bookingService, cancellationService, rescheduleService, checkInService, null, null);
    }

    @Autowired
    public AppointmentController(
            AppointmentBookingService bookingService,
            AppointmentCancellationService cancellationService,
            AppointmentRescheduleService rescheduleService,
            CheckInService checkInService,
            @Autowired(required = false) AppointmentSearchService searchService,
            @Autowired(required = false) AppointmentRepository appointmentRepository
    ) {
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
        this.rescheduleService = rescheduleService;
        this.checkInService = checkInService;
        this.searchService = searchService;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(@RequestParam("citizenId") Long citizenId) {
        if (appointmentRepository == null || citizenId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Appointment> appointments = appointmentRepository.findByCitizenIdOrderByBookedAtDesc(citizenId);
        List<AppointmentResponse> responseList = appointments.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<AvailableSessionResponse>> searchAvailableAppointments(
            @RequestParam("citizenId") Long citizenId,
            @RequestParam("campaignId") Long campaignId,
            @RequestParam(value = "centerId", required = false) Long centerId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        if (searchService == null) {
            return ResponseEntity.ok(List.of());
        }
        List<AvailableSessionSummary> summaries = searchService.searchAvailableSessions(
                citizenId, campaignId, centerId, fromDate, toDate
        );
        List<AvailableSessionResponse> responseList = summaries.stream()
                .map(AvailableSessionResponse::fromSummary)
                .toList();
        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/book")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentBookingRequest request) {
        Appointment appointment = bookingService.bookAppointment(
                request.citizenId(),
                request.campaignId(),
                request.slotId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.fromEntity(appointment));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AppointmentCancellationRequest request
    ) {
        Long requestingCitizenId = (request != null) ? request.requestingCitizenId() : null;
        boolean isStaffOverride = (request != null && Boolean.TRUE.equals(request.isStaffOverride()));
        String staffReason = (request != null) ? request.staffReason() : null;

        Appointment cancelled = cancellationService.cancelAppointment(
                id,
                requestingCitizenId,
                isStaffOverride,
                staffReason,
                Instant.now()
        );
        return ResponseEntity.ok(AppointmentResponse.fromEntity(cancelled));
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable("id") Long id,
            @Valid @RequestBody AppointmentRescheduleRequest request
    ) {
        boolean isStaffOverride = Boolean.TRUE.equals(request.isStaffOverride());

        Appointment rescheduled = rescheduleService.rescheduleAppointment(
                id,
                request.requestingCitizenId(),
                request.newSlotId(),
                isStaffOverride,
                request.staffReason(),
                Instant.now()
        );
        return ResponseEntity.ok(AppointmentResponse.fromEntity(rescheduled));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<AppointmentResponse> checkIn(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CheckInRequest request
    ) {
        boolean isStaffOverride = (request != null && Boolean.TRUE.equals(request.isStaffOverride()));
        String staffReason = (request != null) ? request.staffReason() : null;

        Appointment checkedIn = checkInService.checkIn(id, isStaffOverride, staffReason, Instant.now());
        return ResponseEntity.ok(AppointmentResponse.fromEntity(checkedIn));
    }

    @PostMapping("/{id}/reverse-check-in")
    public ResponseEntity<AppointmentResponse> reverseCheckIn(@PathVariable("id") Long id) {
        Appointment reversed = checkInService.reverseCheckIn(id);
        return ResponseEntity.ok(AppointmentResponse.fromEntity(reversed));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<AppointmentResponse> markNoShow(@PathVariable("id") Long id) {
        Appointment noShow = checkInService.markNoShow(id, Instant.now());
        return ResponseEntity.ok(AppointmentResponse.fromEntity(noShow));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<AppointmentResponse>> getVaccinatorQueue(@RequestParam("slotId") Long slotId) {
        List<Appointment> queue = checkInService.getVaccinatorQueueForSlot(slotId);
        List<AppointmentResponse> responseList = queue.stream()
                .map(AppointmentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responseList);
    }
}
