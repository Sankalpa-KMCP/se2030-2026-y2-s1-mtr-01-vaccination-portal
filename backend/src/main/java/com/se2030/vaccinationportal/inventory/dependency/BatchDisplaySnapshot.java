package com.se2030.vaccinationportal.inventory.dependency;

/** Batch identity required for vaccination record and certificate display. */
public record BatchDisplaySnapshot(String batchNumber, Long vaccineTypeId) {
}
