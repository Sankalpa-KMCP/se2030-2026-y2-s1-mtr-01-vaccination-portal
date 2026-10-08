package com.se2030.vaccinationportal.inventory.dto.response;

import java.time.Instant;

public record StockTransferResponse(Long id, String correlationId, Long batchId,
        Long sourceCenterId, Long destinationCenterId, int quantity, String status,
        Long requestedBy, String reason, Instant completedAt) {}
