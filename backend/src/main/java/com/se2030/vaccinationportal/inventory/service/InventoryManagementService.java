package com.se2030.vaccinationportal.inventory.service;

import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.dto.request.CenterInventoryRequest;
import com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockAdjustmentRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockReceiptRequest;
import com.se2030.vaccinationportal.inventory.dto.response.CenterInventoryResponse;
import com.se2030.vaccinationportal.inventory.dto.response.InventoryReferenceDataResponse;
import com.se2030.vaccinationportal.inventory.dto.response.InventoryReferenceDataResponse.ReferenceItem;
import com.se2030.vaccinationportal.inventory.dto.response.ManufacturerResponse;
import com.se2030.vaccinationportal.inventory.dto.response.VaccineBatchResponse;
import com.se2030.vaccinationportal.inventory.dto.response.StockTransactionResponse;
import com.se2030.vaccinationportal.inventory.exception.InventoryConflictException;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.Manufacturer;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.model.StockTransaction;
import com.se2030.vaccinationportal.inventory.model.StockTransactionType;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import java.time.LocalDate;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.ManufacturerRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryManagementService {
    private final ManufacturerRepository manufacturers;
    private final VaccineBatchRepository batches;
    private final CenterInventoryRepository inventories;
    private final StockTransactionRepository transactions;
    private final VaccineTypeRepository vaccineTypes;
    private final VaccinationCenterRepository centers;

    public InventoryManagementService(ManufacturerRepository manufacturers, VaccineBatchRepository batches,
            CenterInventoryRepository inventories, StockTransactionRepository transactions,
            VaccineTypeRepository vaccineTypes, VaccinationCenterRepository centers) {
        this.manufacturers = manufacturers;
        this.batches = batches;
        this.inventories = inventories;
        this.transactions = transactions;
        this.vaccineTypes = vaccineTypes;
        this.centers = centers;
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
        validateBatchRequest(request);
        VaccineBatch batch = batch(id);
        boolean lotFactsChanged = !batch.getManufacturerId().equals(request.manufacturerId())
                || !batch.getVaccineTypeId().equals(request.vaccineTypeId())
                || !batch.getBatchNumber().equals(clean(request.batchNumber()))
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
        batch(id);
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
        CenterInventory inventory = inventories.saveAndFlush(
                new CenterInventory(request.centerId(), request.batchId(), 0));
        if (request.quantity() > 0) {
            requireActor(actorId);
            validateUsableBatch(batch(request.batchId()));
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
        inventory.setQuantity(request.quantity());
        return toResponse(inventories.saveAndFlush(inventory));
    }

    @Transactional
    public void deleteInventory(Long id) {
        CenterInventory inventory = inventory(id);
        if (transactions.existsByInventoryId(inventory.getId())) {
            throw new InventoryConflictException("Inventory has audit transactions and cannot be deleted");
        }
        inventories.delete(inventory);
    }

    @Transactional
    public CenterInventoryResponse receiveStock(StockReceiptRequest request, Long actorId) {
        requireActor(actorId);
        CenterInventory inventory = lockedInventory(request.inventoryId());
        validateUsableBatch(batch(inventory.getBatchId()));
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
        CenterInventory inventory = lockedInventory(request.inventoryId());
        VaccineBatch batch = batch(inventory.getBatchId());
        if (batch.getExpiryDate().isBefore(LocalDate.now()) || batch.getBatchStatus() == BatchStatus.EXPIRED) {
            throw new InventoryConflictException("Expired batch stock cannot be adjusted");
        }
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

    @Transactional(readOnly = true)
    public List<StockTransactionResponse> recentTransactions() {
        return transactions.findTop100ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryReferenceDataResponse referenceData() {
        List<ReferenceItem> types = vaccineTypes.findAll().stream()
                .map(type -> new ReferenceItem(type.getId(), type.getCode(), type.getName())).toList();
        List<ReferenceItem> centerItems = centers.findAll().stream()
                .map(center -> new ReferenceItem(center.getId(), center.getCode(), center.getName())).toList();
        return new InventoryReferenceDataResponse(types, centerItems);
    }

    private void validateBatchRequest(VaccineBatchRequest request) {
        if (!request.manufactureDate().isBefore(request.expiryDate())) {
            throw new IllegalArgumentException("Manufacture date must be before expiry date");
        }
        manufacturer(request.manufacturerId());
        if (!vaccineTypes.existsById(request.vaccineTypeId())) {
            throw new IllegalArgumentException("Vaccine type not found: " + request.vaccineTypeId());
        }
    }

    private void requireReferences(CenterInventoryRequest request) {
        if (!centers.existsById(request.centerId())) throw new IllegalArgumentException("Center not found");
        batch(request.batchId());
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
    private CenterInventory inventory(Long id) {
        return inventories.findById(id).orElseThrow(() -> new IllegalArgumentException("Center inventory not found: " + id));
    }
    private CenterInventory lockedInventory(Long id) {
        return inventories.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Center inventory not found: " + id));
    }
    private boolean batchHasTransactions(Long batchId) {
        return inventories.findAllByBatchId(batchId).stream()
                .anyMatch(item -> transactions.existsByInventoryId(item.getId()));
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
        return new StockTransactionResponse(item.getId(), item.getInventoryId(), item.getType(), item.getDelta(),
                item.getBeforeQty(), item.getAfterQty(), item.getActorId(), item.getReason(), item.getCreatedAt());
    }
}
