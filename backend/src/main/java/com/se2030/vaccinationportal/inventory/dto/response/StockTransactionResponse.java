package com.se2030.vaccinationportal.inventory.dto.response;

import com.se2030.vaccinationportal.inventory.model.StockTransactionType;
import java.time.Instant;

public record StockTransactionResponse(Long id, Long inventoryId, Long stockTransferId, StockTransactionType type,
        int quantityChange, int beforeQuantity, int afterQuantity, Long actorId,
        String reason, Instant createdAt) {}
