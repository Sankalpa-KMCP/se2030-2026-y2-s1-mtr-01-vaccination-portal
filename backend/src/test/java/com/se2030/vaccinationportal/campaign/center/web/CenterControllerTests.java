package com.se2030.vaccinationportal.campaign.center.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.service.CenterClosureService;
import com.se2030.vaccinationportal.campaign.center.service.OperatingHoursService;
import com.se2030.vaccinationportal.campaign.center.service.VaccinationCenterService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class CenterControllerTests {

    private MockMvc mockMvc;

    @Mock
    private VaccinationCenterService centerService;

    @Mock
    private OperatingHoursService operatingHoursService;

    @Mock
    private CenterClosureService closureService;

    private VaccinationCenter sampleCenter;
    private OperatingHours sampleOperatingHours;
    private CenterClosure sampleClosure;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CenterController(centerService, operatingHoursService, closureService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .setValidator(validator)
                .build();

        sampleCenter = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Vaccination Center",
                "123 Main Street, Colombo 01",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(sampleCenter, "id", 100L);
        ReflectionTestUtils.setField(sampleCenter, "createdAt", Instant.parse("2026-08-01T08:00:00Z"));
        ReflectionTestUtils.setField(sampleCenter, "updatedAt", Instant.parse("2026-08-01T08:00:00Z"));

        sampleOperatingHours = new OperatingHours(
                sampleCenter,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(sampleOperatingHours, "id", 200L);
        ReflectionTestUtils.setField(sampleOperatingHours, "createdAt", Instant.parse("2026-08-01T08:00:00Z"));
        ReflectionTestUtils.setField(sampleOperatingHours, "updatedAt", Instant.parse("2026-08-01T08:00:00Z"));

        sampleClosure = new CenterClosure(
                sampleCenter,
                Instant.parse("2026-10-01T08:00:00Z"),
                Instant.parse("2026-10-03T18:00:00Z"),
                "Facility electrical maintenance"
        );
        ReflectionTestUtils.setField(sampleClosure, "id", 300L);
        ReflectionTestUtils.setField(sampleClosure, "createdAt", Instant.parse("2026-08-01T08:00:00Z"));
        ReflectionTestUtils.setField(sampleClosure, "updatedAt", Instant.parse("2026-08-01T08:00:00Z"));
    }

    // ==========================================
    // 1. POST /api/v1/centers
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers creates center and returns 201 with Location header")
    void createCenterSuccess() throws Exception {
        String jsonPayload = """
                {
                    "code": "CTR-COLOMBO-01",
                    "name": "Colombo Central Vaccination Center",
                    "address": "123 Main Street, Colombo 01",
                    "contact": "+94 11 2345678",
                    "safeConcurrentOccupancy": 150,
                    "sessionServiceCeiling": 50,
                    "timezone": "Asia/Colombo"
                }
                """;

        when(centerService.createCenter(
                eq("CTR-COLOMBO-01"),
                eq("Colombo Central Vaccination Center"),
                eq("123 Main Street, Colombo 01"),
                eq("+94 11 2345678"),
                eq(150),
                eq(50),
                eq("Asia/Colombo")
        )).thenReturn(sampleCenter);

        mockMvc.perform(post("/api/v1/centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/centers/100"))
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.code", is("CTR-COLOMBO-01")))
                .andExpect(jsonPath("$.name", is("Colombo Central Vaccination Center")))
                .andExpect(jsonPath("$.address", is("123 Main Street, Colombo 01")))
                .andExpect(jsonPath("$.contact", is("+94 11 2345678")))
                .andExpect(jsonPath("$.timezone", is("Asia/Colombo")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.safeConcurrentOccupancy", is(150)))
                .andExpect(jsonPath("$.sessionServiceCeiling", is(50)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        verify(centerService).createCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Vaccination Center",
                "123 Main Street, Colombo 01",
                "+94 11 2345678",
                150,
                50,
                "Asia/Colombo"
        );
    }

    @Test
    @DisplayName("POST /api/v1/centers returns 400 when validation fails")
    void createCenterValidationError() throws Exception {
        String invalidPayload = """
                {
                    "code": "",
                    "name": "",
                    "address": "",
                    "contact": "",
                    "safeConcurrentOccupancy": -5,
                    "timezone": ""
                }
                """;

        mockMvc.perform(post("/api/v1/centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("POST /api/v1/centers returns 400 when invalid timezone provided")
    void createCenterInvalidTimezone() throws Exception {
        String jsonPayload = """
                {
                    "code": "CTR-COLOMBO-01",
                    "name": "Colombo Central Vaccination Center",
                    "address": "123 Main Street, Colombo 01",
                    "contact": "+94 11 2345678",
                    "safeConcurrentOccupancy": 150,
                    "timezone": "Invalid/Timezone"
                }
                """;

        when(centerService.createCenter(any(), any(), any(), any(), any(int.class), any(), eq("Invalid/Timezone")))
                .thenThrow(new IllegalArgumentException("Center scheduling timezone is invalid: Invalid/Timezone"));

        mockMvc.perform(post("/api/v1/centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("timezone is invalid")));
    }

    @Test
    @DisplayName("POST /api/v1/centers returns 400 when duplicate code exists")
    void createCenterDuplicateCode() throws Exception {
        String jsonPayload = """
                {
                    "code": "CTR-COLOMBO-01",
                    "name": "Colombo Central Vaccination Center",
                    "address": "123 Main Street, Colombo 01",
                    "contact": "+94 11 2345678",
                    "safeConcurrentOccupancy": 150,
                    "timezone": "Asia/Colombo"
                }
                """;

        when(centerService.createCenter(any(), any(), any(), any(), any(int.class), any(), any()))
                .thenThrow(new IllegalArgumentException("Vaccination center already exists with code: CTR-COLOMBO-01"));

        mockMvc.perform(post("/api/v1/centers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already exists with code")));
    }

    // ==========================================
    // 2. GET /api/v1/centers/{id}
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/centers/{id} returns 200 and CenterResponse")
    void getCenterByIdSuccess() throws Exception {
        when(centerService.getRequiredCenterById(100L)).thenReturn(sampleCenter);

        mockMvc.perform(get("/api/v1/centers/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.code", is("CTR-COLOMBO-01")))
                .andExpect(jsonPath("$.name", is("Colombo Central Vaccination Center")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{id} returns 404 when center does not exist")
    void getCenterByIdNotFound() throws Exception {
        when(centerService.getRequiredCenterById(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(get("/api/v1/centers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination center not found with ID: 999")));
    }

    // ==========================================
    // 3. GET /api/v1/centers
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/centers without filter returns 200 and all centers")
    void getCentersAll() throws Exception {
        when(centerService.getCenters(null)).thenReturn(List.of(sampleCenter));

        mockMvc.perform(get("/api/v1/centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(100)))
                .andExpect(jsonPath("$[0].code", is("CTR-COLOMBO-01")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/centers with status=ACTIVE returns 200 and filtered centers")
    void getCentersFiltered() throws Exception {
        when(centerService.getCenters(CenterStatus.ACTIVE)).thenReturn(List.of(sampleCenter));

        mockMvc.perform(get("/api/v1/centers").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("CTR-COLOMBO-01")));
    }

    @Test
    @DisplayName("GET /api/v1/centers with invalid status returns 400 Bad Request")
    void getCentersInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/centers").param("status", "UNKNOWN_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    // ==========================================
    // 4. PUT /api/v1/centers/{id}
    // ==========================================

    @Test
    @DisplayName("PUT /api/v1/centers/{id} updates center and returns 200")
    void updateCenterSuccess() throws Exception {
        String jsonPayload = """
                {
                    "name": "Colombo Central General Hospital",
                    "address": "456 Hospital Road, Colombo 01",
                    "contact": "+94 11 9998877",
                    "safeConcurrentOccupancy": 200,
                    "sessionServiceCeiling": 75
                }
                """;

        VaccinationCenter updated = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central General Hospital",
                "456 Hospital Road, Colombo 01",
                "+94 11 9998877",
                CenterStatus.ACTIVE,
                200,
                75,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(updated, "id", 100L);

        when(centerService.updateCenter(eq(100L), eq("Colombo Central General Hospital"), eq("456 Hospital Road, Colombo 01"), eq("+94 11 9998877"), eq(200), eq(75)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/centers/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.name", is("Colombo Central General Hospital")))
                .andExpect(jsonPath("$.safeConcurrentOccupancy", is(200)));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{id} returns 404 when center does not exist")
    void updateCenterNotFound() throws Exception {
        String jsonPayload = """
                {
                    "name": "Updated Center Name",
                    "address": "456 Hospital Road, Colombo 01",
                    "contact": "+94 11 9998877",
                    "safeConcurrentOccupancy": 200
                }
                """;

        when(centerService.updateCenter(eq(999L), any(), any(), any(), any(int.class), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(put("/api/v1/centers/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 5. POST /api/v1/centers/{id}/activate
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers/{id}/activate activates center and returns 200")
    void activateCenterSuccess() throws Exception {
        when(centerService.activateCenter(100L)).thenReturn(sampleCenter);

        mockMvc.perform(post("/api/v1/centers/100/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{id}/activate returns 404 when center does not exist")
    void activateCenterNotFound() throws Exception {
        when(centerService.activateCenter(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(post("/api/v1/centers/999/activate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 6. POST /api/v1/centers/{id}/deactivate
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers/{id}/deactivate deactivates center and returns 200")
    void deactivateCenterSuccess() throws Exception {
        VaccinationCenter deactivated = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Vaccination Center",
                "123 Main Street, Colombo 01",
                "+94 11 2345678",
                CenterStatus.INACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(deactivated, "id", 100L);

        when(centerService.deactivateCenter(100L)).thenReturn(deactivated);

        mockMvc.perform(post("/api/v1/centers/100/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{id}/deactivate returns 400 when blocking sessions exist")
    void deactivateCenterBlockingSessions() throws Exception {
        when(centerService.deactivateCenter(100L))
                .thenThrow(new IllegalArgumentException("Cannot deactivate vaccination center because 2 future or in-progress session(s) depend on it"));

        mockMvc.perform(post("/api/v1/centers/100/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("future or in-progress session(s) depend on it")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{id}/deactivate returns 404 when center does not exist")
    void deactivateCenterNotFound() throws Exception {
        when(centerService.deactivateCenter(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(post("/api/v1/centers/999/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 7. POST /api/v1/centers/{id}/operating-hours
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers/{id}/operating-hours creates shift and returns 201 with Location")
    void createOperatingHoursSuccess() throws Exception {
        String jsonPayload = """
                {
                    "dayOfWeek": "MONDAY",
                    "opening": "08:00:00",
                    "closing": "16:00:00",
                    "effectiveFrom": "2026-09-01",
                    "effectiveTo": "2026-12-31"
                }
                """;

        when(operatingHoursService.createOperatingHours(
                eq(100L),
                eq(DayOfWeek.MONDAY),
                eq(LocalTime.of(8, 0)),
                eq(LocalTime.of(16, 0)),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 12, 31))
        )).thenReturn(sampleOperatingHours);

        mockMvc.perform(post("/api/v1/centers/100/operating-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/centers/100/operating-hours/200"))
                .andExpect(jsonPath("$.id", is(200)))
                .andExpect(jsonPath("$.centerId", is(100)))
                .andExpect(jsonPath("$.dayOfWeek", is("MONDAY")))
                .andExpect(jsonPath("$.opening", is("08:00:00")))
                .andExpect(jsonPath("$.closing", is("16:00:00")))
                .andExpect(jsonPath("$.effectiveFrom", is("2026-09-01")))
                .andExpect(jsonPath("$.effectiveTo", is("2026-12-31")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{id}/operating-hours returns 404 when parent center not found")
    void createOperatingHoursParentNotFound() throws Exception {
        String jsonPayload = """
                {
                    "dayOfWeek": "MONDAY",
                    "opening": "08:00:00",
                    "closing": "16:00:00",
                    "effectiveFrom": "2026-09-01"
                }
                """;

        when(operatingHoursService.createOperatingHours(eq(999L), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(post("/api/v1/centers/999/operating-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 8. GET /api/v1/centers/{id}/operating-hours
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/centers/{id}/operating-hours returns 200 and list of operating hours")
    void getOperatingHoursSuccess() throws Exception {
        when(operatingHoursService.getOperatingHoursByCenter(100L)).thenReturn(List.of(sampleOperatingHours));

        mockMvc.perform(get("/api/v1/centers/100/operating-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(200)))
                .andExpect(jsonPath("$[0].centerId", is(100)));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{id}/operating-hours returns 404 when parent center not found")
    void getOperatingHoursParentNotFound() throws Exception {
        when(operatingHoursService.getOperatingHoursByCenter(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(get("/api/v1/centers/999/operating-hours"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 9. PUT /api/v1/centers/{centerId}/operating-hours/{hoursId}
    // ==========================================

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/operating-hours/{hoursId} updates hours and returns 200")
    void updateOperatingHoursSuccess() throws Exception {
        String jsonPayload = """
                {
                    "dayOfWeek": "TUESDAY",
                    "opening": "09:00:00",
                    "closing": "17:00:00",
                    "effectiveFrom": "2026-09-01",
                    "effectiveTo": "2026-12-31"
                }
                """;

        when(operatingHoursService.updateOperatingHours(
                eq(100L),
                eq(200L),
                eq(DayOfWeek.TUESDAY),
                eq(LocalTime.of(9, 0)),
                eq(LocalTime.of(17, 0)),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 12, 31))
        )).thenReturn(sampleOperatingHours);

        mockMvc.perform(put("/api/v1/centers/100/operating-hours/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(200)));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/operating-hours/{hoursId} returns 404 on parent mismatch or missing hours")
    void updateOperatingHoursMismatch() throws Exception {
        String jsonPayload = """
                {
                    "dayOfWeek": "TUESDAY",
                    "opening": "09:00:00",
                    "closing": "17:00:00",
                    "effectiveFrom": "2026-09-01"
                }
                """;

        when(operatingHoursService.updateOperatingHours(eq(100L), eq(999L), any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Operating hours not found with ID: 999 for center ID: 100"));

        mockMvc.perform(put("/api/v1/centers/100/operating-hours/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 10. POST /api/v1/centers/{centerId}/operating-hours/{hoursId}/end-date
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/operating-hours/{hoursId}/end-date ends shift and returns 200")
    void endDateOperatingHoursSuccess() throws Exception {
        String jsonPayload = """
                {
                    "endDate": "2026-10-31"
                }
                """;

        when(operatingHoursService.endDateOperatingHours(eq(100L), eq(200L), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(sampleOperatingHours);

        mockMvc.perform(post("/api/v1/centers/100/operating-hours/200/end-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(200)));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/operating-hours/{hoursId}/end-date returns 404 when hours not found")
    void endDateOperatingHoursNotFound() throws Exception {
        String jsonPayload = """
                {
                    "endDate": "2026-10-31"
                }
                """;

        when(operatingHoursService.endDateOperatingHours(eq(100L), eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Operating hours not found with ID: 999 for center ID: 100"));

        mockMvc.perform(post("/api/v1/centers/100/operating-hours/999/end-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 11. POST /api/v1/centers/{id}/closures
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers/{id}/closures creates closure and returns 201 with Location")
    void createClosureSuccess() throws Exception {
        String jsonPayload = """
                {
                    "startAt": "2026-10-01T08:00:00Z",
                    "endAt": "2026-10-03T18:00:00Z",
                    "reason": "Facility electrical maintenance"
                }
                """;

        when(closureService.createClosure(
                eq(100L),
                eq(Instant.parse("2026-10-01T08:00:00Z")),
                eq(Instant.parse("2026-10-03T18:00:00Z")),
                eq("Facility electrical maintenance")
        )).thenReturn(sampleClosure);

        mockMvc.perform(post("/api/v1/centers/100/closures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/centers/100/closures/300"))
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.centerId", is(100)))
                .andExpect(jsonPath("$.startAt", is("2026-10-01T08:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2026-10-03T18:00:00Z")))
                .andExpect(jsonPath("$.reason", is("Facility electrical maintenance")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{id}/closures returns 404 when parent center not found")
    void createClosureParentNotFound() throws Exception {
        String jsonPayload = """
                {
                    "startAt": "2026-10-01T08:00:00Z",
                    "endAt": "2026-10-03T18:00:00Z",
                    "reason": "Facility electrical maintenance"
                }
                """;

        when(closureService.createClosure(eq(999L), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(post("/api/v1/centers/999/closures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 12. GET /api/v1/centers/{id}/closures
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/centers/{id}/closures returns 200 and list of closures")
    void getClosuresSuccess() throws Exception {
        when(closureService.getClosuresForCenter(100L)).thenReturn(List.of(sampleClosure));

        mockMvc.perform(get("/api/v1/centers/100/closures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(300)))
                .andExpect(jsonPath("$[0].centerId", is(100)));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{id}/closures returns 404 when parent center not found")
    void getClosuresParentNotFound() throws Exception {
        when(closureService.getClosuresForCenter(999L))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        mockMvc.perform(get("/api/v1/centers/999/closures"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 13. PUT /api/v1/centers/{centerId}/closures/{closureId}
    // ==========================================

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/closures/{closureId} updates closure and returns 200")
    void updateClosureSuccess() throws Exception {
        String jsonPayload = """
                {
                    "startAt": "2026-10-02T08:00:00Z",
                    "endAt": "2026-10-04T18:00:00Z",
                    "reason": "Rescheduled facility maintenance"
                }
                """;

        when(closureService.updateFutureClosure(
                eq(100L),
                eq(300L),
                eq(Instant.parse("2026-10-02T08:00:00Z")),
                eq(Instant.parse("2026-10-04T18:00:00Z")),
                eq("Rescheduled facility maintenance")
        )).thenReturn(sampleClosure);

        mockMvc.perform(put("/api/v1/centers/100/closures/300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(300)));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/closures/{closureId} returns 404 on parent mismatch or missing closure")
    void updateClosureMismatch() throws Exception {
        String jsonPayload = """
                {
                    "startAt": "2026-10-02T08:00:00Z",
                    "endAt": "2026-10-04T18:00:00Z",
                    "reason": "Rescheduled facility maintenance"
                }
                """;

        when(closureService.updateFutureClosure(eq(100L), eq(999L), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Center closure not found with ID: 999 for center ID: 100"));

        mockMvc.perform(put("/api/v1/centers/100/closures/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ==========================================
    // 14. POST /api/v1/centers/{centerId}/closures/{closureId}/cancel
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/closures/{closureId}/cancel cancels closure and returns 200")
    void cancelClosureSuccess() throws Exception {
        CenterClosure cancelled = new CenterClosure(
                sampleCenter,
                Instant.parse("2026-10-01T08:00:00Z"),
                Instant.parse("2026-10-03T18:00:00Z"),
                "Facility electrical maintenance"
        );
        ReflectionTestUtils.setField(cancelled, "id", 300L);
        ReflectionTestUtils.setField(cancelled, "status", CenterClosureStatus.CANCELLED);

        when(closureService.cancelFutureClosure(100L, 300L)).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/centers/100/closures/300/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(300)))
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/closures/{closureId}/cancel returns 404 when closure not found")
    void cancelClosureNotFound() throws Exception {
        when(closureService.cancelFutureClosure(100L, 999L))
                .thenThrow(new ResourceNotFoundException("Center closure not found with ID: 999 for center ID: 100"));

        mockMvc.perform(post("/api/v1/centers/100/closures/999/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/closures/{closureId}/cancel returns 400 when closure already started or cancelled")
    void cancelClosureAlreadyStartedOrCancelled() throws Exception {
        when(closureService.cancelFutureClosure(100L, 300L))
                .thenThrow(new IllegalArgumentException("A started center closure cannot be cancelled"));

        mockMvc.perform(post("/api/v1/centers/100/closures/300/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("A started center closure cannot be cancelled")));
    }
}
