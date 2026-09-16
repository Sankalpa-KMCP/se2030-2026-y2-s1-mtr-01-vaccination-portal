package com.se2030.vaccinationportal.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManufacturerRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name) {}
