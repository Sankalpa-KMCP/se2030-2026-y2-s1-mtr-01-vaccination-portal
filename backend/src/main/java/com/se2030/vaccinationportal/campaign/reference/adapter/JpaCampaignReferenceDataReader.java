package com.se2030.vaccinationportal.campaign.reference.adapter;

import com.se2030.vaccinationportal.campaign.api.reference.CampaignReferenceDataReader;
import com.se2030.vaccinationportal.campaign.api.reference.ReferenceSnapshot;


import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adapts center and vaccine catalogs into reference data for other modules. */
@Component
public class JpaCampaignReferenceDataReader implements CampaignReferenceDataReader {
    private final VaccinationCenterRepository centers;
    private final VaccineTypeRepository vaccineTypes;

    public JpaCampaignReferenceDataReader(VaccinationCenterRepository centers, VaccineTypeRepository vaccineTypes) {
        this.centers = centers;
        this.vaccineTypes = vaccineTypes;
    }

    @Override
    public boolean centerExists(Long centerId) {
        return centers.existsById(centerId);
    }

    @Override
    public boolean vaccineTypeExists(Long vaccineTypeId) {
        return vaccineTypes.existsById(vaccineTypeId);
    }

    @Override
    public List<ReferenceSnapshot> centers() {
        return centers.findAll().stream()
                .map(center -> new ReferenceSnapshot(center.getId(), center.getCode(), center.getName()))
                .toList();
    }

    @Override
    public List<ReferenceSnapshot> vaccineTypes() {
        return vaccineTypes.findAll().stream()
                .map(type -> new ReferenceSnapshot(type.getId(), type.getCode(), type.getName()))
                .toList();
    }
}
