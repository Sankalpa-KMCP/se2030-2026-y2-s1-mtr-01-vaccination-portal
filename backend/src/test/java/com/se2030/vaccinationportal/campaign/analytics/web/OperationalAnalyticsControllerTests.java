package com.se2030.vaccinationportal.campaign.analytics.web;

import com.se2030.vaccinationportal.campaign.web.GlobalCampaignExceptionHandler;


import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.analytics.dto.response.OperationalAnalyticsResponse;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.analytics.service.OperationalAnalyticsService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OperationalAnalyticsControllerTests {

    private MockMvc mockMvc;

    @Mock
    private OperationalAnalyticsService operationalAnalyticsService;

    private OperationalAnalyticsResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OperationalAnalyticsController(operationalAnalyticsService))
                .setControllerAdvice(new GlobalCampaignExceptionHandler())
                .build();

        sampleResponse = new OperationalAnalyticsResponse(
                Map.of(CampaignStatus.ACTIVE, 3L, CampaignStatus.PUBLISHED, 2L),
                Map.of(SessionLifecycleStatus.SCHEDULED, 10L, SessionLifecycleStatus.COMPLETED, 5L),
                1200L,
                1400L,
                1000L,
                650L,
                350L,
                65.0,
                2L,
                1L,
                15L,
                18L,
                Map.of(ResourceType.VACCINATION_STATION, 15L, ResourceType.EQUIPMENT, 12L),
                Instant.parse("2026-08-28T20:00:00Z")
        );
    }

    @Test
    @DisplayName("GET /api/v1/analytics/operations without filters returns 200 and operational analytics")
    void getOperationalAnalytics_noFilters_returns200() throws Exception {
        when(operationalAnalyticsService.getOperationalAnalytics(null, null)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/analytics/operations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApprovedCapacity", is(1200)))
                .andExpect(jsonPath("$.totalRecommendedThroughput", is(1400)))
                .andExpect(jsonPath("$.totalConfiguredSlotCapacity", is(1000)))
                .andExpect(jsonPath("$.totalReservedSlots", is(650)))
                .andExpect(jsonPath("$.totalRemainingSlotCapacity", is(350)))
                .andExpect(jsonPath("$.slotUtilizationPercentage", is(65.0)))
                .andExpect(jsonPath("$.cancelledSessionCount", is(2)))
                .andExpect(jsonPath("$.cancelledCampaignCount", is(1)))
                .andExpect(jsonPath("$.assignedStationCount", is(15)))
                .andExpect(jsonPath("$.assignedVaccinatorCount", is(18)))
                .andExpect(jsonPath("$.campaignCountByStatus.ACTIVE", is(3)))
                .andExpect(jsonPath("$.sessionCountByLifecycleStatus.SCHEDULED", is(10)))
                .andExpect(jsonPath("$.calculatedAt", is("2026-08-28T20:00:00Z")));

        verify(operationalAnalyticsService).getOperationalAnalytics(null, null);
    }

    @Test
    @DisplayName("GET /api/v1/analytics/operations with centerId and campaignId filters passes params and returns 200")
    void getOperationalAnalytics_withFilters_returns200() throws Exception {
        when(operationalAnalyticsService.getOperationalAnalytics(eq(10L), eq(20L))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/analytics/operations")
                        .param("centerId", "10")
                        .param("campaignId", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApprovedCapacity", is(1200)))
                .andExpect(jsonPath("$.slotUtilizationPercentage", is(65.0)));

        verify(operationalAnalyticsService).getOperationalAnalytics(10L, 20L);
    }

    @Test
    @DisplayName("GET /api/v1/analytics/operations with invalid filter throws 400 Bad Request")
    void getOperationalAnalytics_invalidCenter_returns400() throws Exception {
        when(operationalAnalyticsService.getOperationalAnalytics(eq(999L), eq(null)))
                .thenThrow(new IllegalArgumentException("Vaccination center not found: 999"));

        mockMvc.perform(get("/api/v1/analytics/operations")
                        .param("centerId", "999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Vaccination center not found: 999")));

        verify(operationalAnalyticsService).getOperationalAnalytics(999L, null);
    }
}
