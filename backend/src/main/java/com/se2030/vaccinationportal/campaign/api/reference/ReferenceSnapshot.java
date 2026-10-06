package com.se2030.vaccinationportal.campaign.api.reference;

/** Immutable reference display facts; no campaign entity crosses the boundary. */
public record ReferenceSnapshot(Long id, String code, String name) {
}
