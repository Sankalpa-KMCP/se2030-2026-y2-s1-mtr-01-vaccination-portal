package com.se2030.vaccinationportal.appointment.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.dto.AppointmentBookingRequest;
import com.se2030.vaccinationportal.appointment.dto.AppointmentCancellationRequest;
import com.se2030.vaccinationportal.appointment.dto.AppointmentRescheduleRequest;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.service.AppointmentBookingService;
import com.se2030.vaccinationportal.appointment.service.AppointmentCancellationService;
import com.se2030.vaccinationportal.appointment.service.AppointmentRescheduleService;
import com.se2030.vaccinationportal.appointment.service.CheckInService;
import com.se2030.vaccinationportal.appointment.service.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.service.TestSlotCapacityPortDouble;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }

        @Bean
        public TestSlotCapacityPortDouble slotCapacityPort() {
            return new TestSlotCapacityPortDouble();
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
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);

        AppointmentController appointmentController = new AppointmentController(
                bookingService, cancellationService, rescheduleService, checkInService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new AppointmentExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @Test
    void bookAppointment_success_returns201Created() throws Exception {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long slotId = 4001L;

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
    void bookAppointment_notEligible_returns403Forbidden() throws Exception {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long slotId = 4001L;

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

        mockMvc.perform(get("/api/v1/appointments/queue").param("slotId", slotId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAppointment_notFound_returns404NotFound() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/999999/reverse-check-in"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }
}
