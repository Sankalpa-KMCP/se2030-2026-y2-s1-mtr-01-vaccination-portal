package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionCapacityPlanRequest;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.OperatingHours;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.OperatingHoursRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionCapacityPlanRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionOperationalReadinessServiceIntegrationTests {

    @Autowired private VaccinationSessionService sessionService;
    @Autowired private SessionOperationalReadinessService readinessService;
    @Autowired private SessionCapacityPlanService capacityPlanService;
    @Autowired private SessionResourceAssignmentService resourceAssignmentService;
    @Autowired private StaffAssignmentService staffAssignmentService;
    @Autowired private OperationalResourceService resourceService;
    @Autowired private OperationalStaffRosterService rosterService;

    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private CampaignCenterAssignmentRepository centerAssignmentRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private CenterClosureRepository closureRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository rosterRepository;
    @Autowired private SessionResourceAssignmentRepository sessionResourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private SessionCapacityPlanRepository capacityPlanRepository;
    @Autowired private CampaignDoseProgrammeRepository doseProgrammeRepository;
    @Autowired private EntityManager entityManager;

    private VaccinationCampaign campaign;
    private VaccinationCenter center;
    private VaccinationSession session;
    private OperationalResource stationResource;
    private OperationalStaffRoster vaccinatorRoster;

    private final LocalDate CAMPAIGN_START = LocalDate.of(2026, 10, 1);
    private final LocalDate CAMPAIGN_END = LocalDate.of(2026, 10, 31);
    private final Instant SESSION_START = Instant.parse("2026-10-15T09:00:00Z");
    private final Instant SESSION_END = Instant.parse("2026-10-15T12:00:00Z"); // 180 min
    private final Instant BOOKING_CLOSE = Instant.parse("2026-10-15T08:00:00Z");

    @BeforeEach
    void setUp() {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-RDY", "Readiness Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(new TargetGroup("TG-RDY", "Readiness Target Group"));

        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-RDY", "Readiness Center", "123 Health Ave", "0112345678",
                CenterStatus.ACTIVE, 50, 100, "UTC"
        ));

        // 7-day operating hours 08:00 - 18:00
        for (DayOfWeek dow : DayOfWeek.values()) {
            operatingHoursRepository.saveAndFlush(new OperatingHours(
                    center, dow, LocalTime.of(8, 0), LocalTime.of(18, 0), CAMPAIGN_START, CAMPAIGN_END
            ));
        }

        campaign = campaignRepository.saveAndFlush(new VaccinationCampaign(
                "CMP-RDY", "Readiness Campaign", "Description", vaccine, targetGroup,
                CAMPAIGN_START, CAMPAIGN_END,
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-31T23:59:59Z")
        ));
        campaign.publish();
        campaign = campaignRepository.saveAndFlush(campaign);

        centerAssignmentRepository.saveAndFlush(new CampaignCenterAssignment(
                campaign, center, CAMPAIGN_START, CAMPAIGN_END.plusDays(1)
        ));

        doseProgrammeRepository.saveAndFlush(new CampaignDoseProgramme(
                campaign, 1, DoseCategory.PRIMARY, "1", 0, false, 10, CAMPAIGN_START, null
        ));

        session = sessionRepository.saveAndFlush(new VaccinationSession(
                campaign, center, SESSION_START, SESSION_END, BOOKING_CLOSE, "UTC"
        ));

        stationResource = resourceService.createResource(
                center.getId(), "STATION-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE
        );

        vaccinatorRoster = rosterService.registerStaffMember(
                center.getId(), 5001L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        );
    }

    private void configureFullReadiness() {
        resourceAssignmentService.createAssignment(
                session.getId(), stationResource.getId(), SESSION_START, SESSION_END
        );
        staffAssignmentService.createAssignment(
                session.getId(), vaccinatorRoster.getId(), StaffRole.VACCINATOR, SESSION_START, SESSION_END
        );
        capacityPlanService.createCapacityPlan(
                session.getId(),
                new CreateSessionCapacityPlanRequest(1, 18, 50, 15, 0.0, null, "Planner")
        );
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("assertReadyForScheduling succeeds when session has complete, valid operational configuration")
    void assertReadyForScheduling_completeSession_succeeds() {
        configureFullReadiness();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        readinessService.assertReadyForScheduling(lockedCampaign, lockedSession);

        SessionOperationalReadinessService.OperationalReadinessResult result =
                readinessService.evaluateReadiness(session.getId());
        assertThat(result.ready()).isTrue();
        assertThat(result.violations()).isEmpty();
        assertThat(result.assignedVaccinators()).isEqualTo(1);
        assertThat(result.availableStations()).isEqualTo(1);
        assertThat(result.effectiveStations()).isEqualTo(1);
        assertThat(result.recommendedThroughput()).isEqualTo(18);
        assertThat(result.approvedCapacity()).isEqualTo(18);
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when no capacity plan exists")
    void assertReadyForScheduling_noCapacityPlan_throwsConflict() {
        resourceAssignmentService.createAssignment(
                session.getId(), stationResource.getId(), SESSION_START, SESSION_END
        );
        staffAssignmentService.createAssignment(
                session.getId(), vaccinatorRoster.getId(), StaffRole.VACCINATOR, SESSION_START, SESSION_END
        );
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No approved capacity plan exists");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when zero vaccination stations assigned")
    void assertReadyForScheduling_zeroStations_throwsConflict() {
        staffAssignmentService.createAssignment(
                session.getId(), vaccinatorRoster.getId(), StaffRole.VACCINATOR, SESSION_START, SESSION_END
        );
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no active vaccination station resources assigned");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when zero vaccinators assigned")
    void assertReadyForScheduling_zeroVaccinators_throwsConflict() {
        resourceAssignmentService.createAssignment(
                session.getId(), stationResource.getId(), SESSION_START, SESSION_END
        );
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no active vaccinator staff assigned");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when assigned resource status was changed to MAINTENANCE")
    void assertReadyForScheduling_resourceInMaintenance_throwsConflict() {
        configureFullReadiness();

        resourceService.updateResource(center.getId(), stationResource.getId(), "Station 1", OperationalResourceStatus.MAINTENANCE);
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when assigned staff roster status was changed to INACTIVE")
    void assertReadyForScheduling_staffInactive_throwsConflict() {
        configureFullReadiness();

        rosterService.updateStaffRoster(center.getId(), vaccinatorRoster.getId(), StaffRole.VACCINATOR, OperationalStaffStatus.INACTIVE);
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when staff roster qualification changed away from VACCINATOR")
    void assertReadyForScheduling_staffQualificationMismatch_throwsConflict() {
        configureFullReadiness();

        rosterService.updateStaffRoster(center.getId(), vaccinatorRoster.getId(), StaffRole.CHECK_IN_STAFF, OperationalStaffStatus.ACTIVE);
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer matches assignment qualification");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when capacity plan is stale after adding extra vaccinator")
    void assertReadyForScheduling_staleCapacityPlanVaccinatorCount_throwsConflict() {
        configureFullReadiness();

        OperationalStaffRoster vaccinator2 = rosterService.registerStaffMember(
                center.getId(), 5002L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        );
        staffAssignmentService.createAssignment(
                session.getId(), vaccinator2.getId(), StaffRole.VACCINATOR, SESSION_START, SESSION_END
        );
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is stale relative to current assigned vaccinators");
    }

    @Test
    @DisplayName("assertReadyForScheduling fails when center closure overlaps session")
    void assertReadyForScheduling_overlappingCenterClosure_throwsConflict() {
        configureFullReadiness();

        closureRepository.saveAndFlush(new CenterClosure(
                center, SESSION_START, SESSION_END, "Emergency maintenance"
        ));
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign lockedCampaign = campaignRepository.findByIdForUpdate(campaign.getId()).orElseThrow();
        VaccinationSession lockedSession = sessionRepository.findByIdForUpdate(session.getId()).orElseThrow();

        assertThatThrownBy(() -> readinessService.assertReadyForScheduling(lockedCampaign, lockedSession))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlaps an active center closure");
    }

    @Test
    @DisplayName("scheduleSession transitions complete session from DRAFT to SCHEDULED")
    void scheduleSession_successPath() {
        configureFullReadiness();

        VaccinationSession scheduled = sessionService.scheduleSession(session.getId());
        assertThat(scheduled.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.SCHEDULED);

        entityManager.clear();
        VaccinationSession reloaded = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(reloaded.getLifecycleStatus()).isEqualTo(SessionLifecycleStatus.SCHEDULED);
    }
}
