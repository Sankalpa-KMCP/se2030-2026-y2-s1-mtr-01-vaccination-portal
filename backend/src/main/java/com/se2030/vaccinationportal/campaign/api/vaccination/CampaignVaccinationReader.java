package com.se2030.vaccinationportal.campaign.api.vaccination;

import java.util.Optional;

/** Authoritative campaign facts needed to validate vaccination administration. */
public interface CampaignVaccinationReader {
    Optional<Long> findCenterIdBySlotId(Long slotId);

    Optional<Long> findVaccineTypeIdByCampaignId(Long campaignId);

    Optional<String> findCampaignNameById(Long campaignId);

    Optional<String> findCenterNameById(Long centerId);

    Optional<String> findVaccineTypeNameById(Long vaccineTypeId);
}
