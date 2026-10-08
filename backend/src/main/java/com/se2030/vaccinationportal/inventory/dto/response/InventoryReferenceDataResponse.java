package com.se2030.vaccinationportal.inventory.dto.response;

import java.util.List;

public record InventoryReferenceDataResponse(List<ReferenceItem> vaccineTypes, List<ReferenceItem> centers) {
    public record ReferenceItem(Long id, String code, String name) {}
}
