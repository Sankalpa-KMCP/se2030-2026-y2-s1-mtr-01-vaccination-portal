package com.se2030.vaccinationportal.campaign.controller;

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
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.service.OperationalResourceService;
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
class CenterOperationalResourceControllerTests {

    private MockMvc mockMvc;

    @Mock
    private OperationalResourceService resourceService;

    private VaccinationCenter sampleCenter;
    private OperationalResource sampleResource;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CenterOperationalResourceController(resourceService))
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

        sampleResource = new OperationalResource(
                sampleCenter,
                "STATION-01",
                ResourceType.VACCINATION_STATION,
                "Vaccination Booth 1",
                OperationalResourceStatus.ACTIVE
        );
        ReflectionTestUtils.setField(sampleResource, "id", 101L);
        ReflectionTestUtils.setField(sampleResource, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(sampleResource, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/resources - valid payload creates resource and returns 201 with Location header")
    void createResource_withValidPayload_returns201() throws Exception {
        when(resourceService.createResource(eq(1L), eq("STATION-01"), eq(ResourceType.VACCINATION_STATION), eq("Vaccination Booth 1"), eq(OperationalResourceStatus.ACTIVE)))
                .thenReturn(sampleResource);

        String json = """
                {
                    "code": "STATION-01",
                    "resourceType": "VACCINATION_STATION",
                    "name": "Vaccination Booth 1",
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/centers/1/resources/101"))
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.centerId", is(1)))
                .andExpect(jsonPath("$.code", is("STATION-01")))
                .andExpect(jsonPath("$.resourceType", is("VACCINATION_STATION")))
                .andExpect(jsonPath("$.name", is("Vaccination Booth 1")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        verify(resourceService).createResource(1L, "STATION-01", ResourceType.VACCINATION_STATION, "Vaccination Booth 1", OperationalResourceStatus.ACTIVE);
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/resources - omitted status defaults to ACTIVE")
    void createResource_withOmittedStatus_defaultsToActive() throws Exception {
        when(resourceService.createResource(eq(1L), eq("STATION-01"), eq(ResourceType.VACCINATION_STATION), eq("Vaccination Booth 1"), isNull()))
                .thenReturn(sampleResource);

        String json = """
                {
                    "code": "STATION-01",
                    "resourceType": "VACCINATION_STATION",
                    "name": "Vaccination Booth 1"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(resourceService).createResource(1L, "STATION-01", ResourceType.VACCINATION_STATION, "Vaccination Booth 1", null);
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/resources - explicit MAINTENANCE creates resource with MAINTENANCE status")
    void createResource_withMaintenanceStatus_returns201() throws Exception {
        OperationalResource maintenanceResource = new OperationalResource(
                sampleCenter,
                "ROOM-02",
                ResourceType.ROOM,
                "Consultation Room B",
                OperationalResourceStatus.MAINTENANCE
        );
        ReflectionTestUtils.setField(maintenanceResource, "id", 102L);
        ReflectionTestUtils.setField(maintenanceResource, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(maintenanceResource, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        when(resourceService.createResource(eq(1L), eq("ROOM-02"), eq(ResourceType.ROOM), eq("Consultation Room B"), eq(OperationalResourceStatus.MAINTENANCE)))
                .thenReturn(maintenanceResource);

        String json = """
                {
                    "code": "ROOM-02",
                    "resourceType": "ROOM",
                    "name": "Consultation Room B",
                    "status": "MAINTENANCE"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(102)))
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/resources - blank code returns 400 Bad Request")
    void createResource_withBlankCode_returns400() throws Exception {
        String json = """
                {
                    "code": "",
                    "resourceType": "VACCINATION_STATION",
                    "name": "Vaccination Booth 1"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("code")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/resources - duplicate code returns 409 Conflict")
    void createResource_withDuplicateCode_returns409() throws Exception {
        when(resourceService.createResource(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Resource code 'STATION-01' already exists for center 1"));

        String json = """
                {
                    "code": "STATION-01",
                    "resourceType": "VACCINATION_STATION",
                    "name": "Vaccination Booth 1"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/centers/{centerId}/resources - non-existent center returns 404 Not Found")
    void createResource_withNonExistentCenter_returns404() throws Exception {
        when(resourceService.createResource(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Vaccination center not found with ID: 999"));

        String json = """
                {
                    "code": "STATION-01",
                    "resourceType": "VACCINATION_STATION",
                    "name": "Vaccination Booth 1"
                }
                """;

        mockMvc.perform(post("/api/v1/centers/999/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Vaccination center not found")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/resources - returns all resources regardless of status")
    void listResources_withoutFilter_returnsAllStatuses() throws Exception {
        OperationalResource res2 = new OperationalResource(
                sampleCenter,
                "ROOM-01",
                ResourceType.ROOM,
                "Main Room",
                OperationalResourceStatus.INACTIVE
        );
        ReflectionTestUtils.setField(res2, "id", 102L);
        ReflectionTestUtils.setField(res2, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(res2, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        when(resourceService.getResourcesForCenter(1L, null))
                .thenReturn(List.of(res2, sampleResource));

        mockMvc.perform(get("/api/v1/centers/1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("ROOM-01")))
                .andExpect(jsonPath("$[0].status", is("INACTIVE")))
                .andExpect(jsonPath("$[1].code", is("STATION-01")))
                .andExpect(jsonPath("$[1].status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/resources?resourceType=ROOM - returns filtered resources")
    void listResources_withResourceTypeFilter_returnsFilteredList() throws Exception {
        OperationalResource room = new OperationalResource(
                sampleCenter,
                "ROOM-01",
                ResourceType.ROOM,
                "Main Room",
                OperationalResourceStatus.ACTIVE
        );
        ReflectionTestUtils.setField(room, "id", 102L);
        ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-08-20T10:00:00Z"));

        when(resourceService.getResourcesForCenter(1L, ResourceType.ROOM))
                .thenReturn(List.of(room));

        mockMvc.perform(get("/api/v1/centers/1/resources")
                        .param("resourceType", "ROOM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resourceType", is("ROOM")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/resources/{resourceId} - returns single resource")
    void getResource_withValidId_returns200() throws Exception {
        when(resourceService.getResource(1L, 101L))
                .thenReturn(sampleResource);

        mockMvc.perform(get("/api/v1/centers/1/resources/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.code", is("STATION-01")));
    }

    @Test
    @DisplayName("GET /api/v1/centers/{centerId}/resources/{resourceId} - cross-center mismatch returns 404 Not Found")
    void getResource_withCrossCenterMismatch_returns404() throws Exception {
        when(resourceService.getResource(2L, 101L))
                .thenThrow(new ResourceNotFoundException("Operational resource with ID 101 does not belong to center 2"));

        mockMvc.perform(get("/api/v1/centers/2/resources/101"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("does not belong to center")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/resources/{resourceId} - valid update returns 200")
    void updateResource_withValidPayload_returns200() throws Exception {
        OperationalResource updatedResource = new OperationalResource(
                sampleCenter,
                "STATION-01",
                ResourceType.VACCINATION_STATION,
                "Updated Station Name",
                OperationalResourceStatus.MAINTENANCE
        );
        ReflectionTestUtils.setField(updatedResource, "id", 101L);
        ReflectionTestUtils.setField(updatedResource, "createdAt", Instant.parse("2026-08-20T10:00:00Z"));
        ReflectionTestUtils.setField(updatedResource, "updatedAt", Instant.parse("2026-08-20T11:00:00Z"));

        when(resourceService.updateResource(1L, 101L, "Updated Station Name", OperationalResourceStatus.MAINTENANCE))
                .thenReturn(updatedResource);

        String json = """
                {
                    "name": "Updated Station Name",
                    "status": "MAINTENANCE"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/1/resources/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.name", is("Updated Station Name")))
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/resources/{resourceId} - blank name returns 400 Bad Request")
    void updateResource_withBlankName_returns400() throws Exception {
        String json = """
                {
                    "name": "",
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/1/resources/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("name")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/resources/{resourceId} - null status returns 400 Bad Request")
    void updateResource_withNullStatus_returns400() throws Exception {
        String json = """
                {
                    "name": "Updated Name"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/1/resources/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")));
    }

    @Test
    @DisplayName("PUT /api/v1/centers/{centerId}/resources/{resourceId} - cross-center mismatch returns 404 Not Found")
    void updateResource_withCrossCenterMismatch_returns404() throws Exception {
        when(resourceService.updateResource(eq(2L), eq(101L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Operational resource with ID 101 does not belong to center 2"));

        String json = """
                {
                    "name": "Updated Name",
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/centers/2/resources/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("does not belong to center")));
    }
}
