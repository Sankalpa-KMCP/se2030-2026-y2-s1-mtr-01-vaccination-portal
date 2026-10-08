package com.se2030.vaccinationportal.inventory.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dependency.BatchAvailabilityReason;
import com.se2030.vaccinationportal.inventory.dto.request.CenterInventoryRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockAdjustmentRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockReceiptRequest;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.inventory.exception.InventoryConflictException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.Manufacturer;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.ManufacturerRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import com.se2030.vaccinationportal.inventory.service.InventoryBatchService;
import com.se2030.vaccinationportal.inventory.service.InventoryManagementService;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Concurrency regression tests for inventory batch lot-fact mutations vs stock transactions (F-045-03),
 * verifying:
 * - Deterministic serialization of lot-fact mutations with stock transactions (receipts & adjustments).
 * - Freshness guarantee (F-053-01): pessimistic refresh under lock eliminates stale batch state races
 *   in initial stock creation and vaccination deduction.
 * - Decisive first-adjustment concurrency (F-053-02): proves mutual exclusion between the first adjustment
 *   and batch metadata editing when starting with zero transaction history, without harness pre-locking.
 */
@SpringBootTest
@ActiveProfiles("test")
@Timeout(40)
public class InventoryLotFactConcurrencyIntegrationTests {

    @Autowired protected DataSource dataSource;
    @Autowired protected PlatformTransactionManager transactionManager;
    @Autowired protected EntityManager entityManager;
    @Autowired protected InventoryManagementService inventoryService;
    @Autowired protected InventoryBatchService inventoryBatchService;
    @Autowired protected VaccineBatchRepository batches;
    @Autowired protected CenterInventoryRepository inventories;
    @Autowired protected StockTransactionRepository transactions;
    @Autowired protected ManufacturerRepository manufacturers;
    @Autowired protected VaccineTypeRepository vaccineTypes;
    @Autowired protected VaccinationCenterRepository centers;

    private final List<Long> createdInventoryIds = new ArrayList<>();
    private final List<Long> createdBatchIds = new ArrayList<>();
    private final List<Long> createdManufacturerIds = new ArrayList<>();
    private final List<Long> createdCenterIds = new ArrayList<>();
    private final List<Long> createdVaccineTypeIds = new ArrayList<>();

