package com.se2030.vaccinationportal.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CenterInventoryRequest(
        @NotNull @Positive Long centerId,
        @NotNull @Positive Long batchId,
        @NotNull @PositiveOrZero Integer quantity) {}
