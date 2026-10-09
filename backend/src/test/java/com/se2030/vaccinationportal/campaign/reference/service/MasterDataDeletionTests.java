package com.se2030.vaccinationportal.campaign.reference.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MasterDataDeletionTests {
    @Test
    void concurrentVaccineReferenceAtFlushBecomesConflict() {
        var vaccines = mock(VaccineTypeRepository.class);
        var campaigns = mock(VaccinationCampaignRepository.class);
        var batches = mock(InventoryBatchPort.class);
        var entry = new VaccineType("RACE", "Race vaccine", "dose", true);
        when(vaccines.findById(1L)).thenReturn(Optional.of(entry));
        doThrow(new DataIntegrityViolationException("Foreign key restriction")).when(vaccines).flush();
        assertThatThrownBy(() -> new VaccineTypeService(vaccines, campaigns, batches).deleteVaccineType(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Deactivate it instead")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        verify(campaigns).existsByVaccineType_Id(1L);
        verify(batches).hasBatchesForVaccineType(1L);
        verify(vaccines).delete(entry);
    }

    @Test
    void concurrentTargetGroupReferenceAtFlushBecomesConflict() {
        var groups = mock(TargetGroupRepository.class);
        var campaigns = mock(VaccinationCampaignRepository.class);
        var entry = new TargetGroup("RACE", "Race group", true);
        when(groups.findById(1L)).thenReturn(Optional.of(entry));
        doThrow(new DataIntegrityViolationException("Foreign key restriction")).when(groups).flush();
        assertThatThrownBy(() -> new TargetGroupService(groups, campaigns).deleteTargetGroup(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Deactivate it instead")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        verify(campaigns).existsByTargetGroup_Id(1L);
        verify(groups).delete(entry);
    }

    @Test
    void missingEntriesAreNotDeleted() {
        var vaccines = mock(VaccineTypeRepository.class);
        var groups = mock(TargetGroupRepository.class);
        var campaigns = mock(VaccinationCampaignRepository.class);
        var batches = mock(InventoryBatchPort.class);
        assertThatThrownBy(() -> new VaccineTypeService(vaccines, campaigns, batches).deleteVaccineType(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> new TargetGroupService(groups, campaigns).deleteTargetGroup(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(campaigns, batches);
        verify(vaccines, never()).flush();
        verify(groups, never()).flush();
    }
}
