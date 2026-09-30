package com.se2030.vaccinationportal.appointment.api.vaccination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AppointmentValidationContractTests {

    @Test
    void snapshotCarriesOnlyTheResolvedIdentifierAndCheckedInState() {
        AppointmentValidationSnapshot snapshot = new AppointmentValidationSnapshot(42L, true);

        assertEquals(42L, snapshot.appointmentId());
        assertTrue(snapshot.checkedIn());
    }

    @Test
    void portUsesAnEmptyOptionalForAnUnresolvedAppointment() {
        AppointmentValidationPort port = appointmentId -> Optional.empty();

        assertFalse(port.findByAppointmentId(42L).isPresent());
    }
}
