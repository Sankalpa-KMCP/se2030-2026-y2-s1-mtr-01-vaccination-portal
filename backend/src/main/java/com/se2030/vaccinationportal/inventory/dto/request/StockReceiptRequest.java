package com.se2030.vaccinationportal.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockReceiptRequest(
        @NotNull @Positive Long inventoryId,
        @NotNull @Positive Integer quantity,
        @Size(max = 500) String reason) {}
