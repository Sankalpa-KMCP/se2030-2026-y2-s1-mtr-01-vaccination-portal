package com.se2030.vaccinationportal.inventory.dto.response;

public record CenterInventoryResponse(Long id, Long centerId, Long batchId, int quantity, Long lockVersion) {}
