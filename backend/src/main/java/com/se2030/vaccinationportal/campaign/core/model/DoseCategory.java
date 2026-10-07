package com.se2030.vaccinationportal.campaign.core.model;

/**
 * Canonical dose category for campaign dose programmes.
 * Defined by canonical specification WBVP-SE2030 section 6.1 / 12.
 */
public enum DoseCategory {
    /**
     * Initial primary vaccination series (e.g. Dose 1, Dose 2).
     */
    PRIMARY,

    /**
     * Subsequent booster vaccination doses (e.g. Dose 3, Autumn Booster).
     */
    BOOSTER
}
