package com.se2030.vaccinationportal.vaccination.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.appointment.model.Appointment;
import com.se2030.vaccinationportal.appointment.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.repository.AppointmentRepository;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Mf03AppointmentValidationAdapterTests {

    private static final Long APPOINTMENT_ID = 101L;

    @Mock
    private AppointmentRepository appointmentRepository;

    private Mf03AppointmentValidationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new Mf03AppointmentValidationAdapter(appointmentRepository);
    }

    @Test
    void checkedInAppointmentMapsToCheckedInSnapshot() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CHECKED_IN);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        Optional<AppointmentValidationSnapshot> result = adapter.findByAppointmentId(APPOINTMENT_ID);

        assertTrue(result.isPresent());
        assertTrue(result.orElseThrow().checkedIn());
        verify(appointmentRepository).findById(APPOINTMENT_ID);
    }

    @Test
    void nonCheckedInAppointmentMapsToUncheckedSnapshot() {
        Appointment appointment = appointmentWithStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));

        Optional<AppointmentValidationSnapshot> result = adapter.findByAppointmentId(APPOINTMENT_ID);

        assertTrue(result.isPresent());
        assertFalse(result.orElseThrow().checkedIn());
    }

    @Test
    void unknownAppointmentMapsToEmptyOptional() {
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.empty());

        Optional<AppointmentValidationSnapshot> result = adapter.findByAppointmentId(APPOINTMENT_ID);

        assertTrue(result.isEmpty());
    }

    private Appointment appointmentWithStatus(AppointmentStatus status) {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(APPOINTMENT_ID);
        when(appointment.getStatus()).thenReturn(status);
        return appointment;
    }
}
