package com.se2030.vaccinationportal.campaign.core;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.campaign.center.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Focused integration tests for MF02-R-002 Campaign Update capability (PUT /api/v1/campaigns/{id}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CampaignUpdateIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignLifecycleEventRepository lifecycleEventRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private Clock clock;
    @Autowired private EntityManager entityManager;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center;
    private String adminAuth;

    private static final LocalDate INITIAL_START = LocalDate.of(2026, 10, 1);
    private static final LocalDate INITIAL_END = LocalDate.of(2026, 12, 31);
    private static final Instant INITIAL_BOOK_OPEN = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant INITIAL_BOOK_CLOSE = Instant.parse("2026-09-30T23:59:59Z");

    private String updatePayload(String name, String description, LocalDate start, LocalDate end, Instant bookOpen, Instant bookClose) {
        return String.format("""
                {
                    "name": "%s",
                    "description": %s,
                    "serviceStartDate": "%s",
                    "serviceEndDate": "%s",
                    "bookingOpenAt": "%s",
                    "bookingCloseAt": "%s"
                }
                """,
                name,
                description != null ? "\"" + description + "\"" : "null",
                start,
                end,
                bookOpen,
                bookClose
        );
    }

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-UPD-01", "Update Test Vaccine", "dose", true));
        targetGroup = targetGroupRepository.saveAndFlush(new TargetGroup("TG-UPD-01", "Update Target Group", true));
        center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-UPD-01", "Update Center", "100 Healthcare Way", "555-0100",
                CenterStatus.ACTIVE, 100, 200, "UTC"
        ));
        adminAuth = "Bearer " + jwtTokenService.generateToken(1L, "admin@portal.gov", List.of("ROLE_ADMIN"));
    }

    private VaccinationCampaign createTestCampaign(String code, CampaignStatus status) {
        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                code,
                "Original Name",
                "Original Description",
                vaccineType,
                targetGroup,
                INITIAL_START,
                INITIAL_END,
                INITIAL_BOOK_OPEN,
                INITIAL_BOOK_CLOSE,
                status
        );
        return campaignRepository.saveAndFlush(campaign);
    }

    @Test
    @DisplayName("A-E & N: Successful update mutates approved fields while preserving immutable fields and adding no lifecycle event")
    void successfulUpdateMutatesApprovedFieldsAndPreservesImmutables() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-SUCCESS", CampaignStatus.DRAFT);
        Long campaignId = campaign.getId();

        LocalDate newStart = LocalDate.of(2026, 10, 15);
        LocalDate newEnd = LocalDate.of(2027, 1, 15);
        Instant newBookOpen = Instant.parse("2026-09-10T08:00:00Z");
        Instant newBookClose = Instant.parse("2026-10-10T18:00:00Z");

        String payload = updatePayload(
                "Updated Campaign Name",
                "Updated Campaign Description",
                newStart,
                newEnd,
                newBookOpen,
                newBookClose
        );

        mockMvc.perform(put("/api/v1/campaigns/" + campaignId)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(campaignId.intValue())))
                .andExpect(jsonPath("$.code", is("CMP-UPD-SUCCESS")))
                .andExpect(jsonPath("$.name", is("Updated Campaign Name")))
                .andExpect(jsonPath("$.description", is("Updated Campaign Description")))
                .andExpect(jsonPath("$.serviceStartDate", is("2026-10-15")))
                .andExpect(jsonPath("$.serviceEndDate", is("2027-01-15")))
                .andExpect(jsonPath("$.bookingOpenAt", is("2026-09-10T08:00:00Z")))
                .andExpect(jsonPath("$.bookingCloseAt", is("2026-10-10T18:00:00Z")))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        // Verify persistence-level assertions
        entityManager.flush();
        entityManager.clear();

        VaccinationCampaign persisted = campaignRepository.findById(campaignId).orElseThrow();

        // A: Approved fields changed
        assertEquals("Updated Campaign Name", persisted.getName());
        assertEquals("Updated Campaign Description", persisted.getDescription());
        assertEquals(newStart, persisted.getServiceStartDate());
        assertEquals(newEnd, persisted.getServiceEndDate());
        assertEquals(newBookOpen, persisted.getBookingOpenAt());
        assertEquals(newBookClose, persisted.getBookingCloseAt());

        // B: Code unchanged
        assertEquals("CMP-UPD-SUCCESS", persisted.getCode());

        // C: VaccineType unchanged
        assertEquals(vaccineType.getId(), persisted.getVaccineType().getId());
        assertEquals("VAX-UPD-01", persisted.getVaccineType().getCode());

        // D: TargetGroup unchanged
        assertEquals(targetGroup.getId(), persisted.getTargetGroup().getId());
        assertEquals("TG-UPD-01", persisted.getTargetGroup().getCode());

        // E: Status unchanged
        assertEquals(CampaignStatus.DRAFT, persisted.getStatus());

        // N: No CampaignLifecycleEvent appended
        List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaignId);
        assertTrue(events.isEmpty(), "Ordinary update must not record a CampaignLifecycleEvent");
    }

    @ParameterizedTest
    @EnumSource(value = CampaignStatus.class, names = {"DRAFT", "SCHEDULED", "PUBLISHED", "ACTIVE", "SUSPENDED"})
    @DisplayName("F: Valid updates are permitted across all non-closed states")
    void validUpdatesPermittedInNonClosedStates(CampaignStatus status) throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-STATE-" + status.name(), status);

        String payload = updatePayload(
                "Name for " + status.name(),
                "Description for " + status.name(),
                INITIAL_START.plusDays(1),
                INITIAL_END.plusDays(1),
                INITIAL_BOOK_OPEN.plusSeconds(3600),
                INITIAL_BOOK_CLOSE.plusSeconds(3600)
        );

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Name for " + status.name())))
                .andExpect(jsonPath("$.status", is(status.name())));

        entityManager.flush();
        entityManager.clear();
        VaccinationCampaign persisted = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(status, persisted.getStatus());
        assertEquals("Name for " + status.name(), persisted.getName());
    }

    @Test
    @DisplayName("G: COMPLETED status rejects update with 409 Conflict")
    void completedStatusRejectsUpdate() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-COMPLETED", CampaignStatus.COMPLETED);

        String payload = updatePayload("Attempted Update", "Desc", INITIAL_START, INITIAL_END, INITIAL_BOOK_OPEN, INITIAL_BOOK_CLOSE);

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot update campaign in COMPLETED status")));
    }

    @Test
    @DisplayName("H: CANCELLED status rejects update with 409 Conflict")
    void cancelledStatusRejectsUpdate() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-CANCELLED", CampaignStatus.CANCELLED);

        String payload = updatePayload("Attempted Update", "Desc", INITIAL_START, INITIAL_END, INITIAL_BOOK_OPEN, INITIAL_BOOK_CLOSE);

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot update campaign in CANCELLED status")));
    }

    @Test
    @DisplayName("I: ARCHIVED status rejects update with 409 Conflict")
    void archivedStatusRejectsUpdate() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-ARCHIVED", CampaignStatus.ARCHIVED);

        String payload = updatePayload("Attempted Update", "Desc", INITIAL_START, INITIAL_END, INITIAL_BOOK_OPEN, INITIAL_BOOK_CLOSE);

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot update campaign in ARCHIVED status")));
    }

    @Test
    @DisplayName("J: Missing campaign returns 404 Not Found")
    void missingCampaignReturns404() throws Exception {
        String payload = updatePayload("Non-Existent", "Desc", INITIAL_START, INITIAL_END, INITIAL_BOOK_OPEN, INITIAL_BOOK_CLOSE);

        mockMvc.perform(put("/api/v1/campaigns/999999")
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Campaign not found with ID: 999999")));
    }

    @Test
    @DisplayName("K: Invalid service-date ordering (serviceEndDate <= serviceStartDate) returns 400 Bad Request")
    void invalidServiceDateOrderingReturns400() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-INVALID-SD", CampaignStatus.DRAFT);

        // serviceEndDate before serviceStartDate
        String payload = updatePayload(
                "Invalid Dates", "Desc",
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 11, 1),
                INITIAL_BOOK_OPEN, INITIAL_BOOK_CLOSE
        );

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("serviceEndDate")))
                .andExpect(jsonPath("$.fieldErrors[0].message", is("Service end date must be after service start date")));
    }

    @Test
    @DisplayName("L: Invalid booking-window ordering (bookingCloseAt <= bookingOpenAt) returns 400 Bad Request")
    void invalidBookingWindowOrderingReturns400() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-INVALID-BW", CampaignStatus.DRAFT);

        // bookingCloseAt before bookingOpenAt
        String payload = updatePayload(
                "Invalid Window", "Desc",
                INITIAL_START, INITIAL_END,
                Instant.parse("2026-09-30T12:00:00Z"),
                Instant.parse("2026-09-01T12:00:00Z")
        );

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("bookingCloseAt")))
                .andExpect(jsonPath("$.fieldErrors[0].message", is("Booking close time must be after booking open time")));
    }

    @Test
    @DisplayName("L2: Booking close after service end date returns 400 Bad Request")
    void bookingCloseAfterServiceEndDateReturns400() throws Exception {
        VaccinationCampaign campaign = createTestCampaign("CMP-UPD-INVALID-BC-SE", CampaignStatus.DRAFT);

        String payload = updatePayload(
                "Invalid Close Date", "Desc",
                INITIAL_START, INITIAL_END,
                INITIAL_BOOK_OPEN,
                Instant.parse("2027-01-05T12:00:00Z")
        );

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("bookingCloseAt")))
                .andExpect(jsonPath("$.fieldErrors[0].message", is("Booking close date must not be after campaign service end date")));
    }

    @Test
    @DisplayName("M: Shortening service dates so an existing blocking session falls outside proposed range returns 400 Bad Request")
    void shorteningServiceDatesWithBlockingSessionReturns400() throws Exception {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.plusDays(60);

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-UPD-REV-IMP",
                "Reverse Impact Campaign",
                "Desc",
                vaccineType,
                targetGroup,
                start,
                end,
                today.minusDays(20).atStartOfDay().toInstant(ZoneOffset.UTC),
                today.minusDays(15).atStartOfDay().toInstant(ZoneOffset.UTC),
                CampaignStatus.ACTIVE
        );
        campaign = campaignRepository.saveAndFlush(campaign);

        assignmentRepository.saveAndFlush(new CampaignCenterAssignment(campaign, center, start, end));

        // Create an active session on today + 5 days
        LocalDate sessionDate = today.plusDays(5);
        Instant sessionStart = sessionDate.atTime(9, 0).toInstant(ZoneOffset.UTC);
        Instant sessionEnd = sessionDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        VaccinationSession session = new VaccinationSession(
                campaign, center, sessionStart, sessionEnd, sessionStart.minusSeconds(3600), "UTC"
        );
        session.transitionLifecycleStatus(SessionLifecycleStatus.SCHEDULED);
        sessionRepository.saveAndFlush(session);

        // Propose shortening start date to today + 10 (past session on today + 5)
        String payload = updatePayload(
                "Shortened Campaign",
                "Desc",
                today.plusDays(10),
                end,
                campaign.getBookingOpenAt(),
                campaign.getBookingCloseAt()
        );

        mockMvc.perform(put("/api/v1/campaigns/" + campaign.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(
                        "Cannot shorten campaign service dates because active or scheduled session(s) fall outside proposed service dates [" +
                                today.plusDays(10) + " to " + end + "]"
                )));
    }
}
