package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.campaign.api.availability.AvailableSessionSummary;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(AppointmentSearchServiceTests.TestConfig.class)
class AppointmentSearchServiceTests {

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

        @Bean
        @org.springframework.context.annotation.Primary
        public TestSlotAvailabilityPortDouble slotAvailabilityPort() {
            return new TestSlotAvailabilityPortDouble();
        }
    }

    @Autowired
    private AppointmentSearchService searchService;

    @Autowired
    private AppointmentBookingService bookingService;

    @Autowired
    private WaitlistJoinService joinService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Autowired
    private TestEligibilityLookupPortDouble eligibilityPortDouble;

    @Autowired
    private TestSlotCapacityPortDouble slotCapacityPortDouble;

    @Autowired
    private TestSlotAvailabilityPortDouble availabilityPortDouble;

    private static final Long CITIZEN_ID = 3001L;
    private static final Long CAMPAIGN_ID = 4001L;
    private static final Long CENTER_ID = 5001L;
    private static final Long SESSION_1_ID = 6001L;
    private static final Long SESSION_2_ID = 6002L;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        availabilityPortDouble.clear();

        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
        availabilityPortDouble.clear();
    }

    @Test
    void searchAvailableSessions_returnsAvailableSessions() {
        Instant now = Instant.now();
        AvailableSessionSummary s1 = new AvailableSessionSummary(
                SESSION_1_ID, 7001L, CAMPAIGN_ID, CENTER_ID, "Center A", "123 Main St",
                now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(1800), 5
        );
        AvailableSessionSummary s2 = new AvailableSessionSummary(
                SESSION_2_ID, 7002L, CAMPAIGN_ID, CENTER_ID, "Center A", "123 Main St",
                now.plusSeconds(14400), now.plusSeconds(18000), now.plusSeconds(10000), 3
        );
        availabilityPortDouble.addSession(s1);
        availabilityPortDouble.addSession(s2);

        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(7);

        List<AvailableSessionSummary> results = searchService.searchAvailableSessions(
                CITIZEN_ID, CAMPAIGN_ID, CENTER_ID, fromDate, toDate
        );

        assertEquals(2, results.size());
        assertEquals(SESSION_1_ID, results.get(0).sessionId());
        assertEquals(SESSION_2_ID, results.get(1).sessionId());
    }

    @Test
    void searchAvailableSessions_excludesSession_whenCitizenHasActiveAppointment() {
        Instant now = Instant.now();
        AvailableSessionSummary s1 = new AvailableSessionSummary(
                SESSION_1_ID, 7001L, CAMPAIGN_ID, CENTER_ID, "Center A", "123 Main St",
                now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(1800), 5
        );
        AvailableSessionSummary s2 = new AvailableSessionSummary(
                SESSION_2_ID, 7002L, CAMPAIGN_ID, CENTER_ID, "Center A", "123 Main St",
                now.plusSeconds(14400), now.plusSeconds(18000), now.plusSeconds(10000), 3
        );
        availabilityPortDouble.addSession(s1);
        availabilityPortDouble.addSession(s2);

        slotCapacityPortDouble.setAvailableCapacity(7001L, 5);
        Appointment appt = bookingService.bookAppointment(CITIZEN_ID, CAMPAIGN_ID, 7001L);

        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(7);

        List<AvailableSessionSummary> results = searchService.searchAvailableSessions(
                CITIZEN_ID, CAMPAIGN_ID, CENTER_ID, fromDate, toDate
        );

        assertEquals(1, results.size());
        assertEquals(SESSION_2_ID, results.get(0).sessionId());
    }

    @Test
    void searchAvailableSessions_excludesSession_whenCitizenHasWaitingWaitlistEntry() {
        Instant now = Instant.now();
        AvailableSessionSummary s1 = new AvailableSessionSummary(
                SESSION_1_ID, 7001L, CAMPAIGN_ID, CENTER_ID, "Center A", "123 Main St",
                now.plusSeconds(3600), now.plusSeconds(7200), now.plusSeconds(1800), 5
        );
        AvailableSessionSummary s2 = new AvailableSessionSummary(
                SESSION_2_ID, 7002L, CAMPAIGN_ID, CENTER_ID, "Center A", "123 Main St",
                now.plusSeconds(14400), now.plusSeconds(18000), now.plusSeconds(10000), 3
        );
        availabilityPortDouble.addSession(s1);
        availabilityPortDouble.addSession(s2);

        slotCapacityPortDouble.setSessionFull(SESSION_1_ID, true);
        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_ID, CAMPAIGN_ID, SESSION_1_ID, now);

        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(7);

        List<AvailableSessionSummary> results = searchService.searchAvailableSessions(
                CITIZEN_ID, CAMPAIGN_ID, CENTER_ID, fromDate, toDate
        );

        assertEquals(1, results.size());
        assertEquals(SESSION_2_ID, results.get(0).sessionId());
    }
}
