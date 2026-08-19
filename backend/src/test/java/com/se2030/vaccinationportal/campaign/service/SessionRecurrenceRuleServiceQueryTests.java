package com.se2030.vaccinationportal.campaign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.SessionRecurrenceRuleRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SessionRecurrenceRuleServiceQueryTests.FixedClockConfig.class)
public class SessionRecurrenceRuleServiceQueryTests {

    private static final Instant NOW = Instant.parse("2026-09-15T23:30:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private SessionRecurrenceRuleService ruleService;

    @Autowired
    private SessionRecurrenceRuleRepository ruleRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    private VaccinationCampaign campaignA;
    private VaccinationCampaign campaignB;
    private VaccinationCenter center1;
    private VaccinationCenter center2;

    private SessionRecurrenceRule rule1; // campaignA, center1, ACTIVE, start 2026-09-01, 08:30
    private SessionRecurrenceRule rule2; // campaignA, center1, ACTIVE, start 2026-09-01, 13:00
    private SessionRecurrenceRule rule3; // campaignA, center2, CANCELLED, start 2026-09-15, 09:00
    private SessionRecurrenceRule rule4; // campaignB, center1, ACTIVE, start 2026-10-01, 08:00

    @BeforeEach
    void setUp() {
        VaccineType vaccineType = vaccineTypeRepository.save(new VaccineType("VAX-QUERY-01", "Query Vaccine"));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-QUERY-01", "Query Group"));

        campaignA = campaignRepository.save(new VaccinationCampaign(
                "CMP-QUERY-01", "Campaign Alpha", "Description", vaccineType, targetGroup,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-07-15T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z")
        ));

        campaignB = campaignRepository.save(new VaccinationCampaign(
                "CMP-QUERY-02", "Campaign Beta", "Description", vaccineType, targetGroup,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-07-15T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z")
        ));

        center1 = centerRepository.save(new VaccinationCenter(
                "CTR-Q-01", "Center 1", "Address 1", "+94111111111",
                CenterStatus.ACTIVE, 100, 30, "Asia/Colombo"
        ));

        center2 = centerRepository.save(new VaccinationCenter(
                "CTR-Q-02", "Center 2", "Address 2", "+94222222222",
                CenterStatus.ACTIVE, 100, 30, "Asia/Colombo"
        ));

        rule1 = ruleRepository.save(new SessionRecurrenceRule(
                campaignA, center1, EnumSet.of(DayOfWeek.MONDAY),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31),
                LocalTime.of(8, 30), LocalTime.of(12, 30), "Asia/Colombo"
        ));

        rule2 = ruleRepository.save(new SessionRecurrenceRule(
                campaignA, center1, EnumSet.of(DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31),
                LocalTime.of(13, 0), LocalTime.of(17, 0), "Asia/Colombo"
        ));

        rule3 = ruleRepository.save(new SessionRecurrenceRule(
                campaignA, center2, EnumSet.of(DayOfWeek.FRIDAY),
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 31),
                LocalTime.of(9, 0), LocalTime.of(15, 0), "Asia/Colombo"
        ));
        rule3.cancel();
        rule3 = ruleRepository.save(rule3);