    @AfterEach
    void cleanupFixtures() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (!createdInventoryIds.isEmpty()) {
                entityManager.createQuery("delete from StockTransaction t where t.inventoryId in :ids")
                        .setParameter("ids", createdInventoryIds)
                        .executeUpdate();
                for (Long invId : createdInventoryIds) {
                    inventories.deleteById(invId);
                }
                createdInventoryIds.clear();
            }
            for (Long batchId : createdBatchIds) {
                batches.deleteById(batchId);
            }
            createdBatchIds.clear();
            for (Long mId : createdManufacturerIds) {
                manufacturers.deleteById(mId);
            }
            createdManufacturerIds.clear();
            for (Long cId : createdCenterIds) {
                centers.deleteById(cId);
            }
            createdCenterIds.clear();
            for (Long vtId : createdVaccineTypeIds) {
                vaccineTypes.deleteById(vtId);
            }
            createdVaccineTypeIds.clear();
        });
    }

    record BatchFixture(String suffix, Long manufacturerId, Long vaccineTypeId, Long centerId,
                        Long batchId, Long inventoryId, VaccineBatchRequest batchRequest) {}

    protected BatchFixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Manufacturer manufacturer = manufacturers.save(new Manufacturer("M-" + suffix, "Manufacturer " + suffix));
        createdManufacturerIds.add(manufacturer.getId());

        VaccineType type = vaccineTypes.save(new VaccineType("VT-" + suffix, "Vaccine " + suffix));
        createdVaccineTypeIds.add(type.getId());

        VaccinationCenter center = centers.save(new VaccinationCenter("C-" + suffix, "Center " + suffix,
                "10 Health Way", "center@example.test", CenterStatus.ACTIVE, 10, 20, "UTC"));
        createdCenterIds.add(center.getId());

        VaccineBatchRequest request = new VaccineBatchRequest(
                manufacturer.getId(),
                type.getId(),
                "LOT-ORIG-" + suffix,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(90),
                BatchStatus.ACTIVE);

        var batchResp = inventoryService.createBatch(request);
        createdBatchIds.add(batchResp.id());

        var invResp = inventoryService.createInventory(
                new CenterInventoryRequest(center.getId(), batchResp.id(), 0), 900L);
        createdInventoryIds.add(invResp.id());

        return new BatchFixture(suffix, manufacturer.getId(), type.getId(), center.getId(),
                batchResp.id(), invResp.id(), request);
    }

    @Test
    void receiveStockWins_competingLotFactEditIsBlockedThenRejected() throws Exception {
        var fixture = createFixture();
        Long batchId = fixture.batchId();
        Long inventoryId = fixture.inventoryId();
        VaccineBatchRequest orig = fixture.batchRequest();

        VaccineBatchRequest lotEditRequest = new VaccineBatchRequest(
                orig.manufacturerId(),
                orig.vaccineTypeId(),
                "LOT-EDITED-" + fixture.suffix(),
                orig.manufactureDate(),
                orig.expiryDate(),
                orig.batchStatus());

        Callable<?> receiptAction = () -> inventoryService.receiveStock(
                new StockReceiptRequest(inventoryId, 25, "First batch delivery"), 901L);
        Callable<?> editAction = () -> inventoryService.updateBatch(batchId, lotEditRequest);

        Throwable loser = competeOperations(receiptAction, editAction, true);

        assertNotNull(loser, "Competing lot fact edit must be rejected after receipt commits");
        assertInstanceOf(InventoryConflictException.class, loser);
        assertTrue(loser.getMessage().contains("transactions"),
                "Expected conflict message to mention transactions: " + loser.getMessage());

        // Batch lot number must remain unchanged
        VaccineBatch persistedBatch = batches.findById(batchId).orElseThrow();
        assertThat(persistedBatch.getBatchNumber()).isEqualTo(orig.batchNumber());

        // Inventory must reflect received quantity and audit transaction
        CenterInventory persistedInv = inventories.findById(inventoryId).orElseThrow();
        assertThat(persistedInv.getQuantity()).isEqualTo(25);
        assertThat(transactions.existsByBatchId(batchId)).isTrue();
    }

    @Test
    void lotFactEditWins_competingReceiptIsBlockedThenSucceedsAgainstUpdatedBatch() throws Exception {
        var fixture = createFixture();
        Long batchId = fixture.batchId();
        Long inventoryId = fixture.inventoryId();
        VaccineBatchRequest orig = fixture.batchRequest();

        VaccineBatchRequest lotEditRequest = new VaccineBatchRequest(
                orig.manufacturerId(),
                orig.vaccineTypeId(),
                "LOT-UPDATED-" + fixture.suffix(),
                orig.manufactureDate(),
                orig.expiryDate(),
                orig.batchStatus());

        Callable<?> editAction = () -> inventoryService.updateBatch(batchId, lotEditRequest);
        Callable<?> receiptAction = () -> inventoryService.receiveStock(
                new StockReceiptRequest(inventoryId, 15, "Delivery after lot update"), 902L);

        competeOperations(editAction, receiptAction, false);

        // Batch has updated lot number
        VaccineBatch persistedBatch = batches.findById(batchId).orElseThrow();
        assertThat(persistedBatch.getBatchNumber()).isEqualTo("LOT-UPDATED-" + fixture.suffix());

        // Receipt succeeded against updated batch
        CenterInventory persistedInv = inventories.findById(inventoryId).orElseThrow();
        assertThat(persistedInv.getQuantity()).isEqualTo(15);
        assertThat(transactions.existsByBatchId(batchId)).isTrue();
    }

    @Test
    void receiptWins_competingStatusOnlyEditIsBlockedThenSucceeds() throws Exception {
        var fixture = createFixture();
        Long batchId = fixture.batchId();
        Long inventoryId = fixture.inventoryId();
        VaccineBatchRequest orig = fixture.batchRequest();

        // Operational status-only change (lot facts remain identical)
        VaccineBatchRequest statusChangeRequest = new VaccineBatchRequest(
                orig.manufacturerId(),
                orig.vaccineTypeId(),
                orig.batchNumber(),
                orig.manufactureDate(),
                orig.expiryDate(),
                BatchStatus.RECALLED);

        Callable<?> receiptAction = () -> inventoryService.receiveStock(
                new StockReceiptRequest(inventoryId, 30, "Receipt before recall"), 903L);
        Callable<?> statusEditAction = () -> inventoryService.updateBatch(batchId, statusChangeRequest);

        competeOperations(receiptAction, statusEditAction, false);

        // Both succeeded: batch status is RECALLED, stock is 30
        VaccineBatch persistedBatch = batches.findById(batchId).orElseThrow();
        assertThat(persistedBatch.getBatchStatus()).isEqualTo(BatchStatus.RECALLED);
        CenterInventory persistedInv = inventories.findById(inventoryId).orElseThrow();
        assertThat(persistedInv.getQuantity()).isEqualTo(30);
    }

    @Test
    void firstAdjustmentWins_competingLotFactEditIsBlockedThenRejected() throws Exception {
        var fixture = createFixture();
        Long batchId = fixture.batchId();
        Long inventoryId = fixture.inventoryId();
        VaccineBatchRequest orig = fixture.batchRequest();

        // Verify zero prior transactions exist for this batch
        assertThat(transactions.existsByBatchId(batchId)).isFalse();

        VaccineBatchRequest lotEditRequest = new VaccineBatchRequest(
                orig.manufacturerId(),
                orig.vaccineTypeId(),
                "LOT-MUTATED-" + fixture.suffix(),
                orig.manufactureDate(),
                orig.expiryDate(),
                orig.batchStatus());

        Callable<?> adjustAction = () -> inventoryService.adjustStock(
                new StockAdjustmentRequest(inventoryId, 5, "Initial count adjustment"), 904L);
        Callable<?> editAction = () -> inventoryService.updateBatch(batchId, lotEditRequest);

        Throwable loser = competeOperations(adjustAction, editAction, true);

        assertNotNull(loser, "Lot fact edit must be rejected after first stock adjustment");
        assertInstanceOf(InventoryConflictException.class, loser);
        assertTrue(loser.getMessage().contains("transactions"));

        VaccineBatch persistedBatch = batches.findById(batchId).orElseThrow();
        assertThat(persistedBatch.getBatchNumber()).isEqualTo(orig.batchNumber());
        CenterInventory persistedInv = inventories.findById(inventoryId).orElseThrow();
        assertThat(persistedInv.getQuantity()).isEqualTo(5);
        assertThat(transactions.existsByBatchId(batchId)).isTrue();
    }

    @Test
    void lotFactEditWins_competingFirstAdjustmentIsBlockedThenSucceeds() throws Exception {
        var fixture = createFixture();
        Long batchId = fixture.batchId();
        Long inventoryId = fixture.inventoryId();
        VaccineBatchRequest orig = fixture.batchRequest();

        // Verify zero prior transactions exist for this batch
        assertThat(transactions.existsByBatchId(batchId)).isFalse();

        VaccineBatchRequest lotEditRequest = new VaccineBatchRequest(
                orig.manufacturerId(),
                orig.vaccineTypeId(),
                "LOT-EDIT-FIRST-" + fixture.suffix(),
                orig.manufactureDate(),
                orig.expiryDate(),
                orig.batchStatus());

        Callable<?> editAction = () -> inventoryService.updateBatch(batchId, lotEditRequest);
        Callable<?> adjustAction = () -> inventoryService.adjustStock(
                new StockAdjustmentRequest(inventoryId, 8, "Adjustment after lot edit"), 905L);

        competeOperations(editAction, adjustAction, false);

        VaccineBatch persistedBatch = batches.findById(batchId).orElseThrow();
        assertThat(persistedBatch.getBatchNumber()).isEqualTo("LOT-EDIT-FIRST-" + fixture.suffix());
        CenterInventory persistedInv = inventories.findById(inventoryId).orElseThrow();
        assertThat(persistedInv.getQuantity()).isEqualTo(8);
        assertThat(transactions.existsByBatchId(batchId)).isTrue();
    }

    @Test
    void createInventory_refreshesStaleBatchUnderLock_rejectsConcurrentlyRecalledBatch() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Manufacturer manufacturer = manufacturers.save(new Manufacturer("M-CI-" + suffix, "Manufacturer " + suffix));
        createdManufacturerIds.add(manufacturer.getId());

        VaccineType type = vaccineTypes.save(new VaccineType("VT-CI-" + suffix, "Vaccine " + suffix));
        createdVaccineTypeIds.add(type.getId());

        VaccinationCenter center = centers.save(new VaccinationCenter("C-CI-" + suffix, "Center " + suffix,
                "10 Health Way", "center@example.test", CenterStatus.ACTIVE, 10, 20, "UTC"));
        createdCenterIds.add(center.getId());

        VaccineBatchRequest request = new VaccineBatchRequest(
                manufacturer.getId(),
                type.getId(),
                "LOT-STALE-CI-" + suffix,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(90),
                BatchStatus.ACTIVE);

        var batchResp = inventoryService.createBatch(request);
        Long batchId = batchResp.id();
        createdBatchIds.add(batchId);

        CountDownLatch preloaded = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> creationFuture = pool.submit(() -> {
                try {
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        // Preload the batch entity into Thread 1's persistence context (L1 cache) as ACTIVE
                        VaccineBatch cachedBatch = batches.findById(batchId).orElseThrow();
                        assertThat(cachedBatch.getBatchStatus()).isEqualTo(BatchStatus.ACTIVE);
                        preloaded.countDown();
                        await(updated);

                        // Concurrent transaction has now updated batch to RECALLED in DB.
                        // createInventory with positive quantity must acquire pessimistic lock,
                        // refresh the stale cached entity, detect RECALLED status, and reject.
                        inventoryService.createInventory(
                                new CenterInventoryRequest(center.getId(), batchId, 25), 906L);
                        return null;
                    });
                } catch (RuntimeException ex) {
                    return ex;
                }
            });

            assertTrue(preloaded.await(10, TimeUnit.SECONDS), "Thread 1 must preload batch");

            // Thread 2 updates batch to RECALLED in a separate committed transaction
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                VaccineBatch b = batches.findById(batchId).orElseThrow();
                b.update(b.getBatchNumber(), b.getManufactureDate(), b.getExpiryDate(), BatchStatus.RECALLED);
                batches.saveAndFlush(b);
            });
            updated.countDown();

            Throwable error = creationFuture.get(15, TimeUnit.SECONDS);
            assertNotNull(error, "createInventory must be rejected due to refreshed RECALLED status");
            assertInstanceOf(InventoryConflictException.class, error);
            assertTrue(error.getMessage().contains("active") || error.getMessage().contains("batch"),
                    "Expected usable batch exception: " + error.getMessage());

            // Center inventory must not exist and 0 transactions must be committed
            assertThat(inventories.findByCenterIdAndBatchId(center.getId(), batchId)).isEmpty();
            assertThat(transactions.existsByBatchId(batchId)).isFalse();
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void deductVaccinationDose_refreshesStaleBatchUnderLock_rejectsConcurrentlyRecalledBatch() throws Exception {
        var fixture = createFixture();
        Long batchId = fixture.batchId();
        Long inventoryId = fixture.inventoryId();
        Long centerId = fixture.centerId();

        // Establish 10 initial units in center inventory
        inventoryService.receiveStock(new StockReceiptRequest(inventoryId, 10, "Initial stock for deduction"), 907L);

        CountDownLatch preloaded = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> deductionFuture = pool.submit(() -> {
                try {
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        // Preload the batch via getBatchAvailability in Thread 1's persistence context
                        var snapshot = inventoryBatchService.getBatchAvailability(centerId, batchId);
                        assertThat(snapshot.isAvailable()).isTrue();
                        assertThat(snapshot.batchStatus()).isEqualTo(BatchStatus.ACTIVE);
                        preloaded.countDown();
                        await(updated);

                        // Concurrent transaction has now updated batch to RECALLED in DB.
                        // deductVaccinationDose must acquire pessimistic lock, refresh the entity,
                        // detect RECALLED status, and reject with BatchUnavailableException.
                        inventoryBatchService.deductVaccinationDose(centerId, batchId, 8888L);
                        return null;
                    });
                } catch (RuntimeException ex) {
                    return ex;
                }
            });

            assertTrue(preloaded.await(10, TimeUnit.SECONDS), "Thread 1 must preload batch availability");

            // Thread 2 updates batch to RECALLED in a separate committed transaction
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                VaccineBatch b = batches.findById(batchId).orElseThrow();
                b.update(b.getBatchNumber(), b.getManufactureDate(), b.getExpiryDate(), BatchStatus.RECALLED);
                batches.saveAndFlush(b);
            });
            updated.countDown();

            Throwable error = deductionFuture.get(15, TimeUnit.SECONDS);
            assertNotNull(error, "deductVaccinationDose must be rejected due to refreshed RECALLED status");
            assertInstanceOf(BatchUnavailableException.class, error);
            BatchUnavailableException bue = (BatchUnavailableException) error;
            assertThat(bue.getReason()).isEqualTo(BatchAvailabilityReason.RECALLED_BATCH);

            // Center inventory quantity must remain 10 (not deducted)
            CenterInventory persistedInv = inventories.findById(inventoryId).orElseThrow();
            assertThat(persistedInv.getQuantity()).isEqualTo(10);
            assertThat(transactions.existsByVaccinationRecordId(8888L)).isFalse();
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void initialStockCreationWins_competingStaleEditorIsBlockedThenRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Manufacturer manufacturer = manufacturers.save(new Manufacturer("M-SC-" + suffix, "Manufacturer " + suffix));
        createdManufacturerIds.add(manufacturer.getId());

        VaccineType type = vaccineTypes.save(new VaccineType("VT-SC-" + suffix, "Vaccine " + suffix));
        createdVaccineTypeIds.add(type.getId());

        VaccinationCenter center = centers.save(new VaccinationCenter("C-SC-" + suffix, "Center " + suffix,
                "10 Health Way", "center@example.test", CenterStatus.ACTIVE, 10, 20, "UTC"));
        createdCenterIds.add(center.getId());

        VaccineBatchRequest request = new VaccineBatchRequest(
                manufacturer.getId(),
                type.getId(),
                "LOT-INITIAL-" + suffix,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(90),
                BatchStatus.ACTIVE);

        var batchResp = inventoryService.createBatch(request);
        Long batchId = batchResp.id();
        createdBatchIds.add(batchId);

        VaccineBatchRequest lotEditRequest = new VaccineBatchRequest(
                manufacturer.getId(),
                type.getId(),
                "LOT-STALE-EDIT-" + suffix,
                request.manufactureDate(),
                request.expiryDate(),
                request.batchStatus());

        CountDownLatch editorSnapshotEstablished = new CountDownLatch(1);
        CountDownLatch creationHoldingBatchGuard = new CountDownLatch(1);
        CountDownLatch releaseCreation = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);

        try {
            // Thread 1: Editor transaction with an older snapshot established before creation commits
            Future<Throwable> editorFuture = pool.submit(() -> {
                try {
                    return new TransactionTemplate(transactionManager).execute(status -> {
                        // Establish an older MVCC snapshot in the editor before creation commits
                        VaccineBatch preRead = batches.findById(batchId).orElseThrow();
                        assertThat(preRead.getBatchNumber()).isEqualTo("LOT-INITIAL-" + suffix);
                        List<CenterInventory> preInvs = inventories.findAllByBatchId(batchId);
                        assertThat(preInvs).isEmpty();

                        editorSnapshotEstablished.countDown();
                        await(creationHoldingBatchGuard);

                        // Attempt lot mutation - will block on the VaccineBatch write lock held by Thread 2
                        inventoryService.updateBatch(batchId, lotEditRequest);
                        return null;
                    });
                } catch (RuntimeException ex) {
                    return ex;
                }
            });

            assertTrue(editorSnapshotEstablished.await(10, TimeUnit.SECONDS), "Editor must establish old snapshot");

            // Thread 2: Initial-stock creation invokes real createInventory with positive quantity
            Future<?> creationFuture = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                var invResp = inventoryService.createInventory(
                        new CenterInventoryRequest(center.getId(), batchId, 25), 950L);
                createdInventoryIds.add(invResp.id());

                // Signal that creation has executed and holds the batch guard before committing
                creationHoldingBatchGuard.countDown();
                await(releaseCreation);
                return invResp;
            }));

            assertTrue(creationHoldingBatchGuard.await(10, TimeUnit.SECONDS), "Creation must hold batch guard");

            // Verify editor is visibly blocked in database on the batch guard
            assertDatabaseLockWait();
            assertFalse(editorFuture.isDone(), "Editor must be blocked before creation commits");

            // Release creation transaction to commit
            releaseCreation.countDown();
            creationFuture.get(15, TimeUnit.SECONDS);

            // Editor unblocks, acquires batch guard, performs current-read history check, and must reject
            Throwable editorError = editorFuture.get(15, TimeUnit.SECONDS);
            assertNotNull(editorError, "Editor must be rejected after initial-stock creation commits");
            assertInstanceOf(InventoryConflictException.class, editorError);
            assertTrue(editorError.getMessage().contains("transactions"),
                    "Expected conflict message to mention transactions: " + editorError.getMessage());

            // Verify original lot facts remain unchanged
            VaccineBatch persistedBatch = batches.findById(batchId).orElseThrow();
            assertThat(persistedBatch.getBatchNumber()).isEqualTo("LOT-INITIAL-" + suffix);

            // Verify created inventory quantity remains correct
            CenterInventory persistedInv = inventories.findByCenterIdAndBatchId(center.getId(), batchId).orElseThrow();
            assertThat(persistedInv.getQuantity()).isEqualTo(25);

            // Verify stock transaction exists
            assertThat(transactions.existsByBatchId(batchId)).isTrue();
        } finally {
            releaseCreation.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Executes two competing operations where the first operation acquires production locks naturally
     * (without harness pre-locking) and holds the transaction open until the second operation is observed
     * blocked in the database.
     */
    protected Throwable competeOperations(Callable<?> firstOperation, Callable<?> secondOperation,
                                          boolean expectSecondRejection) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                Object result = call(firstOperation);
                locked.countDown();
                await(release);
                return result;
            }));
            assertTrue(locked.await(10, TimeUnit.SECONDS), "First operation must hold locks");
            Future<Throwable> second = pool.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).execute(status -> {
                        started.countDown();
                        return call(secondOperation);
                    });
                    return null;
                } catch (RuntimeException ex) {
                    return ex;
                }
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));
            assertDatabaseLockWait();
            assertFalse(second.isDone(), "Contender must be blocked before first commits");
            release.countDown();
            first.get(15, TimeUnit.SECONDS);
            Throwable error = second.get(15, TimeUnit.SECONDS);
            if (expectSecondRejection) {
                assertNotNull(error, "Expected conflicting operation to be rejected");
            } else {
                assertNull(error, "Expected compatible operation to succeed: " + error);
            }
            return error;
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    protected Connection observerConnection() throws Exception {
        return dataSource.getConnection();
    }

    private void assertDatabaseLockWait() throws Exception {
        try (Connection connection = observerConnection(); var statement = connection.createStatement()) {
            String sql = connection.getMetaData().getDatabaseProductName().equals("MySQL")
                    ? "SELECT COUNT(*) FROM performance_schema.data_lock_waits"
                    : "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_STATE = 'BLOCKED'";
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            do {
                try (var result = statement.executeQuery(sql)) {
                    result.next();
                    if (result.getInt(1) > 0) return;
                }
                Thread.onSpinWait();
            } while (System.nanoTime() < deadline);
            fail("Competing service must actually wait for the database guard");
        }
    }

    private static Object call(Callable<?> action) {
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(15, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
