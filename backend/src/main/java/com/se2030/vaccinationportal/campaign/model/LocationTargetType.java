package com.se2030.vaccinationportal.campaign.model;

/**
 * Canonical administrative location target types for campaign service-area targeting.
 * Defined by canonical specification WBVP-SE2030 section 6.1 / 12 (Table 12.1).
 * Controls citizen-facing campaign visibility and access, NOT medical eligibility.
 */
public enum LocationTargetType {
    /**
     * Administrative province (e.g. "WP" for Western Province, "CP" for Central Province).
     */
    PROVINCE,

    /**
     * Administrative district (e.g. "COLOMBO", "GAMPAHA", "KANDY").
     */
    DISTRICT,

    /**
     * Regional Health Directorate / Division (e.g. "HR-NORTH", "HR-WEST").
     */
    HEALTH_REGION,

    /**
     * Specific vaccination center location code (e.g. "CTR-NCMC", "CTR-001").
     * Note: Operational center session assignment is separate (CampaignCenterAssignment).
     */
    CENTER
}
