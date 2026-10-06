package com.se2030.vaccinationportal.campaign.reference.adapter;

import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaCampaignVaccinationReaderTests {
    private final VaccinationCampaignRepository campaigns = mock(VaccinationCampaignRepository.class);
    private final VaccinationCenterRepository centers = mock(VaccinationCenterRepository.class);
    private final VaccineTypeRepository vaccineTypes = mock(VaccineTypeRepository.class);
    private final JpaCampaignVaccinationReader reader = new JpaCampaignVaccinationReader(
            mock(TimeSlotRepository.class), campaigns, centers, vaccineTypes);

    @Test
    void returnsNamesWithoutExposingEntities() {
        VaccinationCampaign campaign = mock(VaccinationCampaign.class);
        VaccinationCenter center = mock(VaccinationCenter.class);
        VaccineType type = mock(VaccineType.class);
        when(campaign.getName()).thenReturn("Autumn campaign");
        when(center.getName()).thenReturn("Central clinic");
        when(type.getName()).thenReturn("Vaccine A");
        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(centers.findById(2L)).thenReturn(Optional.of(center));
        when(vaccineTypes.findById(3L)).thenReturn(Optional.of(type));

        assertThat(reader.findCampaignNameById(1L)).contains("Autumn campaign");
        assertThat(reader.findCenterNameById(2L)).contains("Central clinic");
        assertThat(reader.findVaccineTypeNameById(3L)).contains("Vaccine A");
    }

    @Test
    void missingNamesAreEmptyForCallerFallbacks() {
        assertThat(reader.findCampaignNameById(1L)).isEmpty();
        assertThat(reader.findCenterNameById(2L)).isEmpty();
        assertThat(reader.findVaccineTypeNameById(3L)).isEmpty();
    }
}
