package com.se2030.vaccinationportal.appointment.waitlist.web;

import com.se2030.vaccinationportal.appointment.web.AppointmentExceptionHandler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.waitlist.dto.WaitlistJoinRequest;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.appointment.testsupport.TestEligibilityLookupPortDouble;
import com.se2030.vaccinationportal.appointment.testsupport.TestSlotCapacityPortDouble;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistJoinService;
import com.se2030.vaccinationportal.appointment.waitlist.service.WaitlistLeaveService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc integration tests for {@link WaitlistController}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistControllerTests.TestConfig.class)
class WaitlistControllerTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEligibilityLookupPortDouble eligibilityLookupPort() {
            return new TestEligibilityLookupPortDouble();
        }

        @Bean
        public TestSlotCapacityPortDouble slotCapacityPort() {
            return new TestSlotCapacityPortDouble();
        }
    }

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WaitlistJoinService joinService;

    @Autowired
    private WaitlistLeaveService leaveService;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    private void authenticate(Long userId, String role) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                "user" + userId + "@portal.test",
                null,
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
        // Authoritative campaign/session relationship for R01 admission validation
        slotCapacityPortDouble.setSessionCampaign(3001L, 2001L);

        WaitlistController waitlistController = new WaitlistController(joinService, leaveService, waitlistRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(waitlistController)
                .setControllerAdvice(new AppointmentExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        slotCapacityPortDouble.clearSessionCampaignMap();
        SecurityContextHolder.clearContext();
    }

    @Test
    void joinWaitlist_success_returns201Created() throws Exception {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistJoinRequest request = new WaitlistJoinRequest(citizenId, campaignId, sessionId);

        mockMvc.perform(post("/api/v1/waitlist/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citizenId", is(citizenId.intValue())))
                .andExpect(jsonPath("$.status", is("WAITING")));
    }

    @Test
    void joinWaitlist_duplicateWaitlistEntry_returns409Conflict() throws Exception {
        Long citizenId = 1002L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        // First join
        joinService.joinWaitlist(citizenId, campaignId, sessionId, Instant.now());

        // Attempt second join for same campaign
        WaitlistJoinRequest request = new WaitlistJoinRequest(citizenId, campaignId, sessionId);

        mockMvc.perform(post("/api/v1/waitlist/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("already has an active WAITING waitlist entry")));
    }

    @Test
    void leaveWaitlist_success_returns200Ok() throws Exception {
        Long citizenId = 1003L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignId, sessionId, Instant.now());

        mockMvc.perform(post("/api/v1/waitlist/" + entry.getId() + "/leave")
                        .param("citizenId", citizenId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void leaveWaitlist_unauthorizedCitizen_returns403Forbidden() throws Exception {
        Long citizenId = 1004L;
        Long otherCitizenId = 9999L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;

        authenticate(otherCitizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignId, sessionId, Instant.now());

        mockMvc.perform(post("/api/v1/waitlist/" + entry.getId() + "/leave")
                        .param("citizenId", otherCitizenId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    void leaveWaitlist_notFound_returns404NotFound() throws Exception {
        authenticate(1001L, "ROLE_CITIZEN");
        mockMvc.perform(post("/api/v1/waitlist/999999/leave")
                        .param("citizenId", "1001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    void getWaitlistStatus_found_returns200OkWithPosition() throws Exception {
        Long citizenId = 1005L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        joinService.joinWaitlist(citizenId, campaignId, sessionId, Instant.now());

        mockMvc.perform(get("/api/v1/waitlist/status")
                        .param("citizenId", citizenId.toString())
                        .param("campaignId", campaignId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citizenId", is(citizenId.intValue())))
                .andExpect(jsonPath("$.status", is("WAITING")))
                .andExpect(jsonPath("$.position", is(1)));
    }

    @Test
    void getWaitlistStatus_notFound_returns404NotFound() throws Exception {
        authenticate(999999L, "ROLE_CITIZEN");
        mockMvc.perform(get("/api/v1/waitlist/status")
                        .param("citizenId", "999999")
                        .param("campaignId", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWaitlistStatus_promoted_returns200OkWithNullPosition() throws Exception {
        Long citizenId = 1006L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;

        authenticate(citizenId, "ROLE_CITIZEN");
        eligibilityPortDouble.setEligibility(citizenId, campaignId, EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSessionFull(sessionId, true);

        WaitlistEntry entry = joinService.joinWaitlist(citizenId, campaignId, sessionId, Instant.now());
        entry.markPromoted();
        waitlistRepository.save(entry);

        mockMvc.perform(get("/api/v1/waitlist/status")
                        .param("citizenId", citizenId.toString())
                        .param("campaignId", campaignId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citizenId", is(citizenId.intValue())))
                .andExpect(jsonPath("$.status", is("PROMOTED")))
                .andExpect(jsonPath("$.position").doesNotExist());
    }
}
