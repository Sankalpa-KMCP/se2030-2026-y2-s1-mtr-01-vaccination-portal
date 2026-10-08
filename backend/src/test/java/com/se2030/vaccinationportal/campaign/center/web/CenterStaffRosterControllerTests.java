package com.se2030.vaccinationportal.campaign.center.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.service.OperationalStaffRosterService;
import java.time.Instant;
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
class CenterStaffRosterControllerTests {

    private MockMvc mockMvc;

    @Mock
    private OperationalStaffRosterService rosterService;

    private VaccinationCenter sampleCenter;
    private OperationalStaffRoster sampleRoster;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CenterStaffRosterController(rosterService))
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
        ReflectionTestUtils.setField(sampleCenter, "id", 1L);

        sampleRoster = new OperationalStaffRoster(
                sampleCenter,
                501L,
                StaffRole.VACCINATOR,
                OperationalStaffStatus.ACTIVE
        );
        ReflectionTestUtils.setField(sampleRoster, "id", 201L);
        ReflectionTestUtils.setField(sampleRoster, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(sampleRoster, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - valid payload registers staff and returns 201 with Location header")
    void registerStaff_withValidPayload_returns201() throws Exception {
        when(rosterService.registerStaffMember(eq(1L), eq(501L), eq(StaffRole.VACCINATOR), eq(OperationalStaffStatus.ACTIVE)))
                .thenReturn(sampleRoster);

        String json = """
                {
                    "userId": 501,
                    "role": "VACCINATOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/centers/1/staff-roster/201"))
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.centerId", is(1)))
                .andExpect(jsonPath("$.userId", is(501)))
                .andExpect(jsonPath("$.role", is("VACCINATOR")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        verify(rosterService).registerStaffMember(1L, 501L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE);
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - omitted status defaults to ACTIVE")
    void registerStaff_withOmittedStatus_defaultsToActive() throws Exception {
        when(rosterService.registerStaffMember(eq(1L), eq(501L), eq(StaffRole.VACCINATOR), isNull()))
                .thenReturn(sampleRoster);

        String json = """
                {
                    "userId": 501,
                    "role": "VACCINATOR"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(rosterService).registerStaffMember(1L, 501L, StaffRole.VACCINATOR, null);
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - registers SUPERVISOR role")
    void registerStaff_withSupervisorRole_returns201() throws Exception {
        OperationalStaffRoster supervisorRoster = new OperationalStaffRoster(
                sampleCenter,
                502L,
                StaffRole.SUPERVISOR,
                OperationalStaffStatus.ACTIVE
        );
        ReflectionTestUtils.setField(supervisorRoster, "id", 202L);
        ReflectionTestUtils.setField(supervisorRoster, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(supervisorRoster, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        when(rosterService.registerStaffMember(eq(1L), eq(502L), eq(StaffRole.SUPERVISOR), eq(OperationalStaffStatus.ACTIVE)))
                .thenReturn(supervisorRoster);

        String json = """
                {
                    "userId": 502,
                    "role": "SUPERVISOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(202)))
                .andExpect(jsonPath("$.role", is("SUPERVISOR")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - registers CHECK_IN_STAFF role")
    void registerStaff_withCheckInStaffRole_returns201() throws Exception {
        OperationalStaffRoster checkInRoster = new OperationalStaffRoster(
                sampleCenter,
                503L,
                StaffRole.CHECK_IN_STAFF,
                OperationalStaffStatus.ACTIVE
        );
        ReflectionTestUtils.setField(checkInRoster, "id", 203L);
        ReflectionTestUtils.setField(checkInRoster, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(checkInRoster, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        when(rosterService.registerStaffMember(eq(1L), eq(503L), eq(StaffRole.CHECK_IN_STAFF), eq(OperationalStaffStatus.ACTIVE)))
                .thenReturn(checkInRoster);

        String json = """
                {
                    "userId": 503,
                    "role": "CHECK_IN_STAFF",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(203)))
                .andExpect(jsonPath("$.role", is("CHECK_IN_STAFF")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - null userId returns 400 Bad Request")
    void registerStaff_withNullUserId_returns400() throws Exception {
        String json = """
                {
                    "role": "VACCINATOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("userId")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - negative userId returns 400 Bad Request")
    void registerStaff_withNegativeUserId_returns400() throws Exception {
        String json = """
                {
                    "userId": -5,
                    "role": "VACCINATOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("userId")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - null role returns 400 Bad Request")
    void registerStaff_withNullRole_returns400() throws Exception {
        String json = """
                {
                    "userId": 501,
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("role")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - duplicate user returns 409 Conflict")
    void registerStaff_withDuplicateUser_returns409() throws Exception {
        when(rosterService.registerStaffMember(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Staff user 501 is already rostered at center 1"));

        String json = """
                {
                    "userId": 501,
                    "role": "VACCINATOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already rostered")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/staff-roster - non-existent center returns 404 Not Found")
    void registerStaff_withNonExistentCenter_returns404() throws Exception {
        when(rosterService.registerStaffMember(any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        String json = """
                {
                    "userId": 501,
                    "role": "VACCINATOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/999/staff-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination center not found")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/staff-roster - returns all roster entries")
    void listStaffRoster_withoutFilter_returnsAll() throws Exception {
        OperationalStaffRoster r2 = new OperationalStaffRoster(
                sampleCenter,
                502L,
                StaffRole.CHECK_IN_STAFF,
                OperationalStaffStatus.INACTIVE
        );
        ReflectionTestUtils.setField(r2, "id", 202L);
        ReflectionTestUtils.setField(r2, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(r2, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        when(rosterService.getStaffRosterForCenter(1L, null, null))
                .thenReturn(List.of(sampleRoster, r2));

        mockMvc.perform(get("/api/v1/centers/1/staff-roster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(501)))
                .andExpect(jsonPath("$[0].role", is("VACCINATOR")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[1].userId", is(502)))
                .andExpect(jsonPath("$[1].role", is("CHECK_IN_STAFF")))
                .andExpect(jsonPath("$[1].status", is("INACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/staff-roster?role=VACCINATOR - returns filtered roster entries")
    void listStaffRoster_withRoleFilter_returnsFilteredList() throws Exception {
        when(rosterService.getStaffRosterForCenter(1L, StaffRole.VACCINATOR, null))
                .thenReturn(List.of(sampleRoster));

        mockMvc.perform(get("/api/v1/centers/1/staff-roster")
                        .param("role", "VACCINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role", is("VACCINATOR")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/staff-roster?status=ACTIVE - returns filtered roster entries")
    void listStaffRoster_withStatusFilter_returnsFilteredList() throws Exception {
        when(rosterService.getStaffRosterForCenter(1L, null, OperationalStaffStatus.ACTIVE))
                .thenReturn(List.of(sampleRoster));

        mockMvc.perform(get("/api/v1/centers/1/staff-roster")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/staff-roster?role=VACCINATOR&status=ACTIVE - returns combined filtered roster entries")
    void listStaffRoster_withBothFilters_returnsFilteredList() throws Exception {
        when(rosterService.getStaffRosterForCenter(1L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE))
                .thenReturn(List.of(sampleRoster));

        mockMvc.perform(get("/api/v1/centers/1/staff-roster")
                        .param("role", "VACCINATOR")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role", is("VACCINATOR")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/staff-roster/{rosterId} - returns single roster entry")
    void getRosterEntry_withValidId_returns200() throws Exception {
        when(rosterService.getRosterEntry(1L, 201L))
                .thenReturn(sampleRoster);

        mockMvc.perform(get("/api/v1/centers/1/staff-roster/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.userId", is(501)))
                .andExpect(jsonPath("$.role", is("VACCINATOR")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/staff-roster/{rosterId} - cross-center mismatch returns 404 Not Found")
    void getRosterEntry_withCrossCenterMismatch_returns404() throws Exception {
        when(rosterService.getRosterEntry(2L, 201L))
                .thenThrow(new ResourceNotFoundException("Operational staff roster entry with ID 201 does not belong to center 2"));

        mockMvc.perform(get("/api/v1/centers/2/staff-roster/201"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("does not belong to center")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/staff-roster/{rosterId} - valid update returns 200")
    void updateStaffRoster_withValidPayload_returns200() throws Exception {
        OperationalStaffRoster updated = new OperationalStaffRoster(
                sampleCenter,
                501L,
                StaffRole.CHECK_IN_STAFF,
                OperationalStaffStatus.INACTIVE
        );
        ReflectionTestUtils.setField(updated, "id", 201L);
        ReflectionTestUtils.setField(updated, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(updated, "updatedAt", Instant.parse("2026-08-20T11:00:00Z"));

        when(rosterService.updateStaffRoster(1L, 201L, StaffRole.CHECK_IN_STAFF, OperationalStaffStatus.INACTIVE))
                .thenReturn(updated);

        String json = """
                {
                    "role": "CHECK_IN_STAFF",
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/1/staff-roster/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(201)))
                .andExpect(jsonPath("$.role", is("CHECK_IN_STAFF")))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/staff-roster/{rosterId} - null role returns 400 Bad Request")
    void updateStaffRoster_withNullRole_returns400() throws Exception {
        String json = """
                {
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/1/staff-roster/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("role")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/staff-roster/{rosterId} - null status returns 400 Bad Request")
    void updateStaffRoster_withNullStatus_returns400() throws Exception {
        String json = """
                {
                    "role": "VACCINATOR"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/1/staff-roster/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/staff-roster/{rosterId} - cross-center mismatch returns 404 Not Found")
    void updateStaffRoster_withCrossCenterMismatch_returns404() throws Exception {
        when(rosterService.updateStaffRoster(eq(2L), eq(201L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Operational staff roster entry with ID 201 does not belong to center 2"));

        String json = """
                {
                    "role": "SUPERVISOR",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/2/staff-roster/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("does not belong to center")));
    }
}
