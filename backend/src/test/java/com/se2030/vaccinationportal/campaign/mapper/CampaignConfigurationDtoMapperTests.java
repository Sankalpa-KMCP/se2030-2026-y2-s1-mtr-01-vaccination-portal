package com.se2030.vaccinationportal.campaign.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.se2030.vaccinationportal.campaign.dto.response.CampaignCenterAssignmentResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignDoseProgrammeResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignLocationTargetResponse;
import com.se2030.vaccinationportal.campaign.dto.response.CampaignQuestionnaireAssignmentResponse;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignLocationTarget;
import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.LocationTargetType;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CampaignConfigurationDtoMapperTests {

    private VaccinationCampaign campaign;
    private VaccinationCenter center;

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = new VaccineType("COVID-19", "mRNA");
        TargetGroup targetGroup = new TargetGroup("ELDERLY", "60+");
        campaign = new VaccinationCampaign(
                "National COVID Campaign",
                "COVID-2026",
                "Description",
                vaccineType,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-15T00:00:00Z"),
                Instant.parse("2026-12-30T23:59:59Z")
        );
        ReflectionTestUtils.setField(campaign, "id", 100L);

        center = new VaccinationCenter("CTR-001", "Main Center", "123 Main St", "contact@test.com", CenterStatus.ACTIVE, 50, 100, "Asia/Colombo");
        ReflectionTestUtils.setField(center, "id", 200L);
    }

    @Test
    @DisplayName("Maps CampaignDoseProgramme entity to Response DTO correctly")
    void toResponse_doseProgramme_mapsFields() {
        CampaignDoseProgramme programme = new CampaignDoseProgramme(
                campaign,
                1,
                DoseCategory.PRIMARY,
                "1,2",
                28,
                false,
                15,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1)
        );
        ReflectionTestUtils.setField(programme, "id", 10L);

        CampaignDoseProgrammeResponse response = CampaignConfigurationDtoMapper.toResponse(programme);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.campaignId()).isEqualTo(100L);
        assertThat(response.versionNumber()).isEqualTo(1);
        assertThat(response.doseCategory()).isEqualTo(DoseCategory.PRIMARY);
        assertThat(response.permittedDoseNumbers()).isEqualTo("1,2");
        assertThat(response.minimumIntervalDays()).isEqualTo(28);
        assertThat(response.previousEvidenceRequired()).isFalse();
        assertThat(response.expectedServiceMinutes()).isEqualTo(15);
        assertThat(response.activeFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.activeTo()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    @DisplayName("Maps null CampaignDoseProgramme to null")
    void toResponse_nullDoseProgramme_returnsNull() {
        assertThat(CampaignConfigurationDtoMapper.toResponse((CampaignDoseProgramme) null)).isNull();
    }

    @Test
    @DisplayName("Maps CampaignQuestionnaireAssignment entity to Response DTO correctly")
    void toResponse_questionnaireAssignment_mapsFields() {
        CampaignQuestionnaireAssignment assignment = new CampaignQuestionnaireAssignment(
                campaign,
                55L,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 11, 1)
        );
        ReflectionTestUtils.setField(assignment, "id", 20L);

        CampaignQuestionnaireAssignmentResponse response = CampaignConfigurationDtoMapper.toResponse(assignment);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.campaignId()).isEqualTo(100L);
        assertThat(response.questionnaireVersionId()).isEqualTo(55L);
        assertThat(response.activeFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.activeTo()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    @DisplayName("Maps null CampaignQuestionnaireAssignment to null")
    void toResponse_nullQuestionnaireAssignment_returnsNull() {
        assertThat(CampaignConfigurationDtoMapper.toResponse((CampaignQuestionnaireAssignment) null)).isNull();
    }

    @Test
    @DisplayName("Maps CampaignCenterAssignment entity to Response DTO correctly")
    void toResponse_centerAssignment_mapsFields() {
        CampaignCenterAssignment assignment = new CampaignCenterAssignment(
                campaign,
                center,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31)
        );
        ReflectionTestUtils.setField(assignment, "id", 30L);

        CampaignCenterAssignmentResponse response = CampaignConfigurationDtoMapper.toResponse(assignment);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.campaignId()).isEqualTo(100L);
        assertThat(response.centerId()).isEqualTo(200L);
        assertThat(response.centerCode()).isEqualTo("CTR-001");
        assertThat(response.centerName()).isEqualTo("Main Center");
        assertThat(response.status()).isEqualTo(CampaignCenterAssignmentStatus.ACTIVE);
        assertThat(response.activeFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.activeTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Maps null CampaignCenterAssignment to null")
    void toResponse_nullCenterAssignment_returnsNull() {
        assertThat(CampaignConfigurationDtoMapper.toResponse((CampaignCenterAssignment) null)).isNull();
    }

    @Test
    @DisplayName("Maps CampaignLocationTarget entity to Response DTO correctly")
    void toResponse_locationTarget_mapsFields() {
        CampaignLocationTarget target = new CampaignLocationTarget(
                campaign,
                LocationTargetType.DISTRICT,
                "COLOMBO"
        );
        ReflectionTestUtils.setField(target, "id", 40L);

        CampaignLocationTargetResponse response = CampaignConfigurationDtoMapper.toResponse(target);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.campaignId()).isEqualTo(100L);
        assertThat(response.targetType()).isEqualTo(LocationTargetType.DISTRICT);
        assertThat(response.targetCode()).isEqualTo("COLOMBO");
    }

    @Test
    @DisplayName("Maps null CampaignLocationTarget to null")
    void toResponse_nullLocationTarget_returnsNull() {
        assertThat(CampaignConfigurationDtoMapper.toResponse((CampaignLocationTarget) null)).isNull();
    }
}
