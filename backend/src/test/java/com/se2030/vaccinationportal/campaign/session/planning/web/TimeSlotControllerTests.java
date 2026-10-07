package com.se2030.vaccinationportal.campaign.session.planning.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.session.planning.service.TimeSlotService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class TimeSlotControllerTests {

    private MockMvc mockMvc;

    @Mock
    private TimeSlotService timeSlotService;

    private VaccinationSession sampleSession;
    private TimeSlot sampleSlot1;
    private TimeSlot sampleSlot2;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TimeSlotController(timeSlotService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .setValidator(validator)
                .build();

        VaccineType sampleVaccine = new VaccineType("VAX-01", "COVID-19", "dose", true);
        TargetGroup sampleTarget = new TargetGroup("TG-01", "General", true);
        VaccinationCampaign sampleCampaign = new VaccinationCampaign(
                "CMP-2026", "National COVID 2026", "Description",
                sampleVaccine, sampleTarget,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-30T23:59:59Z")
        );
        ReflectionTestUtils.setField(sampleCampaign, "id", 100L);

        VaccinationCenter sampleCenter = new VaccinationCenter("CTR-01", "Downtown Center", "123 Main St", "555-0100", CenterStatus.ACTIVE, 50, 100, "UTC");
        ReflectionTestUtils.setField(sampleCenter, "id", 200L);

        sampleSession = new VaccinationSession(
                sampleCampaign,
                sampleCenter,
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T12:00:00Z"),
                Instant.parse("2026-09-01T07:00:00Z"),
                "UTC"
        );
        ReflectionTestUtils.setField(sampleSession, "id", 300L);

        sampleSlot1 = new TimeSlot(
                sampleSession,
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T09:00:00Z"),
                20
        );
        ReflectionTestUtils.setField(sampleSlot1, "id", 1001L);
        ReflectionTestUtils.setField(sampleSlot1, "reservedCount", 5);

        sampleSlot2 = new TimeSlot(
                sampleSession,
                Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                25
        );
        ReflectionTestUtils.setField(sampleSlot2, "id", 1002L);
        ReflectionTestUtils.setField(sampleSlot2, "reservedCount", 0);
    }

    // =========================================================================
    // 1. GET /api/v1/sessions/{sessionId}/time-slots
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/sessions/{sessionId}/time-slots")
    class GetTimeSlots {

        @Test
        @DisplayName("Returns 200 OK with mapped ordered slots")
        void listTimeSlots_success() throws Exception {
            when(timeSlotService.getSlotsForSession(300L)).thenReturn(List.of(sampleSlot1, sampleSlot2));

            mockMvc.perform(get("/api/v1/sessions/300/time-slots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1001)))
                    .andExpect(jsonPath("$[0].sessionId", is(300)))
                    .andExpect(jsonPath("$[0].startTime", is("2026-09-01T08:00:00Z")))
                    .andExpect(jsonPath("$[0].endTime", is("2026-09-01T09:00:00Z")))
                    .andExpect(jsonPath("$[0].capacity", is(20)))
                    .andExpect(jsonPath("$[0].reservedCount", is(5)))
                    .andExpect(jsonPath("$[0].availableCapacity", is(15)))
                    .andExpect(jsonPath("$[1].id", is(1002)))
                    .andExpect(jsonPath("$[1].sessionId", is(300)))
                    .andExpect(jsonPath("$[1].capacity", is(25)))
                    .andExpect(jsonPath("$[1].reservedCount", is(0)))
                    .andExpect(jsonPath("$[1].availableCapacity", is(25)));

            verify(timeSlotService).getSlotsForSession(300L);
        }

        @Test
        @DisplayName("Returns 200 OK with empty array when no slots exist")
        void listTimeSlots_empty() throws Exception {
            when(timeSlotService.getSlotsForSession(300L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/sessions/300/time-slots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // 2. POST /api/v1/sessions/{sessionId}/time-slots
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/time-slots")
    class CreateTimeSlotEndpoint {

        @Test
        @DisplayName("Returns 201 Created with Location header and TimeSlotResponse on valid payload")
        void createSlot_success() throws Exception {
            String payload = """
                    {
                        "startTime": "2026-09-01T08:00:00Z",
                        "endTime": "2026-09-01T09:00:00Z",
                        "capacity": 20
                    }
                    """;

            when(timeSlotService.createSlot(eq(300L), any(Instant.class), any(Instant.class), eq(20)))
                    .thenReturn(sampleSlot1);

            mockMvc.perform(post("/api/v1/sessions/300/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/sessions/300/time-slots/1001"))
                    .andExpect(jsonPath("$.id", is(1001)))
                    .andExpect(jsonPath("$.sessionId", is(300)))
                    .andExpect(jsonPath("$.startTime", is("2026-09-01T08:00:00Z")))
                    .andExpect(jsonPath("$.endTime", is("2026-09-01T09:00:00Z")))
                    .andExpect(jsonPath("$.capacity", is(20)))
                    .andExpect(jsonPath("$.reservedCount", is(5)))
                    .andExpect(jsonPath("$.availableCapacity", is(15)));

            verify(timeSlotService).createSlot(
                    300L,
                    Instant.parse("2026-09-01T08:00:00Z"),
                    Instant.parse("2026-09-01T09:00:00Z"),
                    20
            );
        }

        @Test
        @DisplayName("Returns 400 Bad Request when capacity is null or less than 1")
        void createSlot_invalidCapacity_rejected() throws Exception {
            String invalidPayload = """
                    {
                        "startTime": "2026-09-01T08:00:00Z",
                        "endTime": "2026-09-01T09:00:00Z",
                        "capacity": 0
                    }
                    """;

            mockMvc.perform(post("/api/v1/sessions/300/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Validation failed")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when start or end time is missing")
        void createSlot_missingTimes_rejected() throws Exception {
            String invalidPayload = """
                    {
                        "capacity": 20
                    }
                    """;

            mockMvc.perform(post("/api/v1/sessions/300/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Validation failed")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when service throws IllegalArgumentException for window bounds")
        void createSlot_serviceIllegalArgument_returns400() throws Exception {
            String payload = """
                    {
                        "startTime": "2026-09-01T07:00:00Z",
                        "endTime": "2026-09-01T09:00:00Z",
                        "capacity": 20
                    }
                    """;

            when(timeSlotService.createSlot(eq(300L), any(Instant.class), any(Instant.class), eq(20)))
                    .thenThrow(new IllegalArgumentException("Slot interval must fall within session operating window"));

            mockMvc.perform(post("/api/v1/sessions/300/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Slot interval must fall within session operating window")));
        }

        @Test
        @DisplayName("Returns 409 Conflict when service throws IllegalStateException for capacity plan or occupancy breach")
        void createSlot_serviceIllegalState_returns409() throws Exception {
            String payload = """
                    {
                        "startTime": "2026-09-01T08:00:00Z",
                        "endTime": "2026-09-01T09:00:00Z",
                        "capacity": 100
                    }
                    """;

            when(timeSlotService.createSlot(eq(300L), any(Instant.class), any(Instant.class), eq(100)))
                    .thenThrow(new IllegalStateException("Total slot capacity (120) cannot exceed approved session capacity (50)"));

            mockMvc.perform(post("/api/v1/sessions/300/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("Total slot capacity (120) cannot exceed approved session capacity (50)")));
        }

        @Test
        @DisplayName("Returns 409 Conflict when session is in non-configurable lifecycle status")
        void createSlot_sessionInNonConfigurableStatus_returns409() throws Exception {
            String payload = """
                    {
                        "startTime": "2026-09-01T08:00:00Z",
                        "endTime": "2026-09-01T09:00:00Z",
                        "capacity": 20
                    }
                    """;

            when(timeSlotService.createSlot(eq(300L), any(Instant.class), any(Instant.class), eq(20)))
                    .thenThrow(new IllegalStateException("Cannot configure time slots for session in IN_PROGRESS lifecycle status"));

            mockMvc.perform(post("/api/v1/sessions/300/time-slots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")))
                    .andExpect(jsonPath("$.message", is("Cannot configure time slots for session in IN_PROGRESS lifecycle status")));
        }
    }

    // =========================================================================
    // 3. PUT /api/v1/sessions/{sessionId}/time-slots/{slotId}/capacity
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/sessions/{sessionId}/time-slots/{slotId}/capacity")
    class UpdateTimeSlotCapacityEndpoint {

        @Test
        @DisplayName("Returns 200 OK with updated TimeSlotResponse on valid capacity update")
        void updateCapacity_success() throws Exception {
            String payload = """
                    {
                        "capacity": 30
                    }
                    """;
            TimeSlot updatedSlot = new TimeSlot(
                    sampleSession,
                    sampleSlot1.getStartTime(),
                    sampleSlot1.getEndTime(),
                    30
            );
            ReflectionTestUtils.setField(updatedSlot, "id", 1001L);
            ReflectionTestUtils.setField(updatedSlot, "reservedCount", 5);

            when(timeSlotService.updateSlotCapacity(300L, 1001L, 30))
                    .thenReturn(updatedSlot);

            mockMvc.perform(put("/api/v1/sessions/300/time-slots/1001/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1001)))
                    .andExpect(jsonPath("$.capacity", is(30)))
                    .andExpect(jsonPath("$.reservedCount", is(5)))
                    .andExpect(jsonPath("$.availableCapacity", is(25)));

            verify(timeSlotService).updateSlotCapacity(300L, 1001L, 30);
        }

        @Test
        @DisplayName("Returns 400 Bad Request when capacity is missing or less than 1")
        void updateCapacity_invalidCapacity_rejected() throws Exception {
            String invalidPayload = """
                    {
                        "capacity": 0
                    }
                    """;

            mockMvc.perform(put("/api/v1/sessions/300/time-slots/1001/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Validation failed")));
        }

        @Test
        @DisplayName("Returns 409 Conflict when attempting to reduce capacity below existing reserved bookings")
        void updateCapacity_belowReservedFloor_returns409() throws Exception {
            String payload = """
                    {
                        "capacity": 4
                    }
                    """;

            when(timeSlotService.updateSlotCapacity(300L, 1001L, 4))
                    .thenThrow(new IllegalStateException("Cannot reduce slot capacity (4) below existing reserved bookings (5)"));

            mockMvc.perform(put("/api/v1/sessions/300/time-slots/1001/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", is("Cannot reduce slot capacity (4) below existing reserved bookings (5)")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request on slot/session mismatch (slot does not belong to session)")
        void updateCapacity_slotSessionMismatch_returns400() throws Exception {
            String payload = """
                    {
                        "capacity": 30
                    }
                    """;

            when(timeSlotService.updateSlotCapacity(300L, 9999L, 30))
                    .thenThrow(new IllegalArgumentException("TimeSlot 9999 does not belong to session 300"));

            mockMvc.perform(put("/api/v1/sessions/300/time-slots/9999/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("TimeSlot 9999 does not belong to session 300")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when slot is not found")
        void updateCapacity_slotNotFound_returns400() throws Exception {
            String payload = """
                    {
                        "capacity": 30
                    }
                    """;

            when(timeSlotService.updateSlotCapacity(300L, 9999L, 30))
                    .thenThrow(new IllegalArgumentException("TimeSlot not found with ID: 9999"));

            mockMvc.perform(put("/api/v1/sessions/300/time-slots/9999/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("TimeSlot not found with ID: 9999")));
        }

        @Test
        @DisplayName("Returns 409 Conflict when session is in non-configurable lifecycle status during capacity update")
        void updateCapacity_sessionInNonConfigurableStatus_returns409() throws Exception {
            String payload = """
                    {
                        "capacity": 30
                    }
                    """;

            when(timeSlotService.updateSlotCapacity(300L, 1001L, 30))
                    .thenThrow(new IllegalStateException("Cannot configure time slots for session in COMPLETED lifecycle status"));

            mockMvc.perform(put("/api/v1/sessions/300/time-slots/1001/capacity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")))
                    .andExpect(jsonPath("$.message", is("Cannot configure time slots for session in COMPLETED lifecycle status")));
        }
    }
}
