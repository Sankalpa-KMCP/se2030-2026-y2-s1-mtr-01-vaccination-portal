package com.se2030.vaccinationportal.appointment.repository;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.model.CitizenCampaignBookingGuard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying persistence, query methods, conditional uniqueness index behavior,
 * and pessimistic write lock query execution for Appointment and CitizenCampaignBookingGuard repositories
 * (Canonical Section 12.4 & Business Rule MF03-BR-02).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentPersistenceIntegrationTests {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CitizenCampaignBookingGuardRepository guardRepository;

    @Test
    @DisplayName("Unique index on (citizen_id, active_campaign_id) rejects a second active CONFIRMED appointment for same citizen and campaign")
    void uniqueIndexRejectsSecondActiveConfirmedAppointment() {
        Appointment first = appointmentRepository.saveAndFlush(
                new Appointment(1001L, 2001L, 3001L, AppointmentStatus.CONFIRMED, Instant.now())
        );
        assertNotNull(first.getId());

        Appointment second = new Appointment(1001L, 2001L, 3002L, AppointmentStatus.CONFIRMED, Instant.now());

        assertThrows(DataIntegrityViolationException.class, () -> {
            appointmentRepository.saveAndFlush(second);
        }, "Unique constraint (citizen_id, active_campaign_id) must reject second active CONFIRMED appointment per MF03-BR-02");
    }

    @Test
    @DisplayName("Unique index on (citizen_id, active_campaign_id) rejects a CHECKED_IN appointment when a CONFIRMED appointment already exists")
    void uniqueIndexRejectsCheckedInWhenConfirmedExists() {
        Appointment first = appointmentRepository.saveAndFlush(
                new Appointment(1002L, 2002L, 3003L, AppointmentStatus.CONFIRMED, Instant.now())
        );
        assertNotNull(first.getId());

        Appointment second = new Appointment(1002L, 2002L, 3004L, AppointmentStatus.CHECKED_IN, Instant.now());

        assertThrows(DataIntegrityViolationException.class, () -> {
            appointmentRepository.saveAndFlush(second);
        }, "Unique constraint must reject active CHECKED_IN appointment when CONFIRMED appointment exists for same citizen and campaign");
    }

    @Test
    @DisplayName("Unique index allows CANCELLED appointment to coexist with a subsequent CONFIRMED appointment for same citizen and campaign")
    void uniqueIndexAllowsCancelledToCoexistWithLaterConfirmed() {
        Appointment cancelled = appointmentRepository.saveAndFlush(
                new Appointment(1003L, 2003L, 3005L, AppointmentStatus.CANCELLED, Instant.now())
        );
        assertNotNull(cancelled.getId());

        Appointment confirmed = appointmentRepository.saveAndFlush(
                new Appointment(1003L, 2003L, 3006L, AppointmentStatus.CONFIRMED, Instant.now())
        );
        assertNotNull(confirmed.getId());
    }

    @Test
    @DisplayName("findByCitizenIdAndCampaignIdAndStatusIn filters active appointments correctly")
    void findByCitizenIdAndCampaignIdAndStatusInFiltersCorrectly() {
        Appointment app1 = appointmentRepository.saveAndFlush(
                new Appointment(1004L, 2004L, 3007L, AppointmentStatus.CONFIRMED, Instant.now())
        );

        List<Appointment> activeList = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                1004L, 2004L, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
        );

        assertEquals(1, activeList.size());
        assertEquals(app1.getId(), activeList.get(0).getId());

        List<Appointment> cancelledList = appointmentRepository.findByCitizenIdAndCampaignIdAndStatusIn(
                1004L, 2004L, List.of(AppointmentStatus.CANCELLED)
        );
        assertTrue(cancelledList.isEmpty());
    }

    @Test
    @DisplayName("findBySlotIdAndStatusIn retrieves slot appointments filtered by status")
    void findBySlotIdAndStatusInFiltersCorrectly() {
        Appointment app1 = appointmentRepository.saveAndFlush(
                new Appointment(1005L, 2005L, 4001L, AppointmentStatus.CONFIRMED, Instant.now())
        );
        Appointment app2 = appointmentRepository.saveAndFlush(
                new Appointment(1006L, 2005L, 4001L, AppointmentStatus.CHECKED_IN, Instant.now())
        );

        List<Appointment> slotAppointments = appointmentRepository.findBySlotIdAndStatusIn(
                4001L, List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)
        );

        assertEquals(2, slotAppointments.size());
        assertTrue(slotAppointments.stream().anyMatch(a -> a.getId().equals(app1.getId())));
        assertTrue(slotAppointments.stream().anyMatch(a -> a.getId().equals(app2.getId())));
    }

    @Test
    @DisplayName("CitizenCampaignBookingGuard FOR UPDATE lock query executes without error against H2 database")
    void guardLockQueryExecutesWithoutErrorAgainstH2() {
        CitizenCampaignBookingGuard guard = guardRepository.saveAndFlush(new CitizenCampaignBookingGuard(1007L, 2007L));

        Optional<CitizenCampaignBookingGuard> lockedGuard = guardRepository.findByCitizenIdAndCampaignIdForUpdate(1007L, 2007L);

        assertTrue(lockedGuard.isPresent());
        assertEquals(guard.getId(), lockedGuard.get().getId());
        assertEquals(1007L, lockedGuard.get().getCitizenId());
        assertEquals(2007L, lockedGuard.get().getCampaignId());
        /*
         * Note on Test Environment Gap:
         * H2 in-memory mode executes SELECT ... FOR UPDATE query syntax cleanly without error within
         * a single transaction. However, true multi-threaded pessimistic write lock contention, row-level
         * blocking, and timeout behaviors under heavy concurrent load must be validated against production MySQL
         * / Testcontainers MySQL instance as mandated by Canonical Section 12.4 & Section 17.4.
         */
    }

    @Test
    @DisplayName("insertGuardIfAbsent inserts new guard row when missing and returns existing when present")
    void insertGuardIfAbsentBehavior() {
        CitizenCampaignBookingGuard inserted = guardRepository.insertGuardIfAbsent(1008L, 2008L);
        assertNotNull(inserted.getId());
        assertEquals(1008L, inserted.getCitizenId());

        CitizenCampaignBookingGuard existing = guardRepository.insertGuardIfAbsent(1008L, 2008L);
        assertEquals(inserted.getId(), existing.getId());
    }
}
