package com.se2030.vaccinationportal.campaign.session.planning.web;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.center.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.center.model.ResourceType;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionResourceAssignment;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionResourceAssignmentRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.session.planning.service.SessionResourceAssignmentService;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionResourceAssignmentReadIntegrationTests {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T12:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccineTypeRepository vaccineRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private OperationalResourceRepository resourceRepository;
    @Autowired private SessionResourceAssignmentRepository assignmentRepository;
    @Autowired private SessionResourceAssignmentService assignmentService;

    private Long vaccineId;
    private Long targetGroupId;
    private Long centerId;
    private Long campaignId;
    private Long sessionId;
    private Long resourceId;
    private Long secondResourceId;
    private String resourceCode;

    @BeforeEach
    void persistFixtureOutsideHttpTransaction() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        resourceCode = "STATION-READ-" + suffix;
        transactionTemplate.executeWithoutResult(ignored -> {
            VaccineType vaccine = vaccineRepository.save(new VaccineType("VAX-READ-" + suffix, "Read Test Vaccine"));
            TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-READ-" + suffix, "Read Test Group"));
            VaccinationCenter center = centerRepository.save(new VaccinationCenter(
                    "CTR-READ-" + suffix, "Read Test Center", "123 Health Ave", "0112345678",
                    CenterStatus.ACTIVE, 50, 100, "UTC"
            ));
            VaccinationCampaign campaign = campaignRepository.save(new VaccinationCampaign(
                    "CMP-READ-" + suffix, "Read Test Campaign", "Description", vaccine, targetGroup,
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                    Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z")
            ));
            VaccinationSession session = sessionRepository.save(new VaccinationSession(
                    campaign, center, START, END, START.minusSeconds(3600), "UTC"
            ));
            OperationalResource resource = resourceRepository.save(new OperationalResource(
                    center, resourceCode, ResourceType.VACCINATION_STATION,
                    "Read Test Station", OperationalResourceStatus.ACTIVE
            ));

            entityManager.flush();
            vaccineId = vaccine.getId();
            targetGroupId = targetGroup.getId();
            centerId = center.getId();
            campaignId = campaign.getId();
            sessionId = session.getId();
            resourceId = resource.getId();
            entityManager.clear();
        });
    }

    @AfterEach
    void removeFixtureFromTestDatabase() {
        transactionTemplate.executeWithoutResult(ignored -> {
            assignmentRepository.deleteAll(assignmentRepository.findBySessionIdOrderByStartAtAscIdAsc(sessionId));
            sessionRepository.deleteById(sessionId);
            resourceRepository.deleteById(resourceId);
            if (secondResourceId != null) {
                resourceRepository.deleteById(secondResourceId);
            }
            campaignRepository.deleteById(campaignId);
            centerRepository.deleteById(centerId);
            vaccineRepository.deleteById(vaccineId);
            targetGroupRepository.deleteById(targetGroupId);
        });
    }

    private Long persistAssignment() {
        return persistAssignment(resourceId);
    }

    private Long persistAssignment(Long assignedResourceId) {
        return transactionTemplate.execute(ignored -> {
            VaccinationSession session = sessionRepository.findById(sessionId).orElseThrow();
            OperationalResource resource = resourceRepository.findById(assignedResourceId).orElseThrow();
            SessionResourceAssignment assignment = assignmentRepository.save(
                    new SessionResourceAssignment(session, resource, START, END)
            );
            entityManager.flush();
            Long assignmentId = assignment.getId();
            entityManager.clear();
            return assignmentId;
        });
    }

    private String authorization() {
        return "Bearer " + jwtTokenService.generateToken(102L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
    }

    @Test
    void listReturnsPersistedResourceDetailsAfterTransactionCloses() throws Exception {
        Long assignmentId = persistAssignment();

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/resource-assignments", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(assignmentId.intValue())))
                .andExpect(jsonPath("$[0].resourceId", is(resourceId.intValue())))
                .andExpect(jsonPath("$[0].resourceCode", is(resourceCode)))
                .andExpect(jsonPath("$[0].resourceName", is("Read Test Station")))
                .andExpect(jsonPath("$[0].resourceType", is("VACCINATION_STATION")));
    }

    @Test
    void listReturnsEmptyArrayForSessionWithoutAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{sessionId}/resource-assignments", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void singleReadReturnsPersistedResourceDetailsAfterTransactionCloses() throws Exception {
        Long assignmentId = persistAssignment();

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/resource-assignments/{assignmentId}", sessionId, assignmentId)
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(assignmentId.intValue())))
                .andExpect(jsonPath("$.resourceId", is(resourceId.intValue())))
                .andExpect(jsonPath("$.resourceCode", is(resourceCode)))
                .andExpect(jsonPath("$.resourceName", is("Read Test Station")))
                .andExpect(jsonPath("$.resourceType", is("VACCINATION_STATION")));
    }

    @Test
    void listReturnsAllAssignmentsWithResourcesInitialized() {
        secondResourceId = transactionTemplate.execute(ignored -> {
            VaccinationCenter center = centerRepository.findById(centerId).orElseThrow();
            OperationalResource resource = resourceRepository.save(new OperationalResource(
                    center, resourceCode + "-2", ResourceType.ROOM,
                    "Read Test Room", OperationalResourceStatus.ACTIVE
            ));
            entityManager.flush();
            Long id = resource.getId();
            entityManager.clear();
            return id;
        });
        persistAssignment();
        persistAssignment(secondResourceId);

        List<SessionResourceAssignment> assignments = assignmentService.getAssignmentsForSession(sessionId);
        assertThat(assignments).hasSize(2);
        assertThat(assignments).allSatisfy(assignment -> {
            assertThat(Hibernate.isInitialized(assignment.getResource())).isTrue();
            assertThat(assignment.getResource().getCode()).isNotBlank();
        });
    }
}
