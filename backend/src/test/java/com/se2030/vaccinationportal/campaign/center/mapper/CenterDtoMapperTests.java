package com.se2030.vaccinationportal.campaign.center.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.se2030.vaccinationportal.campaign.center.dto.response.CenterClosureResponse;
import com.se2030.vaccinationportal.campaign.center.dto.response.CenterResponse;
import com.se2030.vaccinationportal.campaign.center.dto.response.CenterSummaryResponse;
import com.se2030.vaccinationportal.campaign.center.dto.response.OperatingHoursResponse;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CenterDtoMapperTests {

    @Test
    @DisplayName("Maps null objects safely to null")
    void mapsNullsSafely() {
        assertNull(CenterDtoMapper.toResponse((VaccinationCenter) null));
        assertNull(CenterDtoMapper.toSummaryResponse((VaccinationCenter) null));
        assertNull(CenterDtoMapper.toResponse((OperatingHours) null));
        assertNull(CenterDtoMapper.toResponse((CenterClosure) null));
    }

    @Test
    @DisplayName("Maps VaccinationCenter to CenterResponse and CenterSummaryResponse")
    void mapsVaccinationCenter() {
        VaccinationCenter center = new VaccinationCenter(
                "CTR-COLOMBO-01",
                "Colombo Central Vaccination Center",
                "123 Main Street, Colombo 01",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 1001L);
        Instant createdAt = Instant.parse("2026-08-01T08:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-01T08:30:00Z");
        ReflectionTestUtils.setField(center, "createdAt", createdAt);
        ReflectionTestUtils.setField(center, "updatedAt", updatedAt);

        CenterResponse response = CenterDtoMapper.toResponse(center);
        assertNotNull(response);
        assertEquals(1001L, response.id());
        assertEquals("CTR-COLOMBO-01", response.code());
        assertEquals("Colombo Central Vaccination Center", response.name());
        assertEquals("123 Main Street, Colombo 01", response.address());
        assertEquals("+94 11 2345678", response.contact());
        assertEquals("Asia/Colombo", response.timezone());
        assertEquals(CenterStatus.ACTIVE, response.status());
        assertEquals(150, response.safeConcurrentOccupancy());
        assertEquals(50, response.sessionServiceCeiling());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());

        CenterSummaryResponse summary = CenterDtoMapper.toSummaryResponse(center);
        assertNotNull(summary);
        assertEquals(1001L, summary.id());
        assertEquals("CTR-COLOMBO-01", summary.code());
        assertEquals("Colombo Central Vaccination Center", summary.name());
        assertEquals(CenterStatus.ACTIVE, summary.status());
        assertEquals("Asia/Colombo", summary.timezone());
    }

    @Test
    @DisplayName("Maps OperatingHours to OperatingHoursResponse")
    void mapsOperatingHours() {
        VaccinationCenter center = new VaccinationCenter(
                "CTR-KANDY-01",
                "Kandy General Clinic",
                "45 Hill Street, Kandy",
                "+94 81 2233445",
                CenterStatus.ACTIVE,
                100,
                null,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 2001L);

        OperatingHours hours = new OperatingHours(
                center,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(hours, "id", 3001L);
        Instant createdAt = Instant.parse("2026-08-05T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-05T09:00:00Z");
        ReflectionTestUtils.setField(hours, "createdAt", createdAt);
        ReflectionTestUtils.setField(hours, "updatedAt", updatedAt);

        OperatingHoursResponse response = CenterDtoMapper.toResponse(hours);
        assertNotNull(response);
        assertEquals(3001L, response.id());
        assertEquals(2001L, response.centerId());
        assertEquals(DayOfWeek.MONDAY, response.dayOfWeek());
        assertEquals(LocalTime.of(8, 30), response.opening());
        assertEquals(LocalTime.of(16, 30), response.closing());
        assertEquals(LocalDate.of(2026, 9, 1), response.effectiveFrom());
        assertEquals(LocalDate.of(2026, 12, 31), response.effectiveTo());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    @DisplayName("Maps CenterClosure to CenterClosureResponse")
    void mapsCenterClosure() {
        VaccinationCenter center = new VaccinationCenter(
                "CTR-GALLE-01",
                "Galle Community Health Hub",
                "78 Coast Road, Galle",
                "+94 91 2244668",
                CenterStatus.ACTIVE,
                80,
                40,
                "Asia/Colombo"
        );
        ReflectionTestUtils.setField(center, "id", 4001L);

        Instant startAt = Instant.parse("2026-10-01T08:00:00Z");
        Instant endAt = Instant.parse("2026-10-03T18:00:00Z");
        CenterClosure closure = new CenterClosure(
                center,
                startAt,
                endAt,
                "Facility electrical maintenance"
        );
        ReflectionTestUtils.setField(closure, "id", 5001L);
        Instant createdAt = Instant.parse("2026-08-10T12:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-10T12:00:00Z");
        ReflectionTestUtils.setField(closure, "createdAt", createdAt);
        ReflectionTestUtils.setField(closure, "updatedAt", updatedAt);

        CenterClosureResponse response = CenterDtoMapper.toResponse(closure);
        assertNotNull(response);
        assertEquals(5001L, response.id());
        assertEquals(4001L, response.centerId());
        assertEquals(startAt, response.startAt());
        assertEquals(endAt, response.endAt());
        assertEquals("Facility electrical maintenance", response.reason());
        assertEquals(CenterClosureStatus.ACTIVE, response.status());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }
}
