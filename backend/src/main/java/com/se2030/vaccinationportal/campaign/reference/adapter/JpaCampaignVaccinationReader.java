package com.se2030.vaccinationportal.campaign.reference.adapter;

import com.se2030.vaccinationportal.campaign.api.vaccination.CampaignVaccinationReader;
import com.se2030.vaccinationportal.campaign.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Plain reads; transaction and lock ownership remain with the existing caller. */
@Component
public class JpaCampaignVaccinationReader implements CampaignVaccinationReader {
    private final TimeSlotRepository slots;
    private final VaccinationCampaignRepository campaigns;

    public JpaCampaignVaccinationReader(TimeSlotRepository slots, VaccinationCampaignRepository campaigns) {
        this.slots = slots;
        this.campaigns = campaigns;
    }

    @Override
    public Optional<Long> findCenterIdBySlotId(Long slotId) {
        return slots.findById(slotId).map(slot -> slot.getSession() != null && slot.getSession().getCenter() != null
                ? slot.getSession().getCenter().getId() : null);
    }

    @Override
    public Optional<Long> findVaccineTypeIdByCampaignId(Long campaignId) {
        return campaigns.findById(campaignId).map(campaign -> campaign.getVaccineType() == null
                ? null : campaign.getVaccineType().getId());
    }
}
