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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying CampaignDoseProgramme persistence, repository queries,
 * unique version constraints, and service-layer effective version management (MF02-BR-02 / MF02-BR-18).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(CampaignDoseProgrammePersistenceIntegrationTests.TestClockConfig.class)
@Transactional
class CampaignDoseProgrammePersistenceIntegrationTests {

    public static final LocalDate FIXED_EVALUATION_DATE = LocalDate.of(2026, 8, 9);

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock testClock() {
            return Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);
        }
    }

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

        VaccinationCampaign campaign = CampaignTestFixtures.campaign(
                "CMP-PROG-2026",
                "National Autumn Dose Programme Campaign",
                "Dose programme testing campaign",
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 3, 31),
                Instant.parse("2026-08-15T08:00:00Z"),
                Instant.parse("2027-03-30T18:00:00Z"),
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

        Optional<CampaignDoseProgramme> latest = doseProgrammeRepository
                .findTopByCampaignIdOrderByVersionNumberDesc(testCampaign.getId());
        assertEquals(saved.getId(), latest.orElseThrow().getId());
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

        // Version 2 begins on Nov 1, the exclusive end boundary of version 1.
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
        assertEquals(LocalDate.of(2026, 11, 1), refreshedV1.getActiveTo());
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

    @Test
    @DisplayName("createInitialProgramme with null activeFrom throws IllegalArgumentException and persists nothing")
    void createInitialProgrammeWithNullActiveFromThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            doseProgrammeService.createInitialProgramme(
                    testCampaign.getId(),
                    DoseCategory.PRIMARY,
                    "1,2",
                    21,
                    false,
                    10,
                    null
            );
        });

        assertTrue(ex.getMessage().contains("Active from date is required"));
        assertEquals(0, doseProgrammeRepository.findByCampaignIdOrderByVersionNumberDesc(testCampaign.getId()).size());
    }

    @Test
    @DisplayName("createNewVersion with null effectiveStartDate throws IllegalArgumentException and preserves existing programme")
    void createNewVersionWithNullEffectiveStartDateThrowsException() {
        CampaignDoseProgramme v1 = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 9, 1)
        );
        assertNull(v1.getActiveTo());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            doseProgrammeService.createNewVersion(
                    testCampaign.getId(),
                    DoseCategory.BOOSTER,
                    "3",
                    90,
                    true,
                    10,
                    null
            );
        });

        assertTrue(ex.getMessage().contains("Effective start date is required"));

        // Verify existing programme remains open-ended (activeTo is null)
        CampaignDoseProgramme refreshedV1 = doseProgrammeRepository.findById(v1.getId()).orElseThrow();
        assertNull(refreshedV1.getActiveTo(), "Existing programme must remain open-ended when new version creation fails");

        // Verify no additional version was persisted
        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(testCampaign.getId());
        assertEquals(1, history.size());
        assertEquals(1, history.get(0).getVersionNumber());
    }

    @Test
    @DisplayName("Invalid replacement candidate is rejected before current history is closed")
    void invalidReplacementCandidatePreservesCurrentHistory() {
        CampaignDoseProgramme current = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(), DoseCategory.PRIMARY, "1,2", 21, false, 10,
                LocalDate.of(2026, 7, 1)
        );

        assertThrows(IllegalArgumentException.class, () -> doseProgrammeService.createNewVersion(
                testCampaign.getId(), DoseCategory.BOOSTER, "not-a-dose", 90, true, 15,
                LocalDate.of(2026, 9, 1)
        ));

        assertNull(doseProgrammeRepository.findById(current.getId()).orElseThrow().getActiveTo());
        assertEquals(1, doseProgrammeService.getProgrammeHistory(testCampaign.getId()).size());
    }

    @Test
    @DisplayName("createNewVersion with past effectiveStartDate is rejected and preserves history (MF02-BR-18)")
    void createNewVersionWithPastEffectiveStartDateIsRejected() {
        // Initial programme active from 2026-07-01 (before fixed today 2026-08-09)
        CampaignDoseProgramme v1 = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 7, 1)
        );
        assertNull(v1.getActiveTo());

        // Attempt replacement version with effectiveStartDate = 2026-08-01 (after activeFrom, but in the past relative to 2026-08-09)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            doseProgrammeService.createNewVersion(
                    testCampaign.getId(),
                    DoseCategory.BOOSTER,
                    "3",
                    90,
                    true,
                    15,
                    LocalDate.of(2026, 8, 1)
            );
        });

        assertTrue(ex.getMessage().contains("cannot be in the past") || ex.getMessage().contains("MF02-BR-18"));

        // Verify original programme remains open-ended (activeTo is null)
        CampaignDoseProgramme refreshedV1 = doseProgrammeRepository.findById(v1.getId()).orElseThrow();
        assertNull(refreshedV1.getActiveTo(), "Existing programme must remain open-ended when past version replacement is rejected");

        // Verify no second version was persisted
        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(testCampaign.getId());
        assertEquals(1, history.size());
        assertEquals(1, history.get(0).getVersionNumber());
    }

    @Test
    @DisplayName("createNewVersion today uses the same half-open transition boundary (MF02-BR-18)")
    void createNewVersionEffectiveTodaySucceeds() {
        // Initial programme active from 2026-07-01
        CampaignDoseProgramme v1 = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 7, 1)
        );

        // Replacement version effective today (2026-08-09)
        CampaignDoseProgramme v2 = doseProgrammeService.createNewVersion(
                testCampaign.getId(),
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                15,
                FIXED_EVALUATION_DATE
        );

        assertNotNull(v2.getId());
        assertEquals(2, v2.getVersionNumber());
        assertEquals(FIXED_EVALUATION_DATE, v2.getActiveFrom());
        assertNull(v2.getActiveTo());

        // Previous version ends exclusively when the replacement begins.
        CampaignDoseProgramme refreshedV1 = doseProgrammeRepository.findById(v1.getId()).orElseThrow();
        assertEquals(FIXED_EVALUATION_DATE, refreshedV1.getActiveTo());

        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(testCampaign.getId());
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("createNewVersion in future uses the same half-open transition boundary (MF02-BR-18)")
    void createNewVersionEffectiveFutureSucceeds() {
        // Initial programme active from 2026-07-01
        CampaignDoseProgramme v1 = doseProgrammeService.createInitialProgramme(
                testCampaign.getId(),
                DoseCategory.PRIMARY,
                "1,2",
                21,
                false,
                10,
                LocalDate.of(2026, 7, 1)
        );

        LocalDate futureDate = LocalDate.of(2026, 8, 20);

        // Replacement version effective in future (2026-08-20)
        CampaignDoseProgramme v2 = doseProgrammeService.createNewVersion(
                testCampaign.getId(),
                DoseCategory.BOOSTER,
                "3",
                90,
                true,
                15,
                futureDate
        );

        assertNotNull(v2.getId());
        assertEquals(2, v2.getVersionNumber());
        assertEquals(futureDate, v2.getActiveFrom());
        assertNull(v2.getActiveTo());

        // Previous version ends exclusively when the replacement begins.
        CampaignDoseProgramme refreshedV1 = doseProgrammeRepository.findById(v1.getId()).orElseThrow();
        assertEquals(futureDate, refreshedV1.getActiveTo());

        List<CampaignDoseProgramme> history = doseProgrammeService.getProgrammeHistory(testCampaign.getId());
        assertEquals(2, history.size());
    }
}
