package com.se2030.vaccinationportal.campaign.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Module: MF-02 — Vaccination Campaign, Center and Session Management
 * Use Case: UC-10B — Assign Staff / Resources and Approve Capacity
 * Responsibility: Session staff/resource assignment and capacity planning
 */
public record CreateSessionCapacityPlanRequest(
        @Min(value = 0, message = "Fully equipped station count must be non-negative")
        int fullyEquippedStationCount,

        @NotNull(message = "Approved capacity is required")
        @Min(value = 0, message = "Approved capacity must be non-negative")
        Integer approvedCapacity,

        @Min(value = 1, message = "Safe concurrent occupancy must be positive if specified")
        Integer safeConcurrentOccupancy,

        @Min(value = 1, message = "Expected dwell minutes must be positive if specified")
        Integer expectedDwellMinutes,

        @DecimalMin(value = "0.0", message = "Expected accompanying ratio must be at least 0.0")
        @DecimalMax(value = "1.0", message = "Expected accompanying ratio must not exceed 1.0")
        Double expectedAccompanyingPersonsPerBooking,

        @Size(max = 255, message = "Override reason must not exceed 255 characters")
        String overrideReason,

        @Size(max = 64, message = "Approved by actor ID must not exceed 64 characters")
        String approvedBy
) {}
