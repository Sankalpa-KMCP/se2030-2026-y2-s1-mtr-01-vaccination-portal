package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.StaffAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class OperationalStatusReverseImpactIntegrationTests {

    @Autowired private OperationalResourceService resourceService;
    @Autowired private OperationalStaffRosterService staffService;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private OperationalStaffRosterRepository staffRosterRepository;
    @Autowired private SessionResourceAssignmentRepository resourceAssignmentRepository;
    @Autowired private StaffAssignmentRepository staffAssignmentRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private Clock clock;

    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private OperationalResource resource;
    private OperationalStaffRoster staffRoster;

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-STATUS-IMPACT", "Status impact vaccine", "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-STATUS-IMPACT", "Status impact target", true)
        );
        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-STATUS-IMPACT",
                "Status impact center",
                "100 Test Way",
                "555-0100",
                CenterStatus.ACTIVE,
                50,
                20,
                "UTC"
        ));
        LocalDate today = LocalDate.now(clock);
        campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-STATUS-IMPACT",
                "Status impact campaign",
                "Desc",
                vaccineType,
                targetGroup,
                today.minusDays(10),
                today.plusDays(30),
                clock.instant().minusSeconds(3600),
                clock.instant().plusSeconds(86400 * 30),
                CampaignStatus.ACTIVE
        ));
        resource = resourceRepository.saveAndFlush(new OperationalResource(
                center,
                "STATION-STATUS-IMPACT",
                ResourceType.VACCINATION_STATION,
                "Status impact station",
                OperationalResourceStatus.ACTIVE
        ));
        staffRoster = staffRosterRepository.saveAndFlush(new OperationalStaffRoster(
                center,
                9001L,
                StaffRole.VACCINATOR,
                OperationalStaffStatus.ACTIVE
        ));
    }

    @Test
    @DisplayName("Resource assigned to a blocking session cannot be deactivated")
    void resourceAssignedToBlockingSessionCannotBeDeactivated() {
        assignResource(createSession(SessionLifecycleStatus.SCHEDULED));

        assertThrows(IllegalStateException.class, () ->
                resourceService.updateResourceStatus(resource.getId(), OperationalResourceStatus.INACTIVE)
        );
        assertEquals(OperationalResourceStatus.ACTIVE, resource.getStatus());
    }

    @Test
    @DisplayName("Full resource update cannot bypass blocking-session deactivation protection")
    void fullResourceUpdateCannotBypassDeactivationProtection() {
        assignResource(createSession(SessionLifecycleStatus.DRAFT));

        assertThrows(IllegalStateException.class, () -> resourceService.updateResource(
                center.getId(),
                resource.getId(),
                "Renamed station",
                OperationalResourceStatus.INACTIVE
        ));
        assertEquals("Status impact station", resource.getName());
        assertEquals(OperationalResourceStatus.ACTIVE, resource.getStatus());
    }

    @Test
    @DisplayName("Resource without a blocking dependency can be deactivated")
    void resourceWithoutBlockingDependencyCanBeDeactivated() {
        OperationalResource updated = resourceService.updateResourceStatus(
                resource.getId(), OperationalResourceStatus.INACTIVE
        );

        assertEquals(OperationalResourceStatus.INACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Completed session resource dependency does not block deactivation")
    void completedSessionResourceDependencyDoesNotBlockDeactivation() {
        assignResource(createSession(SessionLifecycleStatus.COMPLETED));

        OperationalResource updated = resourceService.updateResourceStatus(
                resource.getId(), OperationalResourceStatus.INACTIVE
        );

        assertEquals(OperationalResourceStatus.INACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Inactive resource can be reactivated")
    void inactiveResourceCanBeReactivated() {
        resource.setStatus(OperationalResourceStatus.INACTIVE);
        resourceRepository.saveAndFlush(resource);

        OperationalResource updated = resourceService.updateResourceStatus(
                resource.getId(), OperationalResourceStatus.ACTIVE
        );

        assertEquals(OperationalResourceStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Staff assigned to a blocking session cannot be deactivated")
    void staffAssignedToBlockingSessionCannotBeDeactivated() {
        assignStaff(createSession(SessionLifecycleStatus.IN_PROGRESS));

        assertThrows(IllegalStateException.class, () ->
                staffService.updateStaffStatus(staffRoster.getId(), OperationalStaffStatus.INACTIVE)
        );
        assertEquals(OperationalStaffStatus.ACTIVE, staffRoster.getStatus());
    }

    @Test
    @DisplayName("Full staff-roster update cannot bypass blocking-session deactivation protection")
    void fullStaffRosterUpdateCannotBypassDeactivationProtection() {
        assignStaff(createSession(SessionLifecycleStatus.SCHEDULED));

        assertThrows(IllegalStateException.class, () -> staffService.updateStaffRoster(
                center.getId(),
                staffRoster.getId(),
                StaffRole.SUPERVISOR,
                OperationalStaffStatus.INACTIVE
        ));
        assertEquals(StaffRole.VACCINATOR, staffRoster.getRole());
        assertEquals(OperationalStaffStatus.ACTIVE, staffRoster.getStatus());
    }

    @Test
    @DisplayName("Staff without a blocking dependency can be deactivated")
    void staffWithoutBlockingDependencyCanBeDeactivated() {
        OperationalStaffRoster updated = staffService.updateStaffStatus(
                staffRoster.getId(), OperationalStaffStatus.INACTIVE
        );

        assertEquals(OperationalStaffStatus.INACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Cancelled session staff dependency does not block deactivation")
    void cancelledSessionStaffDependencyDoesNotBlockDeactivation() {
        assignStaff(createSession(SessionLifecycleStatus.CANCELLED));

        OperationalStaffRoster updated = staffService.updateStaffStatus(
                staffRoster.getId(), OperationalStaffStatus.INACTIVE
        );

        assertEquals(OperationalStaffStatus.INACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Inactive staff roster entry can be reactivated")
    void inactiveStaffCanBeReactivated() {
        staffRoster.setStatus(OperationalStaffStatus.INACTIVE);
        staffRosterRepository.saveAndFlush(staffRoster);

        OperationalStaffRoster updated = staffService.updateStaffStatus(
                staffRoster.getId(), OperationalStaffStatus.ACTIVE
        );

        assertEquals(OperationalStaffStatus.ACTIVE, updated.getStatus());
    }

    private VaccinationSession createSession(SessionLifecycleStatus status) {
        Instant startAt = clock.instant().plusSeconds(3600);
        Instant endAt = startAt.plusSeconds(7200);
        VaccinationSession session = new VaccinationSession(
                campaign,
                center,
                startAt,
                endAt,
                startAt.minusSeconds(1800),
                "UTC"
        );
        switch (status) {
            case DRAFT -> {
            }
            case SCHEDULED -> session.schedule();
            case IN_PROGRESS -> {
                session.schedule();
                session.start();
            }
            case COMPLETED -> {
                session.schedule();
                session.start();
                session.complete();
            }
            case CANCELLED -> session.cancel();
        }
        return sessionRepository.saveAndFlush(session);
    }

    private void assignResource(VaccinationSession session) {
        resourceAssignmentRepository.saveAndFlush(new SessionResourceAssignment(
                session, resource, session.getStartAt(), session.getEndAt()
        ));
    }

    private void assignStaff(VaccinationSession session) {
        staffAssignmentRepository.saveAndFlush(new StaffAssignment(
                session, staffRoster, StaffRole.VACCINATOR, session.getStartAt(), session.getEndAt()
        ));
    }
}
