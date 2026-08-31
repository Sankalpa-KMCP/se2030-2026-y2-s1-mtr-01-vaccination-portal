package com.se2030.vaccinationportal.vaccination.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.exception.AppointmentNotCheckedInException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationAppointmentNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordAlreadyExistsException;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationDraftIntegrationValidatorTests {

    private static final Long APPOINTMENT_ID = 101L;

    @Mock
    private AppointmentValidationPort appointmentValidationPort;

    @Mock
    private VaccinationRecordRepository vaccinationRecordRepository;

    private VaccinationDraftIntegrationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository
        );
    }

    @Test
    void missingAppointmentIsRejected() {
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                VaccinationAppointmentNotFoundException.class,
                () -> validator.validateAppointment(APPOINTMENT_ID)
        );

        verifyNoInteractions(vaccinationRecordRepository);
    }

    @Test
    void appointmentThatIsNotCheckedInIsRejected() {
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(new AppointmentValidationSnapshot(APPOINTMENT_ID, false)));

        assertThrows(
                AppointmentNotCheckedInException.class,
                () -> validator.validateAppointment(APPOINTMENT_ID)
        );

        verify(vaccinationRecordRepository, never()).existsByAppointmentId(APPOINTMENT_ID);
    }

    @Test
    void appointmentWithFinalizedRecordIsRejected() {
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(new AppointmentValidationSnapshot(APPOINTMENT_ID, true)));
        when(vaccinationRecordRepository.existsByAppointmentId(APPOINTMENT_ID)).thenReturn(true);

        assertThrows(
                VaccinationRecordAlreadyExistsException.class,
                () -> validator.validateAppointment(APPOINTMENT_ID)
        );
    }

    @Test
    void checkedInAppointmentWithoutFinalizedRecordPasses() {
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(new AppointmentValidationSnapshot(APPOINTMENT_ID, true)));
        when(vaccinationRecordRepository.existsByAppointmentId(APPOINTMENT_ID)).thenReturn(false);

        assertDoesNotThrow(() -> validator.validateAppointment(APPOINTMENT_ID));

        verify(vaccinationRecordRepository).existsByAppointmentId(APPOINTMENT_ID);
    }
}
