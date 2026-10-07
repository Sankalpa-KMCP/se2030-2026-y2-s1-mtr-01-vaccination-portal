package com.se2030.vaccinationportal.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.inventory.exception.DuplicateVaccinationDeductionException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.Manufacturer;
import com.se2030.vaccinationportal.inventory.model.StockTransactionType;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.ManufacturerRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryBatchServiceIntegrationTests {
    @Autowired InventoryBatchPort inventoryPort;
    @Autowired ManufacturerRepository manufacturers;
    @Autowired VaccineBatchRepository batches;
    @Autowired CenterInventoryRepository inventories;
    @Autowired StockTransactionRepository transactions;
    @Autowired VaccineTypeRepository vaccineTypes;
    @Autowired VaccinationCenterRepository centers;

    private Long centerId;
    private Long manufacturerId;
    private Long batchId;
    private Long inventoryId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Manufacturer manufacturer = manufacturers.save(new Manufacturer("M-" + suffix, "Maker " + suffix));
        manufacturerId = manufacturer.getId();
        VaccineType type = vaccineTypes.save(new VaccineType("VT-" + suffix, "Vaccine " + suffix));
        VaccinationCenter center = centers.save(new VaccinationCenter(
                "C-" + suffix, "Center " + suffix, "100 Test Road", "contact@example.test",
                CenterStatus.ACTIVE, 10, 20, "UTC"));
        VaccineBatch batch = batches.save(new VaccineBatch(manufacturer.getId(), type.getId(),
                "LOT-" + suffix, LocalDate.now().minusDays(30), LocalDate.now().plusDays(30),
                BatchStatus.ACTIVE));
        CenterInventory stock = inventories.save(new CenterInventory(center.getId(), batch.getId(), 2));
        centerId = center.getId();
        batchId = batch.getId();
        inventoryId = stock.getId();
    }

    @Test
    void batchNotFound() {
        assertThat(inventoryPort.getBatchAvailability(centerId, Long.MAX_VALUE).reason())
                .isEqualTo(BatchAvailabilityReason.BATCH_NOT_FOUND);
        assertUnavailable(Long.MAX_VALUE, 11001L, BatchAvailabilityReason.BATCH_NOT_FOUND);
    }

    @Test
    void availableBatchSnapshotIncludesIdentityAndCenterQuantity() {
        var batch = batches.findById(batchId).orElseThrow();
        var available = inventoryPort.findAvailableBatches(centerId, batch.getVaccineTypeId(),
                LocalDate.now(ZoneOffset.UTC));

        assertThat(available).hasSize(1);
        assertThat(available.get(0).batchId()).isEqualTo(batchId);
        assertThat(available.get(0).batchNumber()).isEqualTo(batch.getBatchNumber());
        assertThat(available.get(0).vaccineTypeId()).isEqualTo(batch.getVaccineTypeId());
        assertThat(available.get(0).availableQuantity()).isEqualTo(2);
        assertThat(inventoryPort.findBatchDisplay(batchId)).get()
                .extracting("batchNumber", "vaccineTypeId")
                .containsExactly(batch.getBatchNumber(), batch.getVaccineTypeId());
        assertThat(inventoryPort.findBatchDisplay(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void availableBatchLookupExcludesMismatchedInactiveExpiredAndEmptyStock() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        var batch = batches.findById(batchId).orElseThrow();
        assertThat(inventoryPort.findAvailableBatches(centerId, batch.getVaccineTypeId() + 1, today)).isEmpty();
        changeStatus(BatchStatus.INACTIVE);
        assertThat(inventoryPort.findAvailableBatches(centerId, null, today)).isEmpty();
        changeStatus(BatchStatus.ACTIVE);
        assertThat(inventoryPort.findAvailableBatches(centerId, null, today.plusYears(1))).isEmpty();
        inventories.findById(inventoryId).ifPresent(stock -> {
            stock.setQuantity(0);
            inventories.saveAndFlush(stock);
        });
        assertThat(inventoryPort.findAvailableBatches(centerId, null, today)).isEmpty();
    }

    @Test
    void inactiveBatch() {
        changeStatus(BatchStatus.INACTIVE);
        assertUnavailable(batchId, 11002L, BatchAvailabilityReason.INACTIVE_BATCH);
    }

    @Test
    void expiredBatch() {
        VaccineBatch batch = batches.findById(batchId).orElseThrow();
        // Expiry is immutable lot data. Create an already-expired batch instead.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        // The service evaluates expiry against the production UTC clock, so the expired batch
        // must be created relative to the UTC date to stay deterministic across time zones.
        VaccineBatch expired = batches.save(new VaccineBatch(
                manufacturerId, batch.getVaccineTypeId(),
                "EXP-" + suffix, LocalDate.now(ZoneOffset.UTC).minusDays(60),
                LocalDate.now(ZoneOffset.UTC).minusDays(1),
                BatchStatus.ACTIVE));
        inventories.save(new CenterInventory(centerId, expired.getId(), 2));
        assertUnavailable(expired.getId(), 11003L, BatchAvailabilityReason.EXPIRED_BATCH);
    }

    @Test
    void recalledBatch() {
        changeStatus(BatchStatus.RECALLED);
        assertThat(inventoryPort.getBatchAvailability(centerId, batchId).isRecalled()).isTrue();
        assertUnavailable(batchId, 11004L, BatchAvailabilityReason.RECALLED_BATCH);
    }

    @Test
    void insufficientQuantity() {
        Long otherCenter = centers.save(new VaccinationCenter(
                "C-" + UUID.randomUUID().toString().substring(0, 8), "Other Center",
                "200 Test Road", "other@example.test", CenterStatus.ACTIVE, 10, 20, "UTC")).getId();
        assertThat(inventoryPort.getBatchAvailability(otherCenter, batchId).availableQuantity()).isZero();
        assertThatThrownBy(() -> inventoryPort.deductVaccinationDose(otherCenter, batchId, 11005L))
                .isInstanceOf(BatchUnavailableException.class)
                .hasMessageContaining("INSUFFICIENT_STOCK");
    }

    @Test
    void successfulDeductionCreatesOneAuditRowAndDoesNotReserveDuringRead() {
        assertThat(inventoryPort.getBatchAvailability(centerId, batchId).isAvailable()).isTrue();
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isEqualTo(2);
        inventoryPort.deductVaccinationDose(centerId, batchId, 11006L);
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isEqualTo(1);
        var audit = transactions.findByVaccinationRecordId(11006L).orElseThrow();
        assertThat(audit.getType()).isEqualTo(StockTransactionType.VACCINATION_DEDUCTION);
        assertThat(audit.getDelta()).isEqualTo(-1);
        assertThat(audit.getBeforeQty()).isEqualTo(2);
        assertThat(audit.getAfterQty()).isEqualTo(1);
    }

    @Test
    void duplicateDeductionCannotConsumeSecondDose() {
        inventoryPort.deductVaccinationDose(centerId, batchId, 11007L);
        assertThatThrownBy(() -> inventoryPort.deductVaccinationDose(centerId, batchId, 11007L))
                .isInstanceOf(DuplicateVaccinationDeductionException.class);
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isEqualTo(1);
        assertThat(transactions.findByVaccinationRecordId(11007L)).isPresent();
    }

    private void changeStatus(BatchStatus status) {
        VaccineBatch batch = batches.findById(batchId).orElseThrow();
        batch.setBatchStatus(status);
        batches.saveAndFlush(batch);
    }

    private void assertUnavailable(Long candidateBatchId, Long recordId, BatchAvailabilityReason reason) {
        assertThat(inventoryPort.getBatchAvailability(centerId, candidateBatchId).reason()).isEqualTo(reason);
        assertThatThrownBy(() -> inventoryPort.deductVaccinationDose(centerId, candidateBatchId, recordId))
                .isInstanceOf(BatchUnavailableException.class)
                .hasMessageContaining(reason.name());
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isEqualTo(2);
    }
}
