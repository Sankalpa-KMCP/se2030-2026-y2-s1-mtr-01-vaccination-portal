package com.se2030.vaccinationportal.inventory.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull @Positive Long inventoryId,
        @NotNull @Min(-1000000) @Max(1000000) Integer quantityChange,
        @NotBlank @Size(max = 500) String reason) {}
