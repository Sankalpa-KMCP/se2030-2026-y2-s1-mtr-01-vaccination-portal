package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
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
 * Integration tests verifying persistence, unique constraints, and repository methods
 * for the MF-02 campaign domain slice.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CampaignPersistenceIntegrationTests {

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Test
    @DisplayName("Successfully persist and retrieve VaccineType entity")
    void persistAndRetrieveVaccineType() {
        VaccineType vaccineType = new VaccineType("VAX-COV-MRNA", "COVID-19 mRNA Monovalent", "0.5ml", true);
        VaccineType saved = vaccineTypeRepository.saveAndFlush(vaccineType);

        assertNotNull(saved.getId());
        assertEquals("VAX-COV-MRNA", saved.getCode());
        assertEquals("COVID-19 mRNA Monovalent", saved.getName());
        assertEquals("0.5ml", saved.getDoseUnit());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());

        Optional<VaccineType> found = vaccineTypeRepository.findByCode("VAX-COV-MRNA");
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(vaccineTypeRepository.existsByCode("VAX-COV-MRNA"));
    }

    @Test
    @DisplayName("Duplicate VaccineType code violates unique constraint")
    void duplicateVaccineTypeCodeViolatesUniqueConstraint() {
        VaccineType vt1 = new VaccineType("VAX-DUPLICATE-01", "Type One", "dose", true);
        vaccineTypeRepository.saveAndFlush(vt1);

        VaccineType vt2 = new VaccineType("VAX-DUPLICATE-01", "Type Two", "dose", true);
        assertThrows(DataIntegrityViolationException.class, () -> {
            vaccineTypeRepository.saveAndFlush(vt2);
        });
    }

    @Test
    @DisplayName("Successfully persist and retrieve TargetGroup entity")
    void persistAndRetrieveTargetGroup() {
        TargetGroup tg = new TargetGroup("TG-HEALTHCARE-WORKERS", "Frontline Healthcare Personnel", true);
        TargetGroup saved = targetGroupRepository.saveAndFlush(tg);

        assertNotNull(saved.getId());
        assertEquals("TG-HEALTHCARE-WORKERS", saved.getCode());
        assertEquals("Frontline Healthcare Personnel", saved.getDescription());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());

        Optional<TargetGroup> found = targetGroupRepository.findByCode("TG-HEALTHCARE-WORKERS");
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(targetGroupRepository.existsByCode("TG-HEALTHCARE-WORKERS"));
    }

    @Test
    @DisplayName("Duplicate TargetGroup code violates unique constraint")
    void duplicateTargetGroupCodeViolatesUniqueConstraint() {
        TargetGroup tg1 = new TargetGroup("TG-DUP-01", "Target Group 1", true);
        targetGroupRepository.saveAndFlush(tg1);

        TargetGroup tg2 = new TargetGroup("TG-DUP-01", "Target Group 2", true);
        assertThrows(DataIntegrityViolationException.class, () -> {
            targetGroupRepository.saveAndFlush(tg2);
        });
    }

    @Test
    @DisplayName("Successfully persist VaccinationCampaign with relations and query by status and code")
    void persistAndRetrieveVaccinationCampaign() {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-FLU-01", "Seasonal Influenza Inactivated", "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-GENERAL-PUBLIC", "General Public 18+", true)
        );

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2027, 3, 31);
        LocalDateTime bookingOpen = LocalDateTime.of(2026, 9, 15, 8, 0);
        LocalDateTime bookingClose = LocalDateTime.of(2027, 3, 30, 18, 0);

        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-FLU-2026",
                "National Seasonal Flu Campaign 2026/2027",
                "Annual flu immunization drive",
                vaccineType,
                targetGroup,
                startDate,
                endDate,
                bookingOpen,
                bookingClose,
                CampaignStatus.SCHEDULED
        );

        VaccinationCampaign saved = campaignRepository.saveAndFlush(campaign);

        assertNotNull(saved.getId());
        assertEquals("CMP-FLU-2026", saved.getCode());
        assertEquals("National Seasonal Flu Campaign 2026/2027", saved.getName());
        assertEquals(CampaignStatus.SCHEDULED, saved.getStatus());
        assertEquals(vaccineType.getId(), saved.getVaccineType().getId());
        assertEquals(targetGroup.getId(), saved.getTargetGroup().getId());
        assertNotNull(saved.getCreatedAt());

        // Test custom repository methods
        Optional<VaccinationCampaign> byCode = campaignRepository.findByCode("CMP-FLU-2026");
        assertTrue(byCode.isPresent());
        assertEquals(saved.getId(), byCode.get().getId());
        assertTrue(campaignRepository.existsByCode("CMP-FLU-2026"));

        List<VaccinationCampaign> scheduled = campaignRepository.findByStatus(CampaignStatus.SCHEDULED);
        assertFalse(scheduled.isEmpty());
        assertTrue(scheduled.stream().anyMatch(c -> c.getCode().equals("CMP-FLU-2026")));

        List<VaccinationCampaign> byVaccineType = campaignRepository.findByVaccineTypeId(vaccineType.getId());
        assertEquals(1, byVaccineType.size());

        List<VaccinationCampaign> byTargetGroup = campaignRepository.findByTargetGroupId(targetGroup.getId());
        assertEquals(1, byTargetGroup.size());
    }

    @Test
    @DisplayName("Duplicate VaccinationCampaign code violates unique constraint")
    void duplicateCampaignCodeViolatesUniqueConstraint() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-SHARED", "Shared Vax", "dose", true));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-SHARED", "Shared Target", true));

        VaccinationCampaign c1 = new VaccinationCampaign(
                "CMP-UNIQUE-CODE-01",
                "Campaign One",
                null,
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 1, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.DRAFT
        );
        campaignRepository.saveAndFlush(c1);

        VaccinationCampaign c2 = new VaccinationCampaign(
                "CMP-UNIQUE-CODE-01",
                "Campaign Two with same code",
                null,
                vt,
                tg,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 1, 8, 0),
                LocalDateTime.of(2026, 12, 30, 18, 0),
                CampaignStatus.DRAFT
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            campaignRepository.saveAndFlush(c2);
        });
    }

    @Test
    @DisplayName("findOpenForBookingAt type-safely filters PUBLISHED and ACTIVE campaigns within open window")
    void findOpenForBookingAtFiltersCorrectly() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-COVID-02", "COVID-19 Booster", "dose", true));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-VULNERABLE", "Immunocompromised Individuals", true));

        LocalDateTime now = LocalDateTime.of(2026, 10, 15, 10, 0);

        // Campaign 1: Published & inside window
        VaccinationCampaign publishedOpen = new VaccinationCampaign(
                "CMP-PUB-OPEN",
                "Published Open",
                null,
                vt,
                tg,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 10, 1, 8, 0),
                LocalDateTime.of(2026, 12, 31, 18, 0),
                CampaignStatus.PUBLISHED
        );
        campaignRepository.saveAndFlush(publishedOpen);

        // Campaign 2: Active & inside window
        VaccinationCampaign activeOpen = new VaccinationCampaign(
                "CMP-ACT-OPEN",
                "Active Open",
                null,
                vt,
                tg,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 10, 1, 8, 0),
                LocalDateTime.of(2026, 12, 31, 18, 0),
                CampaignStatus.ACTIVE
        );
        campaignRepository.saveAndFlush(activeOpen);

        // Campaign 3: Suspended & inside date window (must be excluded per MF02-BR-03)
        VaccinationCampaign suspended = new VaccinationCampaign(
                "CMP-SUSPENDED",
                "Suspended Campaign",
                null,
                vt,
                tg,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 10, 1, 8, 0),
                LocalDateTime.of(2026, 12, 31, 18, 0),
                CampaignStatus.SUSPENDED
        );
        campaignRepository.saveAndFlush(suspended);

        // Campaign 4: Published but booking window is closed
        VaccinationCampaign closedWindow = new VaccinationCampaign(
                "CMP-CLOSED-WIN",
                "Closed Window Campaign",
                null,
                vt,
                tg,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 9, 1, 8, 0),
                LocalDateTime.of(2026, 10, 1, 18, 0), // Closed before 'now'
                CampaignStatus.PUBLISHED
        );
        campaignRepository.saveAndFlush(closedWindow);

        // Test type-safe parameterized query method
        List<VaccinationCampaign> parameterizedResults = campaignRepository.findByStatusInAndBookingWindowOpenAt(
                List.of(CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE),
                now
        );
        List<String> paramCodes = parameterizedResults.stream().map(VaccinationCampaign::getCode).toList();
        assertTrue(paramCodes.contains("CMP-PUB-OPEN"));
        assertTrue(paramCodes.contains("CMP-ACT-OPEN"));
        assertFalse(paramCodes.contains("CMP-SUSPENDED"));
        assertFalse(paramCodes.contains("CMP-CLOSED-WIN"));

        // Test default convenience method
        List<VaccinationCampaign> openCampaigns = campaignRepository.findOpenForBookingAt(now);
        List<String> openCodes = openCampaigns.stream().map(VaccinationCampaign::getCode).toList();

        assertTrue(openCodes.contains("CMP-PUB-OPEN"));
        assertTrue(openCodes.contains("CMP-ACT-OPEN"));
        assertFalse(openCodes.contains("CMP-SUSPENDED"), "Suspended campaigns must not be open for booking");
        assertFalse(openCodes.contains("CMP-CLOSED-WIN"), "Campaigns past booking close must not be open for booking");
    }

    @Test
    @DisplayName("findOverlappingServiceDateRange identifies campaigns with overlapping service date spans")
    void findOverlappingServiceDateRangeQueriesOverlaps() {
        VaccineType vt = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-HPV", "HPV Recombinant", "dose", true));
        TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-ADOLESCENT", "Adolescents 12-15", true));

        VaccinationCampaign campaign = new VaccinationCampaign(
                "CMP-SPRING-2026",
                "Spring 2026 School Campaign",
                null,
                vt,
                tg,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                LocalDateTime.of(2026, 3, 1, 8, 0),
                LocalDateTime.of(2026, 6, 25, 18, 0),
                CampaignStatus.SCHEDULED
        );
        campaignRepository.saveAndFlush(campaign);

        // Overlapping query range (May 2026)
        List<VaccinationCampaign> mayResults = campaignRepository.findOverlappingServiceDateRange(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );
        assertTrue(mayResults.stream().anyMatch(c -> c.getCode().equals("CMP-SPRING-2026")));

        // Non-overlapping query range (July 2026)
        List<VaccinationCampaign> julyResults = campaignRepository.findOverlappingServiceDateRange(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );
        assertFalse(julyResults.stream().anyMatch(c -> c.getCode().equals("CMP-SPRING-2026")));
    }
}
