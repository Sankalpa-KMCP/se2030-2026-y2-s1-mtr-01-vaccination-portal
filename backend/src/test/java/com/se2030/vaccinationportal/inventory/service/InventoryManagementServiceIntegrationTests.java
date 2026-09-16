package com.se2030.vaccinationportal.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dto.request.CenterInventoryRequest;
import com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
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

        var stock = service.createInventory(new CenterInventoryRequest(center.getId(), batch.id(), 12));
        stock = service.updateInventory(stock.id(), new CenterInventoryRequest(center.getId(), batch.id(), 20));
        assertThat(stock.quantity()).isEqualTo(20);

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
}
