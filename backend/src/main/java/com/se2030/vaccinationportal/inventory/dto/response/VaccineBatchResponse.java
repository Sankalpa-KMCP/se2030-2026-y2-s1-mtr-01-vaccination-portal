package com.se2030.vaccinationportal.inventory.dto.response;

import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import java.time.LocalDate;

public record VaccineBatchResponse(Long id, Long manufacturerId, Long vaccineTypeId,
        String batchNumber, LocalDate manufactureDate, LocalDate expiryDate, BatchStatus batchStatus) {}
