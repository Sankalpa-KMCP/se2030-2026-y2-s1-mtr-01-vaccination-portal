package com.se2030.vaccinationportal.inventory.service;

import com.se2030.vaccinationportal.campaign.api.reference.CampaignReferenceDataReader;
import com.se2030.vaccinationportal.inventory.dto.request.CenterInventoryRequest;
import com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockAdjustmentRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockReceiptRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockTransferRequest;
import com.se2030.vaccinationportal.inventory.dto.response.CenterInventoryResponse;
import com.se2030.vaccinationportal.inventory.dto.response.InventoryReferenceDataResponse;
import com.se2030.vaccinationportal.inventory.dto.response.InventoryReferenceDataResponse.ReferenceItem;
import com.se2030.vaccinationportal.inventory.dto.response.ManufacturerResponse;
import com.se2030.vaccinationportal.inventory.dto.response.VaccineBatchResponse;
import com.se2030.vaccinationportal.inventory.dto.response.StockTransactionResponse;
import com.se2030.vaccinationportal.inventory.dto.response.StockTransferResponse;
import com.se2030.vaccinationportal.inventory.exception.InventoryConflictException;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.Manufacturer;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.model.StockTransaction;
import com.se2030.vaccinationportal.inventory.model.StockTransactionType;
import com.se2030.vaccinationportal.inventory.model.StockTransfer;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import java.time.LocalDate;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.ManufacturerRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransferRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryManagementService {
    private final ManufacturerRepository manufacturers;
    private final VaccineBatchRepository batches;
    private final CenterInventoryRepository inventories;
    private final StockTransactionRepository transactions;
    private final StockTransferRepository transfers;
    private final CampaignReferenceDataReader campaignReferences;
    private final EntityManager entityManager;

    public InventoryManagementService(ManufacturerRepository manufacturers, VaccineBatchRepository batches,
            CenterInventoryRepository inventories, StockTransactionRepository transactions,
            CampaignReferenceDataReader campaignReferences) {
        this(manufacturers, batches, inventories, transactions, null, campaignReferences, null);
    }

    @Autowired
    public InventoryManagementService(ManufacturerRepository manufacturers, VaccineBatchRepository batches,
            CenterInventoryRepository inventories, StockTransactionRepository transactions,
            StockTransferRepository transfers, CampaignReferenceDataReader campaignReferences, EntityManager entityManager) {
        this.manufacturers = manufacturers;
        this.batches = batches;
        this.inventories = inventories;
        this.transactions = transactions;
        this.transfers = transfers;
        this.campaignReferences = campaignReferences;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ManufacturerResponse> listManufacturers() {
        return manufacturers.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ManufacturerResponse createManufacturer(ManufacturerRequest request) {
        return toResponse(saveManufacturer(new Manufacturer(clean(request.code()), clean(request.name()))));
    }

    @Transactional
    public ManufacturerResponse updateManufacturer(Long id, ManufacturerRequest request) {
        Manufacturer item = manufacturer(id);
        item.update(clean(request.code()), clean(request.name()));
        return toResponse(saveManufacturer(item));
    }

    @Transactional
    public void deleteManufacturer(Long id) {
        manufacturer(id);
        if (batches.existsByManufacturerId(id)) {
            throw new InventoryConflictException("Manufacturer is used by a vaccine batch and cannot be deleted");
        }
        manufacturers.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<VaccineBatchResponse> listBatches() {
        return batches.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public VaccineBatchResponse createBatch(VaccineBatchRequest request) {
        validateBatchRequest(request);
        VaccineBatch batch = new VaccineBatch(request.manufacturerId(), request.vaccineTypeId(),
                clean(request.batchNumber()), request.manufactureDate(), request.expiryDate(), request.batchStatus());
        return toResponse(saveBatch(batch));
    }

    @Transactional
    public VaccineBatchResponse updateBatch(Long id, VaccineBatchRequest request) {
        VaccineBatch batch = lockedBatch(id);
        // Manufacturer and vaccine type are the batch identity (uk_vaccine_batch_identity) and are
        // immutable after creation; the entity update path cannot change them, so a requested identity
        // change must be rejected instead of silently persisted as the pre-update identity.
        if (!batch.getManufacturerId().equals(request.manufacturerId())
                || !batch.getVaccineTypeId().equals(request.vaccineTypeId())) {
            throw new InventoryConflictException(
                    "Batch manufacturer and vaccine type are immutable and cannot be changed after batch creation");
        }
        validateBatchRequest(request);
        boolean lotFactsChanged = !batch.getBatchNumber().equals(clean(request.batchNumber()))
                || !batch.getManufactureDate().equals(request.manufactureDate())
                || !batch.getExpiryDate().equals(request.expiryDate());
        if (lotFactsChanged && batchHasTransactions(id)) {
            throw new InventoryConflictException("Batch master data cannot be changed after stock transactions exist");
        }
        batch.update(clean(request.batchNumber()), request.manufactureDate(), request.expiryDate(), request.batchStatus());
        return toResponse(saveBatch(batch));
    }

    @Transactional
    public void deleteBatch(Long id) {
        lockedBatch(id);
        if (inventories.existsByBatchId(id)) {
            throw new InventoryConflictException("Batch has center inventory and cannot be deleted");
        }
        batches.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CenterInventoryResponse> listInventories() {
        return inventories.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CenterInventoryResponse createInventory(CenterInventoryRequest request) {
        return createInventory(request, null);
    }

    @Transactional
    public CenterInventoryResponse createInventory(CenterInventoryRequest request, Long actorId) {
        requireReferences(request);
        if (inventories.findByCenterIdAndBatchId(request.centerId(), request.batchId()).isPresent()) {
            throw new InventoryConflictException("Center inventory already exists for this batch");
        }
        VaccineBatch batch = lockedBatch(request.batchId());
        if (request.quantity() > 0) {
            requireActor(actorId);
            validateUsableBatch(batch);
        }
        CenterInventory inventory = inventories.saveAndFlush(
                new CenterInventory(request.centerId(), request.batchId(), 0));
        if (request.quantity() > 0) {
            inventory.applyChange(request.quantity());
            inventories.saveAndFlush(inventory);
            transactions.saveAndFlush(StockTransaction.stockChange(inventory.getId(),
                    StockTransactionType.RECEIPT, request.quantity(), 0, actorId, "Initial stock receipt"));
        }
        return toResponse(inventory);
    }

    @Transactional
    public CenterInventoryResponse updateInventory(Long id, CenterInventoryRequest request) {
        CenterInventory inventory = inventory(id);
        if (!inventory.getCenterId().equals(request.centerId()) || !inventory.getBatchId().equals(request.batchId())) {
            throw new InventoryConflictException("Center and batch cannot be changed; create another inventory row");
        }
        if (inventory.getQuantity() != request.quantity()) {
            throw new InventoryConflictException("Use a stock receipt or adjustment so the quantity change is audited");
        }
        return toResponse(inventory);
    }

    @Transactional
    public void deleteInventory(Long id) {
        CenterInventory inventory = lockedInventory(id);
        if (transactions.existsByInventoryId(inventory.getId())) {
            throw new InventoryConflictException("Inventory has audit transactions and cannot be deleted");
        }
        inventories.delete(inventory);
    }

    @Transactional
    public CenterInventoryResponse receiveStock(StockReceiptRequest request, Long actorId) {
        requireActor(actorId);
        Long batchId = inventoryBatchId(request.inventoryId());
        VaccineBatch batch = lockedBatch(batchId);
        validateUsableBatch(batch);
        CenterInventory inventory = lockedInventory(request.inventoryId());
        int before = inventory.getQuantity();
        inventory.applyChange(request.quantity());
        inventories.saveAndFlush(inventory);
        transactions.saveAndFlush(StockTransaction.stockChange(inventory.getId(),
                StockTransactionType.RECEIPT, request.quantity(), before, actorId, cleanNullable(request.reason())));
        return toResponse(inventory);
    }

    @Transactional
    public CenterInventoryResponse adjustStock(StockAdjustmentRequest request, Long actorId) {
        requireActor(actorId);
        if (request.quantityChange() == 0) throw new IllegalArgumentException("Quantity change cannot be zero");
        Long batchId = inventoryBatchId(request.inventoryId());
        VaccineBatch batch = lockedBatch(batchId);
        if (batch.getExpiryDate().isBefore(LocalDate.now()) || batch.getBatchStatus() == BatchStatus.EXPIRED) {
            throw new InventoryConflictException("Expired batch stock cannot be adjusted");
        }
        CenterInventory inventory = lockedInventory(request.inventoryId());
        int before = inventory.getQuantity();
        try { inventory.applyChange(request.quantityChange()); }
        catch (IllegalStateException exception) { throw new InventoryConflictException("Adjustment would make inventory negative"); }
        inventories.saveAndFlush(inventory);
        StockTransactionType type = request.quantityChange() > 0
                ? StockTransactionType.ADJUSTMENT_INCREASE : StockTransactionType.ADJUSTMENT_DECREASE;
        transactions.saveAndFlush(StockTransaction.stockChange(inventory.getId(), type,
                request.quantityChange(), before, actorId, clean(request.reason())));
        return toResponse(inventory);
    }

    @Transactional
    public StockTransferResponse transferStock(StockTransferRequest request, Long actorId) {
        requireActor(actorId);
        if (!campaignReferences.centerExists(request.destinationCenterId())) {
            throw new IllegalArgumentException("Destination center not found");
        }
        Long batchId = inventoryBatchId(request.inventoryId());
        VaccineBatch batch = lockedBatch(batchId);
        validateUsableBatch(batch);
        CenterInventory source = lockedInventory(request.inventoryId());
        if (source.getCenterId().equals(request.destinationCenterId())) {
            throw new InventoryConflictException("Source and destination centers must differ");
        }
        if (source.getQuantity() < request.quantity()) {
            throw new InventoryConflictException("Insufficient source stock for transfer");
        }

        CenterInventory destination = inventories.findByCenterAndBatchForUpdate(
                request.destinationCenterId(), batchId).orElseGet(() -> inventories.saveAndFlush(
                        new CenterInventory(request.destinationCenterId(), batchId, 0)));
        int sourceBefore = source.getQuantity();
        int destinationBefore = destination.getQuantity();
        source.applyChange(-request.quantity());
        destination.applyChange(request.quantity());
        inventories.saveAndFlush(source);
        inventories.saveAndFlush(destination);

        StockTransfer transfer = transfers.saveAndFlush(new StockTransfer(
                UUID.randomUUID().toString(), batchId, source.getCenterId(),
                destination.getCenterId(), request.quantity(), actorId, cleanNullable(request.reason())));
        transactions.saveAndFlush(StockTransaction.transferChange(source.getId(), transfer.getId(),
                StockTransactionType.TRANSFER_OUT, -request.quantity(), sourceBefore, actorId, transfer.getReason()));
        transactions.saveAndFlush(StockTransaction.transferChange(destination.getId(), transfer.getId(),
                StockTransactionType.TRANSFER_IN, request.quantity(), destinationBefore, actorId, transfer.getReason()));
        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<StockTransferResponse> recentTransfers() {
        return transfers.findTop100ByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StockTransactionResponse> recentTransactions() {
        return transactions.findTop100ByOrderByCreatedAtDescIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryReferenceDataResponse referenceData() {
        List<ReferenceItem> types = campaignReferences.vaccineTypes().stream()
                .map(type -> new ReferenceItem(type.id(), type.code(), type.name())).toList();
        List<ReferenceItem> centerItems = campaignReferences.centers().stream()
                .map(center -> new ReferenceItem(center.id(), center.code(), center.name())).toList();
        return new InventoryReferenceDataResponse(types, centerItems);
    }

    private void validateBatchRequest(VaccineBatchRequest request) {
        if (!request.manufactureDate().isBefore(request.expiryDate())) {
            throw new IllegalArgumentException("Manufacture date must be before expiry date");
        }
        manufacturer(request.manufacturerId());
        if (!campaignReferences.vaccineTypeExists(request.vaccineTypeId())) {
            throw new IllegalArgumentException("Vaccine type not found: " + request.vaccineTypeId());
        }
    }

    private void requireReferences(CenterInventoryRequest request) {
        if (!campaignReferences.centerExists(request.centerId())) throw new IllegalArgumentException("Center not found");
    }

    private Manufacturer saveManufacturer(Manufacturer item) {
        try { return manufacturers.saveAndFlush(item); }
        catch (DataIntegrityViolationException exception) {
            throw new InventoryConflictException("Manufacturer code and name must be unique");
        }
    }

    private VaccineBatch saveBatch(VaccineBatch item) {
        try { return batches.saveAndFlush(item); }
        catch (DataIntegrityViolationException exception) {
            throw new InventoryConflictException("Batch number already exists for this manufacturer and vaccine type");
        }
    }

    private Manufacturer manufacturer(Long id) {
        return manufacturers.findById(id).orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
    }
    private VaccineBatch batch(Long id) {
        return batches.findById(id).orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));
    }
    private VaccineBatch lockedBatch(Long id) {
        if (entityManager != null) {
            VaccineBatch cached = entityManager.find(VaccineBatch.class, id);
            if (cached != null) {
                entityManager.detach(cached);
            }
        }
        VaccineBatch batch = batches.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));
        return batch;
    }
    private CenterInventory inventory(Long id) {
        return inventories.findById(id).orElseThrow(() -> new IllegalArgumentException("Center inventory not found: " + id));
    }
    private CenterInventory lockedInventory(Long id) {
        return inventories.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Center inventory not found: " + id));
    }
    private Long inventoryBatchId(Long inventoryId) {
        return inventories.findBatchIdById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Center inventory not found: " + inventoryId));
    }
    private boolean batchHasTransactions(Long batchId) {
        List<Long> inventoryIds = inventories.findIdsByBatchIdForUpdate(batchId);
        if (inventoryIds.isEmpty()) {
            return false;
        }
        return !transactions.findIdsByInventoryIdsForUpdate(inventoryIds).isEmpty();
    }
    private void validateUsableBatch(VaccineBatch batch) {
        if (batch.getBatchStatus() != BatchStatus.ACTIVE || batch.getExpiryDate().isBefore(LocalDate.now())) {
            throw new InventoryConflictException("Stock can only be received for an active, unexpired batch");
        }
    }
    private void requireActor(Long actorId) {
        if (actorId == null || actorId <= 0) throw new IllegalArgumentException("Authenticated inventory actor is required");
    }
    private String clean(String value) { return value.trim(); }
    private String cleanNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ManufacturerResponse toResponse(Manufacturer item) {
        return new ManufacturerResponse(item.getId(), item.getCode(), item.getName());
    }
    private VaccineBatchResponse toResponse(VaccineBatch item) {
        return new VaccineBatchResponse(item.getId(), item.getManufacturerId(), item.getVaccineTypeId(),
                item.getBatchNumber(), item.getManufactureDate(), item.getExpiryDate(), item.getBatchStatus());
    }
    private CenterInventoryResponse toResponse(CenterInventory item) {
        return new CenterInventoryResponse(item.getId(), item.getCenterId(), item.getBatchId(),
                item.getQuantity(), item.getLockVersion());
    }
    private StockTransactionResponse toResponse(StockTransaction item) {
        return new StockTransactionResponse(item.getId(), item.getInventoryId(), item.getStockTransferId(), item.getType(), item.getDelta(),
                item.getBeforeQty(), item.getAfterQty(), item.getActorId(), item.getReason(), item.getCreatedAt());
    }
    private StockTransferResponse toResponse(StockTransfer item) {
        return new StockTransferResponse(item.getId(), item.getCorrelationId(), item.getBatchId(),
                item.getSourceCenterId(), item.getDestinationCenterId(), item.getQuantity(),
                item.getStatus(), item.getRequestedBy(), item.getReason(), item.getCompletedAt());
    }
}
