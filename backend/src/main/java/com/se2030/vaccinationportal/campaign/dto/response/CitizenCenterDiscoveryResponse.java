package com.se2030.vaccinationportal.campaign.dto.response;

/**
 * Projection for authenticated citizen vaccination center discovery.
 * Provides Center ID, Centre Code, and display information extracted from Vaccination Center Operations.
 */
public record CitizenCenterDiscoveryResponse(
        Long id,
        String code,
        String name,
        String address,
        String contact,
        String timezone
) {
}
