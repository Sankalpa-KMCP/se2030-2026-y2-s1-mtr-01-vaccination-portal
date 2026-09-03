package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.dto.response.CapacityPlanPreviewResponse;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionCapacityPlan;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.SessionCapacityPlanService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionCapacityPlanServiceIntegrationTests {

    @Autowired private SessionCapacityPlanService capacityPlanService;
    @Autowired private SessionCapacityPlanRepository planRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;

    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private VaccinationSession session;
    private OperationalStaffRoster roster1;
    private OperationalStaffRoster roster2;
    private OperationalStaffRoster roster3;
    private OperationalResource station1;
    private OperationalResource station2;
    private OperationalResource station3;
    private OperationalResource roomResource;

    private final Instant sessionStart = Instant.parse("2026-10-01T08:00:00Z");
    private final Instant sessionEnd = Instant.parse("2026-10-01T12:00:00Z"); // 4 hours = 240 minutes

    @BeforeEach
    void setUp() {
        center = centerRepository.save(new VaccinationCenter(
                "CTR-CAP-01", "Capacity Testing Center", "100 Health Way", "+12345",
                CenterStatus.ACTIVE, 60, 100, "UTC"
        ));

        VaccineType vaccine = vaccineTypeRepository.save(new VaccineType("VAX-CAP-01", "Capacity Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-CAP-01", "Capacity Group"));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-CAP-01", "Capacity Campaign", "Testing Capacity",
                vaccine, targetGroup,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.DRAFT
        ));

        // Dose programme: 10 minutes expected service time
        doseProgrammeRepository.save(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1,2", 21, false, 10,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)
        ));

        session = sessionRepository.save(new VaccinationSession(
                campaign, center,
                sessionStart, sessionEnd,
                Instant.parse("2026-10-01T07:00:00Z"), "UTC"
        ));

        // Roster entries
        roster1 = staffRosterRepository.save(new OperationalStaffRoster(center, 101L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        roster2 = staffRosterRepository.save(new OperationalStaffRoster(center, 102L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE));
        roster3 = staffRosterRepository.save(new OperationalStaffRoster(center, 103L, StaffRole.CHECK_IN_STAFF, OperationalStaffStatus.ACTIVE));

        // Resources: (center, code, resourceType, name, status)
        station1 = resourceRepository.save(new OperationalResource(center, "STATION-1", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE));
        station2 = resourceRepository.save(new OperationalResource(center, "STATION-2", ResourceType.VACCINATION_STATION, "Station 2", OperationalResourceStatus.ACTIVE));
        station3 = resourceRepository.save(new OperationalResource(center, "STATION-3", ResourceType.VACCINATION_STATION, "Station 3", OperationalResourceStatus.ACTIVE));
        roomResource = resourceRepository.save(new OperationalResource(center, "ROOM-1", ResourceType.ROOM, "Waiting Room", OperationalResourceStatus.ACTIVE));
    }

    @Test
    @DisplayName("previewCapacityPlan calculates correctly with active assignments")
    void previewCapacityPlan_calculatesAccurately() {
        // Assign 2 vaccinators and 1 check-in staff
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster3, StaffRole.CHECK_IN_STAFF, sessionStart, sessionEnd));

        // Assign 3 vaccination stations and 1 room
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station3, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, roomResource, sessionStart, sessionEnd));

        // Fully equipped = 2 -> effective = min(2 vaccinators, 3 stations, 2 equipped) = 2
        // Recommended throughput = 2 * (240 / 10) = 2 * 24 = 48
        PreviewCapacityPlanRequest request = new PreviewCapacityPlanRequest(2, null, null, null, 48, null);
        CapacityPlanPreviewResponse preview = capacityPlanService.previewCapacityPlan(session.getId(), request);

        assertEquals(240, preview.durationMinutes());
        assertEquals(10, preview.expectedServiceMinutes());
        assertEquals(2, preview.assignedVaccinatorCount());
        assertEquals(3, preview.availableStationCount());
        assertEquals(2, preview.fullyEquippedStationCount());
        assertEquals(2, preview.effectiveStationCount());
        assertEquals(48, preview.recommendedThroughput());
        assertEquals(48, preview.projectedApprovedCapacity());
        assertFalse(preview.overrideRequired());
        assertTrue(preview.overrideValid());
        assertEquals(60, preview.safeConcurrentOccupancy()); // Center default
        assertEquals(15, preview.expectedDwellMinutes()); // Default
        assertEquals(0.0, preview.expectedAccompanyingPersonsPerBooking(), 0.0001);
        assertEquals(100, preview.sessionServiceCeiling());
    }

    @Test
    @DisplayName("createCapacityPlan creates revision 1 and serializes to revision 2")
    void createCapacityPlan_createsSequentialRevisions() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd));

        // Create revision 1 anonymously (approvedBy from client is ignored -> null)
        CreateSessionCapacityPlanRequest req1 = new CreateSessionCapacityPlanRequest(
                2, 48, null, null, null, null, "client-spoofed-admin"
        );
        SessionCapacityPlan rev1 = capacityPlanService.createCapacityPlan(session.getId(), req1);

        assertNotNull(rev1.getId());
        assertEquals(1, rev1.getRevision());
        assertEquals(240, rev1.getDurationMinutes());
        assertEquals(2, rev1.getAssignedVaccinatorCount());
        assertEquals(2, rev1.getAvailableStationCount());
        assertEquals(2, rev1.getEffectiveStationCount());
        assertEquals(48, rev1.getRecommendedThroughput());
        assertEquals(48, rev1.getApprovedCapacity());
        assertNull(rev1.getApprovedBy()); // Ignored client input, anonymous actor -> null

        // Create revision 2 with authenticated actor (approvedBy is derived from actor userId)
        com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor actor2 =
                new com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor(102L, java.util.Set.of("ROLE_CENTER_MANAGER"));
        CreateSessionCapacityPlanRequest req2 = new CreateSessionCapacityPlanRequest(
                1, 24, null, null, null, null, "client-spoofed-admin-2"
        );
        SessionCapacityPlan rev2 = capacityPlanService.createCapacityPlan(session.getId(), req2, actor2);

        assertNotNull(rev2.getId());
        assertEquals(2, rev2.getRevision());
        assertEquals(1, rev2.getEffectiveStationCount());
        assertEquals(24, rev2.getRecommendedThroughput());
        assertEquals(24, rev2.getApprovedCapacity());
        assertEquals("102", rev2.getApprovedBy());

        // Verify history and latest
        List<SessionCapacityPlan> history = capacityPlanService.getCapacityPlansForSession(session.getId());
        assertEquals(2, history.size());
        assertEquals(1, history.get(0).getRevision());
        assertEquals(2, history.get(1).getRevision());

        SessionCapacityPlan latest = capacityPlanService.getLatestCapacityPlan(session.getId());
        assertEquals(2, latest.getRevision());
        assertEquals(24, latest.getApprovedCapacity());

        // Verify revision 1 remains immutable in DB
        SessionCapacityPlan loadedRev1 = capacityPlanService.getCapacityPlanByRevision(session.getId(), 1);
        assertEquals(1, loadedRev1.getRevision());
        assertEquals(48, loadedRev1.getApprovedCapacity());
    }

    @Test
    @DisplayName("createCapacityPlan handles all three bottleneck dimensions")
    void createCapacityPlan_bottleneckDimensions() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station3, sessionStart, sessionEnd));

        // 2 vaccinators, 3 stations, 3 equipped -> limited by vaccinators (effective 2)
        CreateSessionCapacityPlanRequest req1 = new CreateSessionCapacityPlanRequest(3, 48, null, null, null, null, "admin");
        SessionCapacityPlan plan1 = capacityPlanService.createCapacityPlan(session.getId(), req1);
        assertEquals(2, plan1.getEffectiveStationCount());
        assertEquals(48, plan1.getRecommendedThroughput());

        // Fully equipped = 1 -> limited by fully equipped (effective 1)
        CreateSessionCapacityPlanRequest req2 = new CreateSessionCapacityPlanRequest(1, 24, null, null, null, null, "admin");
        SessionCapacityPlan plan2 = capacityPlanService.createCapacityPlan(session.getId(), req2);
        assertEquals(1, plan2.getEffectiveStationCount());
        assertEquals(24, plan2.getRecommendedThroughput());
    }

    @Test
    @DisplayName("createCapacityPlan requires override reason when approved capacity exceeds recommended throughput")
    void createCapacityPlan_overrideReasonRule() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));

        // 1 vaccinator, 1 station, 1 equipped -> recommended = 24. Approved = 30 without actor -> throws AuthenticationCredentialsNotFoundException
        CreateSessionCapacityPlanRequest reqNoActor = new CreateSessionCapacityPlanRequest(
                1, 30, null, null, null, "Urgent booster demand", "admin"
        );
        org.springframework.security.authentication.AuthenticationCredentialsNotFoundException authEx = assertThrows(
                org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), reqNoActor, null));
        assertTrue(authEx.getMessage().contains("Full authentication is required"));

        // With unauthorized role (e.g. ROLE_CITIZEN) -> throws AccessDeniedException
        com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor citizenActor =
                new com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor(10L, java.util.Set.of("ROLE_CITIZEN"));
        org.springframework.security.access.AccessDeniedException citizenEx = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), reqNoActor, citizenActor));
        assertTrue(citizenEx.getMessage().contains("ROLE_CENTER_MANAGER is required"));

        // With ROLE_ADMIN only -> throws AccessDeniedException
        com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor adminActor =
                new com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor(1L, java.util.Set.of("ROLE_ADMIN"));
        org.springframework.security.access.AccessDeniedException adminEx = assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), reqNoActor, adminActor));
        assertTrue(adminEx.getMessage().contains("ROLE_CENTER_MANAGER is required"));

        // With ROLE_CENTER_MANAGER but missing override reason -> fails with IllegalArgumentException
        com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor managerActor =
                new com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor(42L, java.util.Set.of("ROLE_CENTER_MANAGER"));
        CreateSessionCapacityPlanRequest reqNoReason = new CreateSessionCapacityPlanRequest(
                1, 30, null, null, null, null, "spoofed-client-user"
        );
        IllegalArgumentException reasonEx = assertThrows(IllegalArgumentException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), reqNoReason, managerActor));
        assertTrue(reasonEx.getMessage().contains("override reason is required"));

        // With valid ROLE_CENTER_MANAGER and override reason -> succeeds and sets trusted approvedBy to "42" (ignoring client value)
        CreateSessionCapacityPlanRequest reqWithReason = new CreateSessionCapacityPlanRequest(
                1, 30, null, null, null, "Urgent booster demand", "spoofed-client-user"
        );
        SessionCapacityPlan plan = capacityPlanService.createCapacityPlan(session.getId(), reqWithReason, managerActor);
        assertEquals(30, plan.getApprovedCapacity());
        assertEquals("Urgent booster demand", plan.getOverrideReason());
        assertEquals("42", plan.getApprovedBy());
    }

    @Test
    @DisplayName("createCapacityPlan enforces hard session service ceiling")
    void createCapacityPlan_enforcesHardCeiling() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd));

        // Center ceiling is 100. Approved = 120 -> fails even with ROLE_CENTER_MANAGER and override reason
        com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor managerActor =
                new com.se2030.vaccinationportal.campaign.dto.request.AuthenticatedActor(42L, java.util.Set.of("ROLE_CENTER_MANAGER"));
        CreateSessionCapacityPlanRequest reqOverCeiling = new CreateSessionCapacityPlanRequest(
                2, 120, null, null, null, "Overriding ceiling", "admin"
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), reqOverCeiling, managerActor));
        assertTrue(ex.getMessage().contains("cannot exceed session service ceiling"));
    }

    @Test
    @DisplayName("createCapacityPlan rejects fully equipped stations exceeding available stations")
    void createCapacityPlan_rejectsEquippedExceedingAvailable() {
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
        // Available stations = 1. Fully equipped = 2 -> fails
        CreateSessionCapacityPlanRequest req = new CreateSessionCapacityPlanRequest(
                2, 24, null, null, null, null, "admin"
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), req));
        assertTrue(ex.getMessage().contains("cannot exceed available station count"));
    }

    @Test
    @DisplayName("createCapacityPlan rejects sessions not in DRAFT lifecycle status")
    void createCapacityPlan_rejectsNonDraftSession() {
        session.schedule();
        sessionRepository.saveAndFlush(session);

        CreateSessionCapacityPlanRequest req = new CreateSessionCapacityPlanRequest(
                0, 0, null, null, null, null, "admin"
        );
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), req));
        assertTrue(ex.getMessage().contains("only be created for sessions in DRAFT status"));
    }

    @Test
    @DisplayName("getCapacityPlansForSession throws ResourceNotFoundException for missing session")
    void getCapacityPlansForSession_missingSession() {
        assertThrows(ResourceNotFoundException.class,
                () -> capacityPlanService.getCapacityPlansForSession(9999L));
    }

    @Test
    @DisplayName("countAssignedVaccinators excludes inactive staff roster entries and non-vaccinators")
    void countAssignedVaccinators_excludesInactiveRosterStaffAndNonVaccinators() {
        // Create an inactive vaccinator roster
        OperationalStaffRoster inactiveVaccinatorRoster = staffRosterRepository.save(
                new OperationalStaffRoster(center, 104L, StaffRole.VACCINATOR, OperationalStaffStatus.INACTIVE)
        );

        // Assign 1 active vaccinator (roster1), 1 inactive vaccinator, 1 active check-in staff (roster3)
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, inactiveVaccinatorRoster, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster3, StaffRole.CHECK_IN_STAFF, sessionStart, sessionEnd));

        // Assign 2 stations
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd));

        // Preview with fullyEquipped = 2
        PreviewCapacityPlanRequest previewReq = new PreviewCapacityPlanRequest(2, null, null, null, 24, null);
        CapacityPlanPreviewResponse preview = capacityPlanService.previewCapacityPlan(session.getId(), previewReq);

        // assignedVaccinatorCount must be 1 (excluding inactive roster staff)
        assertEquals(1, preview.assignedVaccinatorCount());
        assertEquals(2, preview.availableStationCount());
        assertEquals(2, preview.fullyEquippedStationCount());
        assertEquals(1, preview.effectiveStationCount()); // min(1 vaccinator, 2 stations, 2 equipped) = 1
        assertEquals(24, preview.recommendedThroughput());

        // Create plan
        CreateSessionCapacityPlanRequest createReq = new CreateSessionCapacityPlanRequest(2, 24, null, null, null, null, "admin");
        SessionCapacityPlan plan = capacityPlanService.createCapacityPlan(session.getId(), createReq);

        assertEquals(1, plan.getAssignedVaccinatorCount());
        assertEquals(2, plan.getAvailableStationCount());
        assertEquals(1, plan.getEffectiveStationCount());
        assertEquals(24, plan.getRecommendedThroughput());
    }

    @Test
    @DisplayName("safeConcurrentOccupancy accepts null, equal to center limit, and conservative lower value")
    void safeConcurrentOccupancy_validInputs() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));

        // 1. Null occupancy -> resolves to center default (60)
        PreviewCapacityPlanRequest previewNull = new PreviewCapacityPlanRequest(1, null, null, null, 24, null);
        CapacityPlanPreviewResponse prevRespNull = capacityPlanService.previewCapacityPlan(session.getId(), previewNull);
        assertEquals(60, prevRespNull.safeConcurrentOccupancy());

        CreateSessionCapacityPlanRequest createNull = new CreateSessionCapacityPlanRequest(1, 24, null, null, null, null, "admin");
        SessionCapacityPlan planNull = capacityPlanService.createCapacityPlan(session.getId(), createNull);
        assertEquals(60, planNull.getSafeConcurrentOccupancy());

        // 2. Equal to center limit (60) -> accepted
        PreviewCapacityPlanRequest previewEqual = new PreviewCapacityPlanRequest(1, 60, null, null, 24, null);
        CapacityPlanPreviewResponse prevRespEqual = capacityPlanService.previewCapacityPlan(session.getId(), previewEqual);
        assertEquals(60, prevRespEqual.safeConcurrentOccupancy());

        CreateSessionCapacityPlanRequest createEqual = new CreateSessionCapacityPlanRequest(1, 24, 60, null, null, null, "admin");
        SessionCapacityPlan planEqual = capacityPlanService.createCapacityPlan(session.getId(), createEqual);
        assertEquals(60, planEqual.getSafeConcurrentOccupancy());

        // 3. Conservative lower value (25 < 60) -> accepted
        PreviewCapacityPlanRequest previewLower = new PreviewCapacityPlanRequest(1, 25, null, null, 24, null);
        CapacityPlanPreviewResponse prevRespLower = capacityPlanService.previewCapacityPlan(session.getId(), previewLower);
        assertEquals(25, prevRespLower.safeConcurrentOccupancy());

        CreateSessionCapacityPlanRequest createLower = new CreateSessionCapacityPlanRequest(1, 24, 25, null, null, null, "admin");
        SessionCapacityPlan planLower = capacityPlanService.createCapacityPlan(session.getId(), createLower);
        assertEquals(25, planLower.getSafeConcurrentOccupancy());
    }

    @Test
    @DisplayName("safeConcurrentOccupancy rejects values exceeding center safe limit and persists no plan")
    void safeConcurrentOccupancy_rejectsAboveCenterLimit() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));

        // Center safe limit is 60. Request with 65 -> rejected in preview
        PreviewCapacityPlanRequest previewOver = new PreviewCapacityPlanRequest(1, 65, null, null, 24, null);
        IllegalArgumentException previewEx = assertThrows(IllegalArgumentException.class,
                () -> capacityPlanService.previewCapacityPlan(session.getId(), previewOver));
        assertTrue(previewEx.getMessage().contains("cannot exceed center safety limit (60)"));

        // Request with 100 -> rejected in create
        CreateSessionCapacityPlanRequest createOver = new CreateSessionCapacityPlanRequest(1, 24, 100, null, null, null, "admin");
        IllegalArgumentException createEx = assertThrows(IllegalArgumentException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), createOver));
        assertTrue(createEx.getMessage().contains("cannot exceed center safety limit (60)"));

        // Verify no plan was persisted
        List<SessionCapacityPlan> plans = planRepository.findBySessionIdOrderByRevisionAsc(session.getId());
        assertTrue(plans.isEmpty());
    }

    @Test
    @DisplayName("interval-aware staff capacity calculates minimum concurrent distinct vaccinators")
    void intervalAwareStaffCapacity_scenarios() {
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, sessionEnd));

        Instant midSession = Instant.parse("2026-10-01T10:00:00Z"); // 2 hours in
        Instant threeQuarter = Instant.parse("2026-10-01T11:00:00Z"); // 3 hours in

        // 1. Partial coverage (roster1 assigned only 08:00-10:00) -> whole session capacity = 0
        StaffAssignment partial = staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, midSession));
        PreviewCapacityPlanRequest req1 = new PreviewCapacityPlanRequest(0, null, null, null, 0, null);
        CapacityPlanPreviewResponse prev1 = capacityPlanService.previewCapacityPlan(session.getId(), req1);
        assertEquals(0, prev1.assignedVaccinatorCount());
        assertEquals(0, prev1.effectiveStationCount());
        assertEquals(0, prev1.recommendedThroughput());

        // 2. Same vaccinator with adjacent rows spanning session (08:00-10:00, 10:00-12:00) -> 1 (not 2)
        StaffAssignment adjacent = staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, midSession, sessionEnd));
        CapacityPlanPreviewResponse prev2 = capacityPlanService.previewCapacityPlan(session.getId(), req1);
        assertEquals(1, prev2.assignedVaccinatorCount());
        assertEquals(1, prev2.availableStationCount());

        // Clean up staff assignments for next scenario
        staffAssignmentRepository.delete(partial);
        staffAssignmentRepository.delete(adjacent);
        staffAssignmentRepository.flush();

        // 3. Staggered overlapping vaccinators (v1: 08:00-11:00, v2: 10:00-12:00) -> min concurrent = 1
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, threeQuarter));
        staffAssignmentRepository.save(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, midSession, sessionEnd));
        CapacityPlanPreviewResponse prev3 = capacityPlanService.previewCapacityPlan(session.getId(), req1);
        assertEquals(1, prev3.assignedVaccinatorCount());
    }

    @Test
    @DisplayName("interval-aware station capacity calculates minimum concurrent distinct stations and bounds fully equipped")
    void intervalAwareStationCapacity_scenarios() {
        staffAssignmentRepository.save(new StaffAssignment(session, roster1, StaffRole.VACCINATOR, sessionStart, sessionEnd));
        staffAssignmentRepository.save(new StaffAssignment(session, roster2, StaffRole.VACCINATOR, sessionStart, sessionEnd));

        Instant midSession = Instant.parse("2026-10-01T10:00:00Z");
        Instant threeQuarter = Instant.parse("2026-10-01T11:00:00Z");

        // 1. Partial station assignment (station1 08:00-10:00) -> availableStationCount = 0
        SessionResourceAssignment partial = resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, midSession));
        PreviewCapacityPlanRequest req0 = new PreviewCapacityPlanRequest(0, null, null, null, 0, null);
        CapacityPlanPreviewResponse prev1 = capacityPlanService.previewCapacityPlan(session.getId(), req0);
        assertEquals(0, prev1.availableStationCount());

        // 2. Same station with adjacent rows (08:00-10:00, 10:00-12:00) -> 1 (not 2)
        SessionResourceAssignment adjacent = resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, midSession, sessionEnd));
        CapacityPlanPreviewResponse prev2 = capacityPlanService.previewCapacityPlan(session.getId(), req0);
        assertEquals(1, prev2.availableStationCount());

        // Clean up
        resourceAssignmentRepository.delete(partial);
        resourceAssignmentRepository.delete(adjacent);
        resourceAssignmentRepository.flush();

        // 3. Staggered overlapping stations (s1: 08:00-11:00, s2: 10:00-12:00) -> sustainable availableStationCount = 1
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station1, sessionStart, threeQuarter));
        resourceAssignmentRepository.save(new SessionResourceAssignment(session, station2, sessionStart, sessionEnd)); // s2 full
        // Here s2 is full (08:00-12:00), s1 is 08:00-11:00.
        // Segments: [08:00, 11:00) -> {s1, s2} (2), [11:00, 12:00) -> {s2} (1). Min concurrent = 1.
        CapacityPlanPreviewResponse prev3 = capacityPlanService.previewCapacityPlan(session.getId(), req0);
        assertEquals(1, prev3.availableStationCount());

        // 4. Request fullyEquipped = 2 when sustainable available stations = 1 -> rejected
        CreateSessionCapacityPlanRequest invalidEquipped = new CreateSessionCapacityPlanRequest(2, 24, null, null, null, null, "admin");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> capacityPlanService.createCapacityPlan(session.getId(), invalidEquipped));
        assertTrue(ex.getMessage().contains("cannot exceed available station count (1)"));
    }
}
