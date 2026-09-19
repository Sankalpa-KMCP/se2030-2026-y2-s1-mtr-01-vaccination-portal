package com.se2030.vaccinationportal.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dto.request.CenterInventoryRequest;
import com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockAdjustmentRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockReceiptRequest;
import com.se2030.vaccinationportal.inventory.exception.InventoryConflictException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryManagementServiceIntegrationTests {
    @Autowired InventoryManagementService service;
    @Autowired VaccineTypeRepository vaccineTypes;
    @Autowired VaccinationCenterRepository centers;

    @Test
    void performsManufacturerBatchAndCenterStockCrud() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new ManufacturerRequest("CRUD-" + suffix, "CRUD Maker"));
        manufacturer = service.updateManufacturer(manufacturer.id(),
                new ManufacturerRequest("CRUD-" + suffix, "Updated Maker"));
        assertThat(service.listManufacturers()).contains(manufacturer);

        VaccineType type = vaccineTypes.save(new VaccineType("CRUD-VT-" + suffix, "CRUD Vaccine"));
        VaccinationCenter center = centers.save(new VaccinationCenter("CRUD-C-" + suffix, "CRUD Center",
                "1 Test Road", "test@example.test", CenterStatus.ACTIVE, 10, 20, "UTC"));
        var batch = service.createBatch(new VaccineBatchRequest(manufacturer.id(), type.getId(),
                "CRUD-LOT-" + suffix, LocalDate.now().minusDays(2), LocalDate.now().plusYears(1), BatchStatus.ACTIVE));
        batch = service.updateBatch(batch.id(), new VaccineBatchRequest(manufacturer.id(), type.getId(),
                batch.batchNumber(), batch.manufactureDate(), batch.expiryDate(), BatchStatus.QUARANTINED));
        assertThat(batch.batchStatus()).isEqualTo(BatchStatus.QUARANTINED);

        var stock = service.createInventory(new CenterInventoryRequest(center.getId(), batch.id(), 0), 999L);
        assertThat(stock.quantity()).isZero();

        Long stockId = stock.id();
        Long batchId = batch.id();
        Long manufacturerId = manufacturer.id();
        service.deleteInventory(stock.id());
        service.deleteBatch(batch.id());
        service.deleteManufacturer(manufacturer.id());
        assertThat(service.listInventories()).noneMatch(item -> item.id().equals(stockId));
        assertThat(service.listBatches()).noneMatch(item -> item.id().equals(batchId));
        assertThat(service.listManufacturers()).noneMatch(item -> item.id().equals(manufacturerId));
    }

    @Test
    void receiptsAndAdjustmentsAreAuditedAndCannotMakeStockNegative() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var manufacturer = service.createManufacturer(new ManufacturerRequest("OPS-" + suffix, "Ops Maker " + suffix));
        VaccineType type = vaccineTypes.save(new VaccineType("OPS-VT-" + suffix, "Ops Vaccine"));
        VaccinationCenter center = centers.save(new VaccinationCenter("OPS-C-" + suffix, "Ops Center",
                "2 Test Road", "ops@example.test", CenterStatus.ACTIVE, 10, 20, "UTC"));
        var batch = service.createBatch(new VaccineBatchRequest(manufacturer.id(), type.getId(), "OPS-LOT-" + suffix,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(60), BatchStatus.ACTIVE));
        var stock = service.createInventory(new CenterInventoryRequest(center.getId(), batch.id(), 0), 999L);

        assertThat(service.receiveStock(new StockReceiptRequest(stock.id(), 20, "Delivery"), 999L).quantity()).isEqualTo(20);
        assertThat(service.adjustStock(new StockAdjustmentRequest(stock.id(), -3, "Damaged vials"), 999L).quantity()).isEqualTo(17);
        assertThat(service.recentTransactions()).filteredOn(item -> item.inventoryId().equals(stock.id()))
                .extracting(item -> item.quantityChange()).containsExactly(-3, 20);
        assertThatThrownBy(() -> service.adjustStock(
                new StockAdjustmentRequest(stock.id(), -18, "Invalid"), 999L))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("negative");
        assertThatThrownBy(() -> service.updateBatch(batch.id(), new VaccineBatchRequest(manufacturer.id(), type.getId(),
                "CHANGED-LOT", batch.manufactureDate(), batch.expiryDate(), BatchStatus.ACTIVE)))
                .isInstanceOf(InventoryConflictException.class)
                .hasMessageContaining("transactions");
    }
}
