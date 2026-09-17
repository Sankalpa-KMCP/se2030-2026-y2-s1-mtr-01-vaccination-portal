package com.se2030.vaccinationportal.inventory.dto.request;

import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record VaccineBatchRequest(
        @NotNull @Positive Long manufacturerId,
        @NotNull @Positive Long vaccineTypeId,
        @NotBlank @Size(max = 128) String batchNumber,
        @NotNull LocalDate manufactureDate,
        @NotNull LocalDate expiryDate,
        @NotNull BatchStatus batchStatus) {}
