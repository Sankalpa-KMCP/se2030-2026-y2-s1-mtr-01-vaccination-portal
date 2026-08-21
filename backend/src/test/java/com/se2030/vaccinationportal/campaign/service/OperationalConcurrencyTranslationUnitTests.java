package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalResourceRepository;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OperationalConcurrencyTranslationUnitTests {

    @Mock private OperationalResourceRepository resourceRepository;
    @Mock private OperationalStaffRosterRepository rosterRepository;
    @Mock private VaccinationCenterRepository centerRepository;

    private OperationalResourceService resourceService;
    private OperationalStaffRosterService rosterService;
    private VaccinationCenter sampleCenter;

    @BeforeEach
    void setUp() {
        resourceService = new OperationalResourceService(resourceRepository, centerRepository);
        rosterService = new OperationalStaffRosterService(rosterRepository, centerRepository);

        sampleCenter = new VaccinationCenter(
                "CTR-01",
                "Center 1",
                "Address",
                "123",
                CenterStatus.ACTIVE,
                50,
                100,
                "UTC"
        );
        ReflectionTestUtils.setField(sampleCenter, "id", 1L);
    }

    @Test
    @DisplayName("createResource - database duplicate code race translating DataIntegrityViolationException throws IllegalStateException")
    void createResource_dbDuplicateRace_translatesToIllegalStateException() {
        when(centerRepository.findById(1L)).thenReturn(Optional.of(sampleCenter));
        // Passes pre-check
        when(resourceRepository.existsByCenterIdAndCode(1L, "STATION-01"))
                .thenReturn(false) // First pre-check passes
                .thenReturn(true);  // Second check in catch block confirms duplicate

        when(resourceRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '1-STATION-01' for key 'uk_center_resource_code'"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resourceService.createResource(
                1L, "STATION-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE
        ));

        assertEquals("Resource code 'STATION-01' already exists for center 1", ex.getMessage());
    }

    @Test
    @DisplayName("createResource - unrelated DataIntegrityViolationException is rethrown without misclassification")
    void createResource_unrelatedDataIntegrityViolation_rethrown() {
        when(centerRepository.findById(1L)).thenReturn(Optional.of(sampleCenter));
        when(resourceRepository.existsByCenterIdAndCode(1L, "STATION-01")).thenReturn(false);

        when(resourceRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Unrelated database constraint failure"));

        assertThrows(DataIntegrityViolationException.class, () -> resourceService.createResource(
                1L, "STATION-01", ResourceType.VACCINATION_STATION, "Station 1", OperationalResourceStatus.ACTIVE
        ));
    }

    @Test
    @DisplayName("registerStaffMember - database duplicate user race translating DataIntegrityViolationException throws IllegalStateException")
    void registerStaffMember_dbDuplicateRace_translatesToIllegalStateException() {
        when(centerRepository.findById(1L)).thenReturn(Optional.of(sampleCenter));
        when(rosterRepository.findByUserIdForUpdateOrderByIdAsc(501L)).thenReturn(List.of());
        when(rosterRepository.existsByCenterIdAndUserId(1L, 501L)).thenReturn(true);

        when(rosterRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '1-501' for key 'uk_center_staff_user'"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> rosterService.registerStaffMember(
                1L, 501L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        ));

        assertEquals("Staff user 501 is already rostered at center 1", ex.getMessage());
    }

    @Test
    @DisplayName("registerStaffMember - unrelated DataIntegrityViolationException is rethrown without misclassification")
    void registerStaffMember_unrelatedDataIntegrityViolation_rethrown() {
        when(centerRepository.findById(1L)).thenReturn(Optional.of(sampleCenter));
        when(rosterRepository.existsByCenterIdAndUserId(1L, 501L)).thenReturn(false);

        when(rosterRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Unrelated database constraint failure"));

        assertThrows(DataIntegrityViolationException.class, () -> rosterService.registerStaffMember(
                1L, 501L, StaffRole.VACCINATOR, OperationalStaffStatus.ACTIVE
        ));
    }
}
