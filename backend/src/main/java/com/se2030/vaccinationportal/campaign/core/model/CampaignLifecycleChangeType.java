package com.se2030.vaccinationportal.campaign.core.model;

/**
 * Audited lifecycle changes applied to a vaccination campaign.
 */
public enum CampaignLifecycleChangeType {
    SUSPENDED,
    RESUMED,
    COMPLETED,
    CANCELLED,
    ARCHIVED
}
