package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignDoseProgramme;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignDoseProgrammeRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignDoseProgrammeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying CampaignDoseProgramme persistence, repository queries,
 * unique version constraints, and service-layer effective version management (MF02-BR-02 / MF02-BR-18).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignDoseProgrammePersistenceIntegrationTests {

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private CampaignDoseProgrammeRepository doseProgrammeRepository;

    @Autowired
    private CampaignDoseProgrammeService doseProgrammeService;

    private VaccinationCampaign testCampaign;

    @BeforeEach
    void setUp() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-COVID-PROG", "COVID-19 mRNA Programme", "dose", true)
        );
        TargetGroup tg = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-ADULT-PROG", "Adults 18-64 Programme", true)
        );

        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-PROG-2026",
                "National Autumn Dose Programme Campaign",
                "Dose programme testing campaign",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 3, 31),
                LocalDateTime.of(2026, 8, 15, 8, 0),
                LocalDateTime.of(2027, 3, 30, 18, 0),
                CampaignStatus.SCHEDULED
        );
        testCampaign = campaignRepository.saveAndFlush(campaign);
    }

    @Test
    @DisplayName("Successfully persist and retrieve CampaignDoseProgramme")
    void persistAndRetrieveCampaignDoseProgramme() {
        CampaignDoseProgramme programme = new CampaignDoseProgramme(
                testCampaign,
                1,
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                15,
                LocalDate.of(2026, 9, 1),
                null
        );

        CampaignDoseProgramme saved = doseProgrammeRepository.saveAndFlush(programme);

        assertNotNull(saved.getId());
        assertEquals(testCampaign.getId(), saved.getCampaign().getId());
        assertEquals(1, saved.getVersionNumber());
        assertEquals(DoseCategory.PRIMARY, saved.getDoseCategory());
        assertEquals("1,2", saved.getPermittedDoseNumbers());
        assertEquals(21, saved.getMinimumIntervalDays());
        assertFalse(saved.isPreviousEvidenceRequired());
        assertEquals(15, saved.getExpectedServiceMinutes());
        assertEquals(LocalDate.of(2026, 9, 1), saved.getActiveFrom());
        assertNull(saved.getActiveTo());
        assertNotNull(saved.getCreatedAt());

        Optional<CampaignDoseProgramme> byVersion = doseProgrammeRepository.findByCampaignIdAndVersionNumber(
                testCampaign.getId(), 1
        );
        assertTrue(byVersion.isPresent());
        assertEquals(saved.getId(), byVersion.get().getId());
        assertTrue(doseProgrammeRepository.existsByCampaignIdAndVersionNumber(testCampaign.getId(), 1));
    }

    @Test
    @DisplayName("Duplicate campaign and version number violates unique constraint")
    void duplicateCampaignAndVersionNumberViolatesUniqueConstraint() {
        CampaignDoseProgramme p1 = new CampaignDoseProgramme(
                testCampaign,
                1,
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31)
        );
        doseProgrammeRepository.saveAndFlush(p1);

        CampaignDoseProgramme p2 = new CampaignDoseProgramme(
                testCampaign,
                1, // Duplicate version for the same campaign
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                10,
                LocalDate.of(2026, 11, 1),
                null
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            doseProgrammeRepository.saveAndFlush(p2);
        });
    }

    @Test
    @DisplayName("Dose programme history is retained across multiple versions (MF02-BR-18)")
    void programmeHistoryRetainedAcrossVersions() {
        CampaignDoseProgramme v1 = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1)
        );

        // Version 2 published on Nov 1 (closes version 1 on Oct 31)
        CampaignDoseProgramme v2 = doseProgrammeService.createNewVersion(
                testCampaign.getId(),
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                15,
                LocalDate.of(2026, 11, 1)
        );

        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(testCampaign.getId());
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getVersionNumber(), "Ordered descending by version");
        assertEquals(1, history.get(1).getVersionNumber());

        // Verify version 1 was closed without in-place loss of past configuration
        CampaignDoseProgramme refreshedV1 = doseProgrammeRepository.findById(v1.getId()).orElseThrow();
        assertEquals(LocalDate.of(2026, 10, 31), refreshedV1.getActiveTo());
        assertEquals(DoseCategory.PRIMARY, refreshedV1.getDoseCategory());
        assertEquals("1,2", refreshedV1.getPermittedDoseNumbers());

        // Verify version 2 is open-ended
        assertNull(v2.getActiveTo());
        assertEquals(2, v2.getVersionNumber());
        assertEquals(DoseCategory.BOOSTER, v2.getDoseCategory());
    }

    @Test
    @DisplayName("Effective version query returns correct version per date (MF02-BR-02)")
    void effectiveVersionQueryResolvesCorrectly() {
        doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1)
        );

        doseProgrammeService.createNewVersion(
                testCampaign.getId(),
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                15,
                LocalDate.of(2026, 11, 1)
        );

        // Date before version 1 start
        Optional<CampaignDoseProgramme> beforeStart = doseProgrammeService.getEffectiveProgramme(
                testCampaign.getId(), LocalDate.of(2026, 8, 31)
        );
        assertTrue(beforeStart.isEmpty());

        // Date during version 1 (October 15)
        Optional<CampaignDoseProgramme> duringV1 = doseProgrammeService.getEffectiveProgramme(
                testCampaign.getId(), LocalDate.of(2026, 10, 15)
        );
        assertTrue(duringV1.isPresent());
        assertEquals(1, duringV1.get().getVersionNumber());
        assertEquals(DoseCategory.PRIMARY, duringV1.get().getDoseCategory());

        // Date during version 2 (November 15)
        Optional<CampaignDoseProgramme> duringV2 = doseProgrammeService.getEffectiveProgramme(
                testCampaign.getId(), LocalDate.of(2026, 11, 15)
        );
        assertTrue(duringV2.isPresent());
        assertEquals(2, duringV2.get().getVersionNumber());
        assertEquals(DoseCategory.BOOSTER, duringV2.get().getDoseCategory());
    }

    @Test
    @DisplayName("Overlapping effective programme periods are rejected (MF02-BR-02)")
    void overlappingEffectivePeriodsRejected() {
        // Initial version 1 active from Sept 1 (open-ended)
        doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1)
        );

        // Trying to create an overlapping initial programme fails
        assertThrows(IllegalStateException.class, () -> {
            doseProgrammeService.createInitialProgramme(
                    testCampaign.getId(),
                    DoseCategory.BOOSTER,
                    "3",
                    90,
                    true,
                    10,
                    LocalDate.of(2026, 9, 15)
            );
        });
    }

    @Test
    @DisplayName("Only one programme is effective on any specific evaluation date (MF02-BR-02)")
    void onlyOneProgrammeIsEffectivePerDate() {
        doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1)
        );

        doseProgrammeService.createNewVersion(
                testCampaign.getId(),
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                15,
                LocalDate.of(2026, 11, 1)
        );

        LocalDate boundaryDayV1 = LocalDate.of(2026, 10, 31);
        Optional<CampaignDoseProgramme> pV1 = doseProgrammeService.getEffectiveProgramme(testCampaign.getId(), boundaryDayV1);
        assertTrue(pV1.isPresent());
        assertEquals(1, pV1.get().getVersionNumber());

        LocalDate boundaryDayV2 = LocalDate.of(2026, 11, 1);
        Optional<CampaignDoseProgramme> pV2 = doseProgrammeService.getEffectiveProgramme(testCampaign.getId(), boundaryDayV2);
        assertTrue(pV2.isPresent());
        assertEquals(2, pV2.get().getVersionNumber());
    }

    @Test
    @DisplayName("New version effective date on or before current version start date is rejected")
    void newVersionEffectiveDateOnOrBeforeCurrentStartIsRejected() {
        CampaignDoseProgramme v1 = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1)
        );

        // Same date as current version start
        IllegalArgumentException exSame = assertThrows(IllegalArgumentException.class, () -> {
            doseProgrammeService.createNewVersion(
                    testCampaign.getId(),
                    DoseCategory.BOOSTER,
                    "3",
                    90,
                    true,
                    10,
                    LocalDate.of(2026, 9, 1)
            );
        });
        assertTrue(exSame.getMessage().contains("must be strictly after the previous version start date"));

        // Date before current version start
        IllegalArgumentException exBefore = assertThrows(IllegalArgumentException.class, () -> {
            doseProgrammeService.createNewVersion(
                    testCampaign.getId(),
                    DoseCategory.BOOSTER,
                    "3",
                    90,
                    true,
                    10,
                    LocalDate.of(2026, 8, 15)
            );
        });
        assertTrue(exBefore.getMessage().contains("must be strictly after the previous version start date"));

        // Verify version 1 activeTo remains null (unmodified)
        CampaignDoseProgramme refreshedV1 = doseProgrammeRepository.findById(v1.getId()).orElseThrow();
        assertNull(refreshedV1.getActiveTo());
    }
}
