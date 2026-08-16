package com.se2030.vaccinationportal.campaign.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.response.CampaignLifecycleEventResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignSummaryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.TargetGroupSummaryResponse;
import com.se2030.vaccinationportal.campaign.dto.response.VaccineTypeSummaryResponse;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CampaignDtoMapperTests {

    @Test
    @DisplayName("Maps null objects safely to null")
    void mapsNullsSafely() {
        assertNull(CampaignDtoMapper.toResponse((VaccinationCampaign) null));
        assertNull(CampaignDtoMapper.toSummaryResponse((VaccinationCampaign) null));
        assertNull(CampaignDtoMapper.toResponse((CampaignLifecycleEvent) null));
        assertNull(CampaignDtoMapper.toSummaryResponse((VaccineType) null));
        assertNull(CampaignDtoMapper.toSummaryResponse((TargetGroup) null));
    }

    @Test
    @DisplayName("Maps VaccineType and TargetGroup to summary responses")
    void mapsMasterDataSummaries() {
        VaccineType vaccineType = new VaccineType("VT-01", "COVID-19 mRNA", "dose", true);
        ReflectionTestUtils.setField(vaccineType, "id", 101L);

        VaccineTypeSummaryResponse vtResponse = CampaignDtoMapper.toSummaryResponse(vaccineType);
        assertNotNull(vtResponse);
        assertEquals(101L, vtResponse.id());
        assertEquals("VT-01", vtResponse.code());
        assertEquals("COVID-19 mRNA", vtResponse.name());
        assertEquals("dose", vtResponse.doseUnit());
        assertTrue(vtResponse.active());

        TargetGroup targetGroup = new TargetGroup("TG-01", "Elderly 65+", true);
        ReflectionTestUtils.setField(targetGroup, "id", 202L);

        TargetGroupSummaryResponse tgResponse = CampaignDtoMapper.toSummaryResponse(targetGroup);
        assertNotNull(tgResponse);
        assertEquals(202L, tgResponse.id());
        assertEquals("TG-01", tgResponse.code());
        assertEquals("Elderly 65+", tgResponse.description());
        assertTrue(tgResponse.active());
    }

    @Test
    @DisplayName("Maps VaccinationCampaign to detailed and summary responses")
    void mapsCampaignDetailsAndSummary() {
        VaccineType vaccineType = new VaccineType("VT-01", "COVID-19 mRNA", "dose", true);
        ReflectionTestUtils.setField(vaccineType, "id", 101L);

        TargetGroup targetGroup = new TargetGroup("TG-01", "Elderly 65+", true);
        ReflectionTestUtils.setField(targetGroup, "id", 202L);

        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Instant openAt = Instant.parse("2026-08-15T08:00:00Z");
        Instant closeAt = Instant.parse("2026-12-30T18:00:00Z");

        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-2026-01",
                "Fall Booster Campaign",
                "Annual booster drive",
                vaccineType,
                targetGroup,
                startDate,
                endDate,
                openAt,
                closeAt
        );
        ReflectionTestUtils.setField(campaign, "id", 1L);
        Instant now = Instant.now();
        ReflectionTestUtils.setField(campaign, "createdAt", now);
        ReflectionTestUtils.setField(campaign, "updatedAt", now);

        CampaignResponse detailed = CampaignDtoMapper.toResponse(campaign);
        assertNotNull(detailed);
        assertEquals(1L, detailed.id());
        assertEquals("CMP-2026-01", detailed.code());
        assertEquals("Fall Booster Campaign", detailed.name());
        assertEquals("Annual booster drive", detailed.description());
        assertEquals(CampaignStatus.DRAFT, detailed.status());
        assertEquals(startDate, detailed.serviceStartDate());
        assertEquals(endDate, detailed.serviceEndDate());
        assertEquals(openAt, detailed.bookingOpenAt());
        assertEquals(closeAt, detailed.bookingCloseAt());
        assertNotNull(detailed.vaccineType());
        assertEquals("VT-01", detailed.vaccineType().code());
        assertNotNull(detailed.targetGroup());
        assertEquals("TG-01", detailed.targetGroup().code());

        CampaignSummaryResponse summary = CampaignDtoMapper.toSummaryResponse(campaign);
        assertNotNull(summary);
        assertEquals(1L, summary.id());
        assertEquals("CMP-2026-01", summary.code());
        assertEquals("Fall Booster Campaign", summary.name());
        assertEquals("VT-01", summary.vaccineTypeCode());
        assertEquals("TG-01", summary.targetGroupCode());
        assertEquals(startDate, summary.serviceStartDate());
        assertEquals(endDate, summary.serviceEndDate());
        assertEquals(CampaignStatus.DRAFT, summary.status());
    }

    @Test
    @DisplayName("Maps CampaignLifecycleEvent to response DTO")
    void mapsLifecycleEvent() {
        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-2026-02",
                "Winter Campaign",
                null,
                new VaccineType("VT-01", "COVID-19 mRNA"),
                new TargetGroup("TG-01", "General Population"),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 1),
                Instant.parse("2026-10-01T08:00:00Z"),
                Instant.parse("2026-11-30T18:00:00Z")
        );
        ReflectionTestUtils.setField(campaign, "id", 42L);

        Instant eventTime = Instant.now();
        CampaignLifecycleEvent event = new CampaignLifecycleEvent(
                campaign,
                CampaignLifecycleChangeType.SUSPENDED,
                CampaignStatus.PUBLISHED,
                CampaignStatus.SUSPENDED,
                "Adverse weather warning",
                "OFFICER-01",
                eventTime
        );
        ReflectionTestUtils.setField(event, "id", 501L);

        CampaignLifecycleEventResponse response = CampaignDtoMapper.toResponse(event);
        assertNotNull(response);
        assertEquals(501L, response.id());
        assertEquals(42L, response.campaignId());
        assertEquals(CampaignLifecycleChangeType.SUSPENDED, response.changeType());
        assertEquals(CampaignStatus.PUBLISHED, response.previousStatus());
        assertEquals(CampaignStatus.SUSPENDED, response.newStatus());
        assertEquals("Adverse weather warning", response.reason());
        assertEquals("OFFICER-01", response.actorId());
        assertEquals(eventTime, response.createdAt());
    }
}
