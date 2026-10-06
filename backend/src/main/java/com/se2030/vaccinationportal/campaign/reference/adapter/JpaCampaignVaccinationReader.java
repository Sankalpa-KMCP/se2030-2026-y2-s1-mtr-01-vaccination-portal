package com.se2030.vaccinationportal.campaign.reference.adapter;

import com.se2030.vaccinationportal.campaign.api.vaccination.CampaignVaccinationReader;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Plain reads; transaction and lock ownership remain with the existing caller. */
@Component
public class JpaCampaignVaccinationReader implements CampaignVaccinationReader {
    private final TimeSlotRepository slots;
    private final VaccinationCampaignRepository campaigns;
    private final VaccinationCenterRepository centers;
    private final VaccineTypeRepository vaccineTypes;

    public JpaCampaignVaccinationReader(TimeSlotRepository slots, VaccinationCampaignRepository campaigns,
                                        VaccinationCenterRepository centers, VaccineTypeRepository vaccineTypes) {
        this.slots = slots;
        this.campaigns = campaigns;
        this.centers = centers;
        this.vaccineTypes = vaccineTypes;
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

    @Override
    public Optional<String> findCampaignNameById(Long campaignId) {
        return campaigns.findById(campaignId).map(VaccinationCampaign::getName);
    }

    @Override
    public Optional<String> findCenterNameById(Long centerId) {
        return centers.findById(centerId).map(VaccinationCenter::getName);
    }

    @Override
    public Optional<String> findVaccineTypeNameById(Long vaccineTypeId) {
        return vaccineTypes.findById(vaccineTypeId).map(VaccineType::getName);
    }
}
