package com.se2030.vaccinationportal.campaign.center.model;

import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;


import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalResourceModelTests {

    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private VaccinationSession session;

    @BeforeEach
    void setUp() {
        center = new VaccinationCenter("CTR-OP-01", "Operational Center", "123 Health Ave", "+1234567890", CenterStatus.ACTIVE, 50, 100, "UTC");
        VaccineType vaccine = new VaccineType("VAX-OP-01", "Operational Vaccine");
        TargetGroup targetGroup = new TargetGroup("TG-OP-01", "Operational Target Group");
        campaign = new VaccinationCampaign(
                "CMP-OP-01",
                "COVID Autumn",
                "COVID-19",
                vaccine,
                targetGroup,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                Instant.now(),
                Instant.now().plusSeconds(86400 * 90)
        );
        session = new VaccinationSession(
                campaign,
                center,
                Instant.parse("2026-10-01T09:00:00Z"),
                Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-10-01T08:00:00Z"),
                "UTC"
        );
    }

    @Test
    void operationalResource_validatesParameters() {
        OperationalResource resource = new OperationalResource(center, "ROOM-101", ResourceType.ROOM, "Main Vaccination Room");
        assertEquals("ROOM-101", resource.getCode());
        assertEquals(ResourceType.ROOM, resource.getResourceType());
        assertEquals("Main Vaccination Room", resource.getName());
        assertEquals(OperationalResourceStatus.ACTIVE, resource.getStatus());
        assertTrue(resource.isActive());

        assertThrows(IllegalArgumentException.class, () -> new OperationalResource(null, "ROOM-101", ResourceType.ROOM, "Name"));
        assertThrows(IllegalArgumentException.class, () -> new OperationalResource(center, null, ResourceType.ROOM, "Name"));
        assertThrows(IllegalArgumentException.class, () -> new OperationalResource(center, "  ", ResourceType.ROOM, "Name"));
        assertThrows(IllegalArgumentException.class, () -> new OperationalResource(center, "ROOM-101", null, "Name"));
        assertThrows(IllegalArgumentException.class, () -> new OperationalResource(center, "ROOM-101", ResourceType.ROOM, null));
        assertThrows(IllegalArgumentException.class, () -> new OperationalResource(center, "ROOM-101", ResourceType.ROOM, "  "));
    }

    @Test
    void operationalStaffRoster_validatesParameters() {
        OperationalStaffRoster roster = new OperationalStaffRoster(center, 100L, StaffRole.VACCINATOR);
        assertEquals(100L, roster.getUserId());
        assertEquals(StaffRole.VACCINATOR, roster.getRole());
        assertEquals(OperationalStaffStatus.ACTIVE, roster.getStatus());
        assertTrue(roster.isActive());

        assertThrows(IllegalArgumentException.class, () -> new OperationalStaffRoster(null, 100L, StaffRole.VACCINATOR));
        assertThrows(IllegalArgumentException.class, () -> new OperationalStaffRoster(center, null, StaffRole.VACCINATOR));
        assertThrows(IllegalArgumentException.class, () -> new OperationalStaffRoster(center, 100L, null));
    }

    @Test
    void sessionResourceAssignment_validatesInterval() {
        OperationalResource resource = new OperationalResource(center, "STATION-01", ResourceType.VACCINATION_STATION, "Station 1");
        Instant start = Instant.parse("2026-10-01T09:00:00Z");
        Instant end = Instant.parse("2026-10-01T12:00:00Z");

        SessionResourceAssignment assignment = new SessionResourceAssignment(session, resource, start, end);
        assertEquals(session, assignment.getSession());
        assertEquals(resource, assignment.getResource());
        assertEquals(start, assignment.getStartAt());
        assertEquals(end, assignment.getEndAt());

        assertThrows(IllegalArgumentException.class, () -> new SessionResourceAssignment(null, resource, start, end));
        assertThrows(IllegalArgumentException.class, () -> new SessionResourceAssignment(session, null, start, end));
        assertThrows(IllegalArgumentException.class, () -> new SessionResourceAssignment(session, resource, null, end));
        assertThrows(IllegalArgumentException.class, () -> new SessionResourceAssignment(session, resource, start, null));
        assertThrows(IllegalArgumentException.class, () -> new SessionResourceAssignment(session, resource, end, start)); // end before start
        assertThrows(IllegalArgumentException.class, () -> new SessionResourceAssignment(session, resource, start, start)); // zero duration
    }

    @Test
    void staffAssignment_validatesInterval() {
        OperationalStaffRoster roster = new OperationalStaffRoster(center, 200L, StaffRole.VACCINATOR);
        Instant start = Instant.parse("2026-10-01T09:00:00Z");
        Instant end = Instant.parse("2026-10-01T12:00:00Z");

        StaffAssignment assignment = new StaffAssignment(session, roster, StaffRole.VACCINATOR, start, end);
        assertEquals(session, assignment.getSession());
        assertEquals(roster, assignment.getStaffRoster());
        assertEquals(200L, assignment.getUserId());
        assertEquals(StaffRole.VACCINATOR, assignment.getRole());

        assertThrows(IllegalArgumentException.class, () -> new StaffAssignment(null, roster, StaffRole.VACCINATOR, start, end));
        assertThrows(IllegalArgumentException.class, () -> new StaffAssignment(session, null, StaffRole.VACCINATOR, start, end));
        assertThrows(IllegalArgumentException.class, () -> new StaffAssignment(session, roster, null, start, end));
        assertThrows(IllegalArgumentException.class, () -> new StaffAssignment(session, roster, StaffRole.VACCINATOR, end, start));
    }

    @Test
    void sessionCapacityPlan_validatesCalculatedInvariants() {
        // Valid plan: 3 vaccinators, 4 stations, 3 fully equipped -> effective 3
        // 180 min duration, 10 min service -> recommended 3 * 18 = 54
        // approved 54, safe occupancy 60, dwell 15, ratio 0.0, ceiling 100
        SessionCapacityPlan plan = new SessionCapacityPlan(
                session,
                1,
                180,
                10,
                3,
                4,
                3,
                3,
                54,
                54,
                60,
                15,
                0.0,
                100,
                null,
                "actor-admin"
        );

        assertEquals(1, plan.getRevision());
        assertEquals(3, plan.getEffectiveStationCount());
        assertEquals(54, plan.getRecommendedThroughput());
        assertEquals(54, plan.getApprovedCapacity());
        assertNull(plan.getOverrideReason());
        assertEquals("actor-admin", plan.getApprovedBy());
    }

    @Test
    void sessionCapacityPlan_overrideRequiresReason() {
        // Approved capacity (60) > recommended throughput (54) requires override reason
        assertThrows(IllegalArgumentException.class, () -> new SessionCapacityPlan(
                session, 1, 180, 10, 3, 4, 3, 3, 54, 60, 60, 15, 0.0, 100, null, "actor-admin"
        ));

        assertThrows(IllegalArgumentException.class, () -> new SessionCapacityPlan(
                session, 1, 180, 10, 3, 4, 3, 3, 54, 60, 60, 15, 0.0, 100, "   ", "actor-admin"
        ));

        // With valid override reason
        SessionCapacityPlan planWithOverride = new SessionCapacityPlan(
                session, 1, 180, 10, 3, 4, 3, 3, 54, 60, 60, 15, 0.0, 100, "High demand community clinic", "actor-admin"
        );
        assertEquals("High demand community clinic", planWithOverride.getOverrideReason());
    }

    @Test
    void sessionCapacityPlan_rejectsExceedingCeiling() {
        // Ceiling is 50, approved capacity is 54 -> rejected
        assertThrows(IllegalArgumentException.class, () -> new SessionCapacityPlan(
                session, 1, 180, 10, 3, 4, 3, 3, 54, 54, 60, 15, 0.0, 50, null, "actor-admin"
        ));
    }

    @Test
    void sessionCapacityPlan_rejectsFullyEquippedGreaterThanAvailable() {
        // Fully equipped (5) > available (4) -> rejected
        assertThrows(IllegalArgumentException.class, () -> new SessionCapacityPlan(
                session, 1, 180, 10, 3, 4, 5, 3, 54, 54, 60, 15, 0.0, 100, null, "actor-admin"
        ));
    }
}
