package com.se2030.vaccinationportal.appointment.booking.web;

import com.se2030.vaccinationportal.appointment.web.AppointmentExceptionHandler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;
import com.se2030.vaccinationportal.screening.api.eligibility.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentBookingRequest;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentCancellationRequest;
import com.se2030.vaccinationportal.appointment.booking.dto.AppointmentRescheduleRequest;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.booking.service.AppointmentRescheduleService;
import com.se2030.vaccinationportal.appointment.query.service.AppointmentSearchService;
import com.se2030.vaccinationportal.appointment.checkin.service.CheckInService;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotAvailabilityPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestEmailVerificationStatusReaderDouble;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc integration tests for {@link AppointmentController} and {@link AppointmentExceptionHandler}.
 * Verifies success paths and exception HTTP status mappings (400, 403, 404, 409).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentControllerTests.TestConfig.class)
class AppointmentControllerTests {

    @Autowired
    private TestEmailVerificationStatusReaderDouble emailVerificationStatusReaderDouble;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEmailVerificationStatusReaderDouble emailVerificationStatusReader() {
            return new TestEmailVerificationStatusReaderDouble();
        }

        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }

        @Bean
        public TestSlotCapacityPortDouble slotCapacityPort() {
            return new TestSlotCapacityPortDouble();
        }

        @Bean
        @org.springframework.context.annotation.Primary
        public TestSlotAvailabilityPortDouble slotAvailabilityPort() {
            return new TestSlotAvailabilityPortDouble();
        }
    }

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private AppointmentCancellationService cancellationService;

    @Autowired
    private AppointmentRescheduleService rescheduleService;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private AppointmentSearchService searchService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    @Autowired
    private TestSlotAvailabilityPortDouble availabilityPortDouble;

    private void authenticate(Long userId, String role) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                "user" + userId + "@portal.test",
                null,
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @BeforeEach
    void setUp() {
        emailVerificationStatusReaderDouble.clear();
        emailVerificationStatusReaderDouble.markVerified(1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L, 1011L);
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        availabilityPortDouble.clear();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);

        AppointmentController appointmentController = new AppointmentController(
                bookingService, cancellationService, rescheduleService, checkInService, searchService, appointmentRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new AppointmentExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        availabilityPortDouble.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyAppointments_returns200OkWithCitizenAppointments() throws Exception {
        Long citizenId = 1011L;
        Long campaignId = 2001L;
        Long slotId = 4010L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);

        bookingService.bookAppointment(citizenId, campaignId, slotId);

        mockMvc.perform(get("/api/v1/appointments/mine").param("citizenId", citizenId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].citizenId", is(citizenId.intValue())))
                .andExpect(jsonPath("$[0].status", is("CONFIRMED")));
    }

    @Test
    void searchAvailableAppointments_returns200OkWithAvailableSessions() throws Exception {
        Long citizenId = 1010L;
        Long campaignId = 2001L;
        Long centerId = 5001L;
        Instant now = Instant.now();

        authenticate(citizenId, "ROLE_CITIZEN");
        AvailableSessionSummary s1 = new AvailableSessionSummary(
                6001L, 7001L, campaignId, centerId, "Center A", "123 Main St",
                now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(1800), 5
        );
        availabilityPortDouble.addSession(s1);

        mockMvc.perform(get("/api/v1/appointments/search")
                        .param("citizenId", citizenId.toString())
                        .param("campaignId", campaignId.toString())
                        .param("centerId", centerId.toString())
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionId", is(6001)))
                .andExpect(jsonPath("$[0].slotId", is(7001)))
                .andExpect(jsonPath("$[0].centerName", is("Center A")));
    }

    @Test
    void searchAvailableAppointments_negativeCenterId_returns400BadRequest() throws Exception {
        Long citizenId = 1001L;
        authenticate(citizenId, "ROLE_CITIZEN");

        mockMvc.perform(get("/api/v1/appointments/search")
                        .param("citizenId", citizenId.toString())
                        .param("campaignId", "8001")
                        .param("centerId", "-9")
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    void searchAvailableAppointments_negativeCampaignId_returns400BadRequest() throws Exception {
        Long citizenId = 1001L;
        authenticate(citizenId, "ROLE_CITIZEN");

        mockMvc.perform(get("/api/v1/appointments/search")
                        .param("citizenId", citizenId.toString())
                        .param("campaignId", "-1")
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    void bookAppointment_success_returns201Created() throws Exception {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long slotId = 4001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);

        AppointmentBookingRequest request = new AppointmentBookingRequest(citizenId, campaignId, slotId);

        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citizenId", is(citizenId.intValue())))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void bookAppointment_unverifiedCitizen_returns403WithEmailVerificationRequired() throws Exception {
        Long unverifiedCitizen = 1999L;
        Long campaignId = 2001L;
        Long slotId = 4001L;

        authenticate(unverifiedCitizen, "ROLE_CITIZEN");
        // The request would otherwise succeed: eligible and capacity available.
        eligibilityPortDouble.setEligibility(unverifiedCitizen, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        int capacityBefore = slotCapacityPortDouble.getAvailableCapacity(slotId);

        AppointmentBookingRequest request = new AppointmentBookingRequest(unverifiedCitizen, campaignId, slotId);

        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("EMAIL_VERIFICATION_REQUIRED")))
                .andExpect(jsonPath("$.message", containsString("Verify your email")));

        assertEquals(capacityBefore, slotCapacityPortDouble.getAvailableCapacity(slotId),
                "Rejected booking must not consume capacity");
    }

    @Test
    void bookAppointment_notEligible_returns403Forbidden() throws Exception {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long slotId = 4001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.NOT_ELIGIBLE);

        AppointmentBookingRequest request = new AppointmentBookingRequest(citizenId, campaignId, slotId);

        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("not eligible")));
    }

    @Test
    void bookAppointment_duplicateAppointment_returns409Conflict() throws Exception {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long slotId = 4001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);

        // Book first appointment
        bookingService.bookAppointment(citizenId, campaignId, slotId);

        // Attempt second booking for same campaign
        AppointmentBookingRequest request = new AppointmentBookingRequest(citizenId, campaignId, slotId);

        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("already has an active appointment")));
    }

    @Test
    void bookAppointment_slotFull_returns409Conflict() throws Exception {
        Long citizenId = 1004L;
        Long campaignId = 2001L;
        Long slotId = 4002L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setAvailableCapacity(slotId, 0);

        AppointmentBookingRequest request = new AppointmentBookingRequest(citizenId, campaignId, slotId);

        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONFLICT")));
    }

    @Test
    void cancelAppointment_success_returns200Ok() throws Exception {
        Long citizenId = 1005L;
        Long campaignId = 2001L;
        Long slotId = 4003L;
        Instant now = Instant.now();
        Instant slotStart = now.plus(48, ChronoUnit.HOURS);

        authenticate(citizenId, "ROLE_CITIZEN");
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);

        AppointmentCancellationRequest request = new AppointmentCancellationRequest(citizenId, false, null);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void cancelAppointment_requiresStaffOverride_returns403Forbidden() throws Exception {
        Long citizenId = 1006L;
        Long campaignId = 2001L;
        Long slotId = 4004L;
        Instant now = Instant.now();
        Instant slotStart = now.plus(12, ChronoUnit.HOURS); // Inside 24h window

        authenticate(citizenId, "ROLE_CITIZEN");
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);

        AppointmentCancellationRequest request = new AppointmentCancellationRequest(citizenId, false, null);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    void rescheduleAppointment_success_returns200Ok() throws Exception {
        Long citizenId = 1007L;
        Long campaignId = 2001L;
        Long oldSlot = 4005L;
        Long newSlot = 4006L;
        Instant now = Instant.now();
        Instant slotStart = now.plus(48, ChronoUnit.HOURS);

        authenticate(citizenId, "ROLE_CITIZEN");
        slotCapacityPortDouble.setAvailableCapacity(oldSlot, 5);
        slotCapacityPortDouble.setAvailableCapacity(newSlot, 5);
        slotCapacityPortDouble.setSlotStartTime(oldSlot, slotStart);
        slotCapacityPortDouble.setSlotStartTime(newSlot, slotStart);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, oldSlot);

        AppointmentRescheduleRequest request = new AppointmentRescheduleRequest(citizenId, newSlot, false, null);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId", is(newSlot.intValue())));
    }

    @Test
    void checkIn_success_returns200Ok() throws Exception {
        Long citizenId = 1008L;
        Long campaignId = 2001L;
        Long slotId = 4007L;
        Instant now = Instant.now();

        authenticate(999L, "ROLE_CENTER_MANAGER");
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, now);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CHECKED_IN")));
    }

    @Test
    void reverseCheckIn_success_returns200Ok() throws Exception {
        Long citizenId = 1009L;
        Long campaignId = 2001L;
        Long slotId = 4008L;
        Instant now = Instant.now();

        authenticate(999L, "ROLE_CENTER_MANAGER");
        slotCapacityPortDouble.setAvailableCapacity(slotId, 5);
        slotCapacityPortDouble.setSlotStartTime(slotId, now);

        Appointment appt = bookingService.bookAppointment(citizenId, campaignId, slotId);
        checkInService.checkIn(appt.getId(), now);

        mockMvc.perform(post("/api/v1/appointments/" + appt.getId() + "/reverse-check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void getVaccinatorQueue_returns200Ok() throws Exception {
        Long slotId = 4009L;

        authenticate(999L, "ROLE_CENTER_MANAGER");
        mockMvc.perform(get("/api/v1/appointments/queue").param("slotId", slotId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAppointment_notFound_returns404NotFound() throws Exception {
        authenticate(999L, "ROLE_CENTER_MANAGER");
        mockMvc.perform(post("/api/v1/appointments/999999/reverse-check-in"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }
}
