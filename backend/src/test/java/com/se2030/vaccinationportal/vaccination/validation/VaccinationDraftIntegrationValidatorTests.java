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

    @Test
    void validateBatchCompatibility_matchingVaccineTypePasses() {
        Long batchId = 301L;
        Long centerId = 10L;
        Long vaccineTypeId = 5L;

        com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort mockInventory =
                org.mockito.Mockito.mock(com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort.class);
        VaccinationDraftIntegrationValidator validatorWithInventory = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository,
                mockInventory
        );

        AppointmentValidationSnapshot snapshot = new AppointmentValidationSnapshot(
                APPOINTMENT_ID, true, 201L, 501L, 601L, centerId, vaccineTypeId
        );
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.of(snapshot));
        when(mockInventory.getBatchAvailability(centerId, batchId)).thenReturn(
                new com.se2030.vaccinationportal.inventory.dependency.BatchAvailability(
                        batchId, vaccineTypeId, com.se2030.vaccinationportal.inventory.model.BatchStatus.ACTIVE,
                        java.time.LocalDate.now().plusMonths(6), 10,
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.AVAILABLE
                )
        );

        assertDoesNotThrow(() -> validatorWithInventory.validateBatchCompatibility(APPOINTMENT_ID, batchId));
    }

    @Test
    void validateBatchCompatibility_mismatchedVaccineTypeThrowsException() {
        Long batchId = 301L;
        Long centerId = 10L;
        Long expectedVaccineTypeId = 5L;
        Long actualVaccineTypeId = 8L;

        com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort mockInventory =
                org.mockito.Mockito.mock(com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort.class);
        VaccinationDraftIntegrationValidator validatorWithInventory = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository,
                mockInventory
        );

        AppointmentValidationSnapshot snapshot = new AppointmentValidationSnapshot(
                APPOINTMENT_ID, true, 201L, 501L, 601L, centerId, expectedVaccineTypeId
        );
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.of(snapshot));
        when(mockInventory.getBatchAvailability(centerId, batchId)).thenReturn(
                new com.se2030.vaccinationportal.inventory.dependency.BatchAvailability(
                        batchId, actualVaccineTypeId, com.se2030.vaccinationportal.inventory.model.BatchStatus.ACTIVE,
                        java.time.LocalDate.now().plusMonths(6), 10,
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.AVAILABLE
                )
        );

        assertThrows(
                com.se2030.vaccinationportal.vaccination.exception.VaccinationBatchVaccineTypeMismatchException.class,
                () -> validatorWithInventory.validateBatchCompatibility(APPOINTMENT_ID, batchId)
        );
    }

    @Test
    void validateBatchCompatibility_nonexistentBatchThrowsBatchUnavailableException() {
        Long batchId = 999L;
        Long centerId = 10L;
        Long vaccineTypeId = 5L;

        com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort mockInventory =
                org.mockito.Mockito.mock(com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort.class);
        VaccinationDraftIntegrationValidator validatorWithInventory = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository,
                mockInventory
        );

        AppointmentValidationSnapshot snapshot = new AppointmentValidationSnapshot(
                APPOINTMENT_ID, true, 201L, 501L, 601L, centerId, vaccineTypeId
        );
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.of(snapshot));
        when(mockInventory.getBatchAvailability(centerId, batchId)).thenReturn(null);

        assertThrows(
                com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException.class,
                () -> validatorWithInventory.validateBatchCompatibility(APPOINTMENT_ID, batchId)
        );
    }

    @Test
    void validateBatchCompatibility_nullBatchVaccineTypeThrowsInvalidVaccinationDraftException() {
        Long batchId = 301L;
        Long centerId = 10L;
        Long vaccineTypeId = 5L;

        com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort mockInventory =
                org.mockito.Mockito.mock(com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort.class);
        VaccinationDraftIntegrationValidator validatorWithInventory = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository,
                mockInventory
        );

        AppointmentValidationSnapshot snapshot = new AppointmentValidationSnapshot(
                APPOINTMENT_ID, true, 201L, 501L, 601L, centerId, vaccineTypeId
        );
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.of(snapshot));
        when(mockInventory.getBatchAvailability(centerId, batchId)).thenReturn(
                new com.se2030.vaccinationportal.inventory.dependency.BatchAvailability(
                        batchId, null, com.se2030.vaccinationportal.inventory.model.BatchStatus.ACTIVE,
                        java.time.LocalDate.now().plusMonths(6), 10,
                        com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason.AVAILABLE
                )
        );

        assertThrows(
                com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException.class,
                () -> validatorWithInventory.validateBatchCompatibility(APPOINTMENT_ID, batchId)
        );
    }

    @Test
    void validateBatchCompatibility_missingExpectedCampaignTypeThrowsInvalidVaccinationDraftException() {
        Long batchId = 301L;
        Long centerId = 10L;

        VaccinationDraftIntegrationValidator validatorWithInventory = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository,
                org.mockito.Mockito.mock(com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort.class)
        );

        // Snapshot has null expected campaign vaccine type
        AppointmentValidationSnapshot snapshot = new AppointmentValidationSnapshot(
                APPOINTMENT_ID, true, 201L, 501L, 601L, centerId, null
        );
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.of(snapshot));

        assertThrows(
                com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationDraftException.class,
                () -> validatorWithInventory.validateBatchCompatibility(APPOINTMENT_ID, batchId)
        );
    }

    @Test
    void validateBatchCompatibility_missingAppointmentThrowsVaccinationAppointmentNotFoundException() {
        VaccinationDraftIntegrationValidator validatorWithInventory = new VaccinationDraftIntegrationValidator(
                appointmentValidationPort,
                vaccinationRecordRepository,
                org.mockito.Mockito.mock(com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort.class)
        );
        when(appointmentValidationPort.findByAppointmentId(APPOINTMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                VaccinationAppointmentNotFoundException.class,
                () -> validatorWithInventory.validateBatchCompatibility(APPOINTMENT_ID, 301L)
        );
    }
}
