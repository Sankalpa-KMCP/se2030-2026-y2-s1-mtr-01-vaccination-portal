package com.se2030.vaccinationportal.inventory.controller;

import com.se2030.vaccinationportal.inventory.dto.request.CenterInventoryRequest;
import com.se2030.vaccinationportal.inventory.dto.request.ManufacturerRequest;
import com.se2030.vaccinationportal.inventory.dto.request.VaccineBatchRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockAdjustmentRequest;
import com.se2030.vaccinationportal.inventory.dto.request.StockReceiptRequest;
import com.se2030.vaccinationportal.inventory.dto.response.CenterInventoryResponse;
import com.se2030.vaccinationportal.inventory.dto.response.InventoryReferenceDataResponse;
import com.se2030.vaccinationportal.inventory.dto.response.ManufacturerResponse;
import com.se2030.vaccinationportal.inventory.dto.response.VaccineBatchResponse;
import com.se2030.vaccinationportal.inventory.dto.response.StockTransactionResponse;
import com.se2030.vaccinationportal.inventory.service.InventoryManagementService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasAnyRole('ADMIN','INVENTORY_OFFICER')")
public class InventoryManagementController {
    private final InventoryManagementService service;

    public InventoryManagementController(InventoryManagementService service) { this.service = service; }

    @GetMapping("/manufacturers")
    public List<ManufacturerResponse> manufacturers() { return service.listManufacturers(); }
    @PostMapping("/manufacturers") @ResponseStatus(HttpStatus.CREATED)
    public ManufacturerResponse createManufacturer(@Valid @RequestBody ManufacturerRequest request) {
        return service.createManufacturer(request);
    }
    @PutMapping("/manufacturers/{id}")
    public ManufacturerResponse updateManufacturer(@PathVariable Long id, @Valid @RequestBody ManufacturerRequest request) {
        return service.updateManufacturer(id, request);
    }
    @DeleteMapping("/manufacturers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteManufacturer(@PathVariable Long id) { service.deleteManufacturer(id); }

    @GetMapping("/batches")
    public List<VaccineBatchResponse> batches() { return service.listBatches(); }
    @PostMapping("/batches") @ResponseStatus(HttpStatus.CREATED)
    public VaccineBatchResponse createBatch(@Valid @RequestBody VaccineBatchRequest request) {
        return service.createBatch(request);
    }
    @PutMapping("/batches/{id}")
    public VaccineBatchResponse updateBatch(@PathVariable Long id, @Valid @RequestBody VaccineBatchRequest request) {
        return service.updateBatch(id, request);
    }
    @DeleteMapping("/batches/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBatch(@PathVariable Long id) { service.deleteBatch(id); }

    @GetMapping("/stocks")
    public List<CenterInventoryResponse> stocks() { return service.listInventories(); }
    @PostMapping("/stocks") @ResponseStatus(HttpStatus.CREATED)
    public CenterInventoryResponse createStock(@Valid @RequestBody CenterInventoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.createInventory(request, principal.getUserId());
    }
    @PutMapping("/stocks/{id}")
    public CenterInventoryResponse updateStock(@PathVariable Long id, @Valid @RequestBody CenterInventoryRequest request) {
        return service.updateInventory(id, request);
    }
    @DeleteMapping("/stocks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStock(@PathVariable Long id) { service.deleteInventory(id); }

    @PostMapping("/operations/receipts")
    public CenterInventoryResponse receiveStock(@Valid @RequestBody StockReceiptRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.receiveStock(request, principal.getUserId());
    }

    @PostMapping("/operations/adjustments")
    public CenterInventoryResponse adjustStock(@Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.adjustStock(request, principal.getUserId());
    }

    @GetMapping("/transactions")
    public List<StockTransactionResponse> transactions() { return service.recentTransactions(); }

    @GetMapping("/reference-data")
    public InventoryReferenceDataResponse referenceData() { return service.referenceData(); }
}
