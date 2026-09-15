package com.se2030.vaccinationportal.appointment.service;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.appointment.exception.AppointmentNotFoundException;
import com.se2030.vaccinationportal.appointment.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.appointment.repository.WaitlistRepository;
import java.time.Instant;
import java.util.Optional;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(WaitlistLeaveServiceTests.TestConfig.class)
class WaitlistLeaveServiceTests {

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

    @Autowired
    private WaitlistLeaveService leaveService;

    @Autowired
    private WaitlistJoinService joinService;

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

    private static final Long CITIZEN_A = 2001L;
    private static final Long CITIZEN_B = 2002L;
    private static final Long CAMPAIGN_ID = 3001L;
    private static final Long SESSION_ID = 4001L;

    @BeforeEach
    void setUp() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();

        eligibilityPortDouble.setDefaultStatus(EligibilityStatus.ELIGIBLE);
        slotCapacityPortDouble.setSimulateReleaseFailure(false);
    }

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @Test
    void leaveWaitlist_success_transitionsStatusToCancelled() {
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_ID, SESSION_ID, Instant.now());
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());

        WaitlistEntry cancelled = leaveService.leaveWaitlist(entry.getId(), CITIZEN_A);
        assertEquals(WaitlistStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void leaveWaitlist_rejection_whenBelongsToAnotherCitizen() {
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_ID, SESSION_ID, Instant.now());

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveService.leaveWaitlist(entry.getId(), CITIZEN_B)
        );
    }

    @Test
    void leaveWaitlist_rejection_whenAlreadyPromoted() {
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_ID, SESSION_ID, Instant.now());
        entry.markPromoted();
        waitlistRepository.save(entry);

        assertThrows(
                IllegalStateException.class,
                () -> leaveService.leaveWaitlist(entry.getId(), CITIZEN_A)
        );
    }

    @Test
    void leaveWaitlist_rejection_whenNotFound() {
        assertThrows(
                AppointmentNotFoundException.class,
                () -> leaveService.leaveWaitlist(999999L, CITIZEN_A)
        );
    }

    @Test
    void getWaitlistStatus_returnsCorrectEntryAndPosition() {
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        Instant now = Instant.now();
        WaitlistEntry entryA = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_ID, SESSION_ID, now);
        WaitlistEntry entryB = joinService.joinWaitlist(CITIZEN_B, CAMPAIGN_ID + 1, SESSION_ID, now.plusSeconds(10));

        Optional<WaitlistEntry> foundA = leaveService.getWaitlistStatus(CITIZEN_A, CAMPAIGN_ID);
        assertTrue(foundA.isPresent());
        assertEquals(entryA.getId(), foundA.get().getId());
        assertEquals(1, leaveService.getQueuePosition(foundA.get()));

        Optional<WaitlistEntry> foundB = leaveService.getWaitlistStatus(CITIZEN_B, CAMPAIGN_ID + 1);
        assertTrue(foundB.isPresent());
        assertEquals(entryB.getId(), foundB.get().getId());
        assertEquals(2, leaveService.getQueuePosition(foundB.get()));
    }

    @Test
    void getWaitlistStatus_returnsEmpty_whenNoWaitlistEntry() {
        Optional<WaitlistEntry> result = leaveService.getWaitlistStatus(999999L, CAMPAIGN_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWaitlistStatus_returnsPromotedEntry_whenCitizenPromoted() {
        slotCapacityPortDouble.setSessionFull(SESSION_ID, true);

        WaitlistEntry entry = joinService.joinWaitlist(CITIZEN_A, CAMPAIGN_ID, SESSION_ID, Instant.now());
        entry.markPromoted();
        waitlistRepository.save(entry);

        Optional<WaitlistEntry> found = leaveService.getWaitlistStatus(CITIZEN_A, CAMPAIGN_ID);
        assertTrue(found.isPresent());
        assertEquals(WaitlistStatus.PROMOTED, found.get().getStatus());
        assertEquals(entry.getId(), found.get().getId());
        assertNull(leaveService.getQueuePosition(found.get()));
    }
}
