package com.se2030.vaccinationportal.appointment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security integration tests for {@code /api/v1/appointments} and {@code /api/v1/waitlist} endpoints (MF-03).
 * Tests method security, staff override authorization, principal identity binding, and IDOR prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AppointmentSecurityAuthorizationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    private String token(Long userId, String email, List<String> roles) {
        return "Bearer " + jwtTokenService.generateToken(userId, email, roles);
    }

    @Nested
    @DisplayName("Operational Endpoints Allowed Roles (ADMIN, CENTER_MANAGER)")
    class OperationalAllowedRoles {

        @Test
        @DisplayName("ADMIN role is allowed to access GET /api/v1/appointments/queue")
        void adminQueueAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("CENTER_MANAGER role is allowed to access GET /api/v1/appointments/queue")
        void centerManagerQueueAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(3L, "cm@test.gov", List.of("ROLE_CENTER_MANAGER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Operational Endpoints Denied Roles (CITIZEN, CAMPAIGN_OFFICER, INVENTORY_OFFICER, HEALTHCARE_OFFICER)")
    class OperationalDeniedRoles {

        @Test
        @DisplayName("HEALTHCARE_OFFICER role is denied with 403 Forbidden on GET /api/v1/appointments/queue")
        void healthcareOfficerQueueDenied() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("HEALTHCARE_OFFICER role is denied with 403 Forbidden on check-in")
        void healthcareOfficerCheckInDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/check-in")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CITIZEN role is denied with 403 Forbidden on GET /api/v1/appointments/queue")
        void citizenQueueDenied() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(4L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CAMPAIGN_OFFICER role is denied with 403 Forbidden on check-in")
        void campaignOfficerCheckInDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/check-in")
                            .header(HttpHeaders.AUTHORIZATION, token(5L, "co@test.gov", List.of("ROLE_CAMPAIGN_OFFICER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INVENTORY_OFFICER role is denied with 403 Forbidden on reverse-check-in")
        void inventoryOfficerReverseCheckInDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/reverse-check-in")
                            .header(HttpHeaders.AUTHORIZATION, token(6L, "inv@test.gov", List.of("ROLE_INVENTORY_OFFICER"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous request to GET /api/v1/appointments/queue is rejected with 401/403")
        void anonymousQueueRejected() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/queue")
                            .param("slotId", "1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Anonymous request to POST /api/v1/appointments/1/check-in is rejected with 401/403")
        void anonymousCheckInRejected() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/check-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Role-Based Staff Override Cancellation Access (AC-3)")
    class StaffOverrideCancelAllowed {

        @Test
        @DisplayName("CITIZEN role claiming isStaffOverride=true on cancel is denied with 403")
        void citizenStaffOverrideCancelDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(4L, "cit@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CAMPAIGN_OFFICER role claiming isStaffOverride=true on cancel is denied with 403")
        void campaignOfficerStaffOverrideCancelDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(8L, "co@test.gov", List.of("ROLE_CAMPAIGN_OFFICER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN role claiming isStaffOverride=true on cancel passes security layer")
        void adminStaffOverrideCancelAllowedThroughSecurity() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/999999/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
        }

        @Test
        @DisplayName("HEALTHCARE_OFFICER role claiming isStaffOverride=true on cancel is denied with 403")
        void hcoStaffOverrideCancelDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/999999/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(2L, "hco@test.gov", List.of("ROLE_HEALTHCARE_OFFICER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CENTER_MANAGER role claiming isStaffOverride=true on cancel passes security layer")
        void cmStaffOverrideCancelAllowedThroughSecurity() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/999999/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(3L, "cm@test.gov", List.of("ROLE_CENTER_MANAGER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":true,\"staffReason\":\"emergency\"}"))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
        }

        @Test
        @DisplayName("Staff member calling cancel with isStaffOverride=false is denied with 403 (cannot bypass audit)")
        void staffCallingCancelAsCitizenDenied() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(1L, "admin@test.gov", List.of("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isStaffOverride\":false}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Anonymous Access Blocked for Citizen Endpoints (AC-1, AC-2)")
    class AnonymousAccessBlocked {

        @Test
        @DisplayName("Anonymous GET /api/v1/appointments/mine is rejected with 401 Unauthorized")
        void anonymousMineRejected() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/mine").param("citizenId", "100"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/appointments/search is rejected with 401 Unauthorized")
        void anonymousSearchRejected() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/search")
                            .param("citizenId", "100")
                            .param("campaignId", "1")
                            .param("fromDate", LocalDate.now().toString())
                            .param("toDate", LocalDate.now().plusDays(7).toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/appointments/book is rejected with 401 Unauthorized")
        void anonymousBookRejected() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"citizenId\":100,\"campaignId\":1,\"slotId\":1}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/appointments/1/cancel is rejected with 401 Unauthorized")
        void anonymousCancelRejected() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestingCitizenId\":100}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/appointments/1/reschedule is rejected with 401 Unauthorized")
        void anonymousRescheduleRejected() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/1/reschedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestingCitizenId\":100,\"newSlotId\":2}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/waitlist/join is rejected with 401 Unauthorized")
        void anonymousWaitlistJoinRejected() throws Exception {
            mockMvc.perform(post("/api/v1/waitlist/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"citizenId\":100,\"campaignId\":1,\"sessionId\":1}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/waitlist/1/leave is rejected with 401 Unauthorized")
        void anonymousWaitlistLeaveRejected() throws Exception {
            mockMvc.perform(post("/api/v1/waitlist/1/leave").param("citizenId", "100"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous GET /api/v1/waitlist/status is rejected with 401 Unauthorized")
        void anonymousWaitlistStatusRejected() throws Exception {
            mockMvc.perform(get("/api/v1/waitlist/status")
                            .param("citizenId", "100")
                            .param("campaignId", "1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Citizen Identity Binding and IDOR Prevention (AC-3 through AC-10)")
    class CitizenIdentityBindingAndIDORPrevention {

        private static final Long CITIZEN_A = 101L;
        private static final Long CITIZEN_B = 102L;

        @Test
        @DisplayName("Citizen A querying /mine for Citizen A returns 200 OK")
        void citizenAQueryingOwnMineReturns200() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/mine")
                            .param("citizenId", CITIZEN_A.toString())
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Citizen A querying /mine with Citizen B ID is rejected with 403 Forbidden")
        void citizenAQueryingCitizenBMineRejectedWith403() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/mine")
                            .param("citizenId", CITIZEN_B.toString())
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A querying /mine without citizenId param succeeds using principal identity")
        void citizenAQueryingMineWithoutParamSucceeds() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/mine")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Citizen A searching with own ID returns 200 OK")
        void citizenASearchingOwnReturns200() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/search")
                            .param("citizenId", CITIZEN_A.toString())
                            .param("campaignId", "1")
                            .param("fromDate", LocalDate.now().toString())
                            .param("toDate", LocalDate.now().plusDays(7).toString())
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Citizen A searching with Citizen B ID is rejected with 403 Forbidden")
        void citizenASearchingWithCitizenBIDRejectedWith403() throws Exception {
            mockMvc.perform(get("/api/v1/appointments/search")
                            .param("citizenId", CITIZEN_B.toString())
                            .param("campaignId", "1")
                            .param("fromDate", LocalDate.now().toString())
                            .param("toDate", LocalDate.now().plusDays(7).toString())
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A booking with Citizen B ID in body is rejected with 403 Forbidden")
        void citizenABookingForCitizenBRejectedWith403() throws Exception {
            mockMvc.perform(post("/api/v1/appointments/book")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"citizenId\":" + CITIZEN_B + ",\"campaignId\":1,\"slotId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A cancelling Citizen B's appointment is rejected with 403 Forbidden")
        void citizenACancellingCitizenBAppointmentRejectedWith403() throws Exception {
            Appointment apptB = appointmentRepository.save(new Appointment(CITIZEN_B, 1L, 1L));

            mockMvc.perform(post("/api/v1/appointments/" + apptB.getId() + "/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestingCitizenId\":" + CITIZEN_A + "}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A cancelling appointment with forged requestingCitizenId is rejected with 403 Forbidden")
        void citizenACancellingWithForgedCitizenIDRejectedWith403() throws Exception {
            Appointment apptA = appointmentRepository.save(new Appointment(CITIZEN_A, 1L, 1L));

            mockMvc.perform(post("/api/v1/appointments/" + apptA.getId() + "/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestingCitizenId\":" + CITIZEN_B + "}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A with null requestingCitizenId cannot cancel Citizen B's appointment (403 Forbidden)")
        void citizenAWithNullRequestingCitizenIdCannotCancelCitizenBAppointment() throws Exception {
            Appointment apptB = appointmentRepository.save(new Appointment(CITIZEN_B, 1L, 1L));

            mockMvc.perform(post("/api/v1/appointments/" + apptB.getId() + "/cancel")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A rescheduling Citizen B's appointment is rejected with 403 Forbidden")
        void citizenAReschedulingCitizenBAppointmentRejectedWith403() throws Exception {
            Appointment apptB = appointmentRepository.save(new Appointment(CITIZEN_B, 1L, 1L));

            mockMvc.perform(post("/api/v1/appointments/" + apptB.getId() + "/reschedule")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestingCitizenId\":" + CITIZEN_A + ",\"newSlotId\":2}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A joining waitlist with Citizen B ID is rejected with 403 Forbidden")
        void citizenAJoiningWaitlistForCitizenBRejectedWith403() throws Exception {
            mockMvc.perform(post("/api/v1/waitlist/join")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"citizenId\":" + CITIZEN_B + ",\"campaignId\":1,\"sessionId\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A leaving Citizen B's waitlist entry is rejected with 403 Forbidden")
        void citizenALeavingCitizenBWaitlistEntryRejectedWith403() throws Exception {
            WaitlistEntry entryB = waitlistRepository.save(
                    new WaitlistEntry(CITIZEN_B, 1L, 1L, WaitlistStatus.WAITING, Instant.now())
            );

            mockMvc.perform(post("/api/v1/waitlist/" + entryB.getId() + "/leave")
                            .param("citizenId", CITIZEN_A.toString())
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A supplying Citizen B ID to leave own waitlist entry is rejected with 403 Forbidden")
        void citizenASupplyingCitizenBIDToLeaveRejectedWith403() throws Exception {
            WaitlistEntry entryA = waitlistRepository.save(
                    new WaitlistEntry(CITIZEN_A, 1L, 1L, WaitlistStatus.WAITING, Instant.now())
            );

            mockMvc.perform(post("/api/v1/waitlist/" + entryA.getId() + "/leave")
                            .param("citizenId", CITIZEN_B.toString())
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A inspecting Citizen B's waitlist status is rejected with 403 Forbidden")
        void citizenAInspectingCitizenBWaitlistStatusRejectedWith403() throws Exception {
            mockMvc.perform(get("/api/v1/waitlist/status")
                            .param("citizenId", CITIZEN_B.toString())
                            .param("campaignId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Citizen A inspecting own waitlist status passes security (returns 200 or 404, not 401/403)")
        void citizenAInspectingOwnWaitlistStatusPassesSecurity() throws Exception {
            mockMvc.perform(get("/api/v1/waitlist/status")
                            .param("citizenId", CITIZEN_A.toString())
                            .param("campaignId", "1")
                            .header(HttpHeaders.AUTHORIZATION, token(CITIZEN_A, "citA@test.gov", List.of("ROLE_CITIZEN"))))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(401, result.getResponse().getStatus()))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
        }
    }
}
