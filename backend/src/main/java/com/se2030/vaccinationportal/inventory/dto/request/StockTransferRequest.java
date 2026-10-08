package com.se2030.vaccinationportal.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockTransferRequest(
        @NotNull @Positive Long inventoryId,
        @NotNull @Positive Long destinationCenterId,
        @NotNull @Positive Integer quantity,
        @Size(max = 500) String reason) {}
