package com.se2030.vaccinationportal.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
import com.se2030.vaccinationportal.inventory.exception.InventoryConflictException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression tests for the inventory batch update contract (R08):
 * manufacturer and vaccine type are the batch identity and are immutable after
 * batch creation. A requested identity change must be rejected with a controlled
 * domain error instead of being silently ignored while the API reports success.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryBatchIdentityImmutabilityTests {
    @Autowired InventoryManagementService service;
    @Autowired VaccineTypeRepository vaccineTypes;
    @Autowired VaccinationCenterRepository centers;

    private VaccineBatchRequest batchRequest(Long manufacturerId, Long vaccineTypeId, String batchNumber) {
        return new VaccineBatchRequest(manufacturerId, vaccineTypeId, batchNumber,
                LocalDate.now().minusDays(2), LocalDate.now().plusYears(1), BatchStatus.ACTIVE);
    }

    @Test
    void updateBatch_withUnchangedIdentity_updatesMutableFields() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M-" + suffix, "R08 Maker"));
        VaccineType type = vaccineTypes.save(new VaccineType("R08-VT-" + suffix, "R08 Vaccine"));

        var batch = service.createBatch(batchRequest(manufacturer.id(), type.getId(), "R08-LOT-" + suffix));

        var updated = service.updateBatch(batch.id(), batchRequest(manufacturer.id(), type.getId(), "R08-LOT-REV-" + suffix));

        assertThat(updated.batchNumber()).isEqualTo("R08-LOT-REV-" + suffix);
        assertThat(updated.manufacturerId()).isEqualTo(manufacturer.id());
        assertThat(updated.vaccineTypeId()).isEqualTo(type.getId());
    }

    @Test
    void updateBatch_withChangedManufacturer_isRejected() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M2-" + suffix, "R08 Maker Two"));
        var otherManufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M3-" + suffix, "R08 Maker Three"));
        VaccineType type = vaccineTypes.save(new VaccineType("R08-VT2-" + suffix, "R08 Vaccine Two"));

        var batch = service.createBatch(batchRequest(manufacturer.id(), type.getId(), "R08-LOT2-" + suffix));

        assertThatThrownBy(() -> service.updateBatch(batch.id(),
                batchRequest(otherManufacturer.id(), type.getId(), batch.batchNumber())))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("immutable");

        var persisted = service.listBatches().stream()
                .filter(item -> item.id().equals(batch.id()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.manufacturerId()).isEqualTo(manufacturer.id());
        assertThat(persisted.vaccineTypeId()).isEqualTo(type.getId());
        assertThat(persisted.batchNumber()).isEqualTo(batch.batchNumber());
    }

    @Test
    void updateBatch_withChangedVaccineType_isRejected() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M4-" + suffix, "R08 Maker Four"));
        VaccineType type = vaccineTypes.save(new VaccineType("R08-VT3-" + suffix, "R08 Vaccine Three"));
        VaccineType otherType = vaccineTypes.save(new VaccineType("R08-VT4-" + suffix, "R08 Vaccine Four"));

        var batch = service.createBatch(batchRequest(manufacturer.id(), type.getId(), "R08-LOT3-" + suffix));

        assertThatThrownBy(() -> service.updateBatch(batch.id(),
                batchRequest(manufacturer.id(), otherType.getId(), batch.batchNumber())))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("immutable");

        var persisted = service.listBatches().stream()
                .filter(item -> item.id().equals(batch.id()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.vaccineTypeId()).isEqualTo(type.getId());
    }

    @Test
    void updateBatch_withBothIdentityFieldsChanged_isRejected() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M5-" + suffix, "R08 Maker Five"));
        var otherManufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M6-" + suffix, "R08 Maker Six"));
        VaccineType type = vaccineTypes.save(new VaccineType("R08-VT5-" + suffix, "R08 Vaccine Five"));
        VaccineType otherType = vaccineTypes.save(new VaccineType("R08-VT6-" + suffix, "R08 Vaccine Six"));

        var batch = service.createBatch(batchRequest(manufacturer.id(), type.getId(), "R08-LOT4-" + suffix));

        assertThatThrownBy(() -> service.updateBatch(batch.id(),
                batchRequest(otherManufacturer.id(), otherType.getId(), batch.batchNumber())))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("immutable");

        var persisted = service.listBatches().stream()
                .filter(item -> item.id().equals(batch.id()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.manufacturerId()).isEqualTo(manufacturer.id());
        assertThat(persisted.vaccineTypeId()).isEqualTo(type.getId());
    }

    @Test
    void updateBatch_withUnknownBatch_stillReturnsNotFoundStyleError() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest(
                "R08M7-" + suffix, "R08 Maker Seven"));
        VaccineType type = vaccineTypes.save(new VaccineType("R08-VT7-" + suffix, "R08 Vaccine Seven"));

        assertThatThrownBy(() -> service.updateBatch(999999999L,
                batchRequest(manufacturer.id(), type.getId(), "R08-LOT5-" + suffix)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch not found");

        // Unchanged identity on a missing batch must not create anything
        assertThat(service.listBatches()).noneMatch(item -> item.batchNumber().startsWith("R08-LOT5-"));
    }
}