        rule4 = ruleRepository.save(new SessionRecurrenceRule(
                campaignB, center1, EnumSet.of(DayOfWeek.TUESDAY),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30),
                LocalTime.of(8, 0), LocalTime.of(16, 0), "Asia/Colombo"
        ));
    }

    @Test
    @DisplayName("getRequiredRuleById returns rule when found")
    void testGetRequiredRuleByIdSuccess() {
        SessionRecurrenceRule found = ruleService.getRequiredRuleById(rule1.getId());
        assertNotNull(found);
        assertEquals(rule1.getId(), found.getId());
        assertEquals("CMP-QUERY-01", found.getCampaign().getCode());
    }

    @Test
    @DisplayName("getRequiredRuleById throws ResourceNotFoundException when not found")
    void testGetRequiredRuleByIdNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> ruleService.getRequiredRuleById(999999L));
    }

    @Test
    @DisplayName("getRules with no filters returns all rules in deterministic order")
    void testGetRulesNoFilters() {
        List<SessionRecurrenceRule> results = ruleService.getRules(null, null, null);

        assertEquals(4, results.size());
        assertEquals(rule1.getId(), results.get(0).getId());
        assertEquals(rule2.getId(), results.get(1).getId());
        assertEquals(rule3.getId(), results.get(2).getId());
        assertEquals(rule4.getId(), results.get(3).getId());
    }

    @Test
    @DisplayName("getRules filtered by campaignId only")
    void testGetRulesByCampaignOnly() {
        List<SessionRecurrenceRule> results = ruleService.getRules(campaignA.getId(), null, null);

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(r -> r.getCampaign().getId().equals(campaignA.getId())));
    }

    @Test
    @DisplayName("getRules filtered by centerId only")
    void testGetRulesByCenterOnly() {
        List<SessionRecurrenceRule> results = ruleService.getRules(null, center1.getId(), null);

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(r -> r.getCenter().getId().equals(center1.getId())));
    }

    @Test
    @DisplayName("getRules filtered by status only")
    void testGetRulesByStatusOnly() {
        List<SessionRecurrenceRule> activeResults = ruleService.getRules(null, null, SessionRecurrenceRuleStatus.ACTIVE);
        assertEquals(3, activeResults.size());

        List<SessionRecurrenceRule> cancelledResults = ruleService.getRules(null, null, SessionRecurrenceRuleStatus.CANCELLED);
        assertEquals(1, cancelledResults.size());
        assertEquals(rule3.getId(), cancelledResults.get(0).getId());
    }

    @Test
    @DisplayName("getRules filtered by campaignId and centerId")
    void testGetRulesByCampaignAndCenter() {
        List<SessionRecurrenceRule> results = ruleService.getRules(campaignA.getId(), center1.getId(), null);

        assertEquals(2, results.size());
        assertEquals(rule1.getId(), results.get(0).getId());
        assertEquals(rule2.getId(), results.get(1).getId());
    }

    @Test
    @DisplayName("getRules filtered by campaignId and status")
    void testGetRulesByCampaignAndStatus() {
        List<SessionRecurrenceRule> results = ruleService.getRules(campaignA.getId(), null, SessionRecurrenceRuleStatus.CANCELLED);

        assertEquals(1, results.size());
        assertEquals(rule3.getId(), results.get(0).getId());
    }

    @Test
    @DisplayName("getRules filtered by centerId and status")
    void testGetRulesByCenterAndStatus() {
        List<SessionRecurrenceRule> results = ruleService.getRules(null, center2.getId(), SessionRecurrenceRuleStatus.CANCELLED);

        assertEquals(1, results.size());
        assertEquals(rule3.getId(), results.get(0).getId());
    }

    @Test
    @DisplayName("getRules filtered by all three parameters")
    void testGetRulesByAllFilters() {
        List<SessionRecurrenceRule> results = ruleService.getRules(
                campaignA.getId(), center1.getId(), SessionRecurrenceRuleStatus.ACTIVE);

        assertEquals(2, results.size());
        assertEquals(rule1.getId(), results.get(0).getId());
        assertEquals(rule2.getId(), results.get(1).getId());
    }

    @Test
    @DisplayName("getRules returns empty list when no rules match filters")
    void testGetRulesNoMatches() {
        List<SessionRecurrenceRule> results = ruleService.getRules(
                campaignB.getId(), center2.getId(), SessionRecurrenceRuleStatus.ACTIVE);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("getRules maintains strict deterministic ordering: startDate ASC, startTime ASC, id ASC")
    void testDeterministicOrdering() {
        List<SessionRecurrenceRule> results = ruleService.getRules(campaignA.getId(), center1.getId(), null);

        assertEquals(2, results.size());
        assertEquals(rule1.getStartDate(), results.get(0).getStartDate());
        assertEquals(rule2.getStartDate(), results.get(1).getStartDate());
        assertTrue(results.get(0).getStartTime().isBefore(results.get(1).getStartTime()));
    }
}
