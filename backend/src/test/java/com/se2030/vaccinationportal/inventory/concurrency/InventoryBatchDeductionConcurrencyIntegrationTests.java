package com.se2030.vaccinationportal.inventory.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.Manufacturer;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.ManufacturerRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventoryBatchDeductionConcurrencyIntegrationTests {
    @Autowired InventoryBatchPort inventoryPort;
    @Autowired ManufacturerRepository manufacturers;
    @Autowired VaccineBatchRepository batches;
    @Autowired CenterInventoryRepository inventories;
    @Autowired StockTransactionRepository transactions;
    @Autowired VaccineTypeRepository vaccineTypes;
    @Autowired VaccinationCenterRepository centers;

    @Test
    @Timeout(20)
    void onlyOneOfTwoFinalizationsCanDeductLastDose() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long manufacturerId = manufacturers.save(new Manufacturer("CM-" + suffix, "Concurrency Maker " + suffix)).getId();
        Long vaccineTypeId = vaccineTypes.save(new VaccineType("CV-" + suffix, "Concurrency Vaccine " + suffix)).getId();
        Long centerId = centers.save(new VaccinationCenter("CC-" + suffix, "Concurrency Center",
                "100 Test Road", "contact@example.test", CenterStatus.ACTIVE, 10, 20, "UTC")).getId();
        Long batchId = batches.save(new VaccineBatch(manufacturerId, vaccineTypeId, "CL-" + suffix,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(30), BatchStatus.ACTIVE)).getId();
        Long inventoryId = inventories.save(new CenterInventory(centerId, batchId, 1)).getId();

        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Throwable> first = pool.submit(() -> attempt(start, centerId, batchId, 12001L));
            Future<Throwable> second = pool.submit(() -> attempt(start, centerId, batchId, 12002L));
            start.countDown();
            Throwable firstResult = first.get(15, TimeUnit.SECONDS);
            Throwable secondResult = second.get(15, TimeUnit.SECONDS);
            assertThat(firstResult == null ^ secondResult == null).isTrue();
            assertThat(firstResult == null ? secondResult : firstResult)
                    .isInstanceOf(BatchUnavailableException.class);
        }
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantity()).isZero();
        assertThat(transactions.existsByVaccinationRecordId(12001L) ^
                transactions.existsByVaccinationRecordId(12002L)).isTrue();
    }

    private Throwable attempt(CountDownLatch start, Long centerId, Long batchId, Long recordId) {
        try {
            start.await();
            inventoryPort.deductVaccinationDose(centerId, batchId, recordId);
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }
}
