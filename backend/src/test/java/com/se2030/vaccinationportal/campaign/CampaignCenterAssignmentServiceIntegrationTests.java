package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignment;
import com.se2030.vaccinationportal.campaign.model.CampaignCenterAssignmentStatus;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignCenterAssignmentRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.CampaignCenterAssignmentService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(CampaignCenterAssignmentServiceIntegrationTests.FixedClockConfig.class)
class CampaignCenterAssignmentServiceIntegrationTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired private CampaignCenterAssignmentService service;
    @Autowired private CampaignCenterAssignmentRepository assignmentRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    @Test
    void createsFiniteOpenHistoricalCurrentAndFutureAssignments() {
        VaccinationCampaign campaign = campaign("CREATE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("CREATE", CenterStatus.ACTIVE);

        CampaignCenterAssignment historical = service.createAssignment(
                campaign.getId(), center.getId(), day(-20), day(-10));
        CampaignCenterAssignment current = service.createAssignment(
                campaign.getId(), center.getId(), day(-5), day(1));
        CampaignCenterAssignment future = service.createAssignment(
                campaign.getId(), center.getId(), day(2), null);

        assertEquals(List.of(historical.getId(), current.getId(), future.getId()),
                service.getAssignmentsForCampaign(campaign.getId()).stream()
                        .map(CampaignCenterAssignment::getId).toList());
        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, future.getStatus());
        assertNotNull(future.getCreatedAt());
    }

    @Test
    void creationRejectsMissingParentsAndInvalidPeriod() {
        VaccinationCampaign campaign = campaign("MISSING", CampaignStatus.DRAFT);
        VaccinationCenter center = center("MISSING", CenterStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class,
                () -> service.createAssignment(999_999L, center.getId(), day(1), day(2)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAssignment(campaign.getId(), 999_999L, day(1), day(2)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAssignment(campaign.getId(), center.getId(), day(1), day(1)));
    }

    @ParameterizedTest
    @MethodSource("overlapCandidates")
    void creationRejectsEveryActiveOverlapShape(LocalDate from, LocalDate to) {
        VaccinationCampaign campaign = campaign("OVERLAP", CampaignStatus.DRAFT);
        VaccinationCenter center = center("OVERLAP", CenterStatus.ACTIVE);
        service.createAssignment(campaign.getId(), center.getId(), day(4), day(8));

        assertThrows(IllegalArgumentException.class,
                () -> service.createAssignment(campaign.getId(), center.getId(), from, to));
    }

    static Stream<Arguments> overlapCandidates() {
        return Stream.of(
                Arguments.of(day(4), day(8)),
                Arguments.of(day(3), day(6)),
                Arguments.of(day(6), day(9)),
                Arguments.of(day(5), day(7)),
                Arguments.of(day(3), day(9)),
                Arguments.of(day(6), null)
        );
    }

    @Test
    void creationAllowsAdjacencyAndCrossPairIndependence() {
        VaccinationCampaign firstCampaign = campaign("PAIR-A", CampaignStatus.DRAFT);
        VaccinationCampaign secondCampaign = campaign("PAIR-B", CampaignStatus.ACTIVE);
        VaccinationCenter firstCenter = center("PAIR-A", CenterStatus.ACTIVE);
        VaccinationCenter secondCenter = center("PAIR-B", CenterStatus.INACTIVE);
        service.createAssignment(firstCampaign.getId(), firstCenter.getId(), day(4), day(8));

        service.createAssignment(firstCampaign.getId(), firstCenter.getId(), day(2), day(4));
        service.createAssignment(firstCampaign.getId(), firstCenter.getId(), day(8), day(10));
        service.createAssignment(firstCampaign.getId(), secondCenter.getId(), day(4), day(8));
        service.createAssignment(secondCampaign.getId(), firstCenter.getId(), day(4), day(8));

        assertEquals(4, service.getAssignmentsForCampaign(firstCampaign.getId()).size());
        assertEquals(1, service.getAssignmentsForCampaign(secondCampaign.getId()).size());
    }

    @Test
    void cancelledSiblingDoesNotBlockReplacement() {
        VaccinationCampaign campaign = campaign("CANCELLED-SIBLING", CampaignStatus.DRAFT);
        VaccinationCenter center = center("CANCELLED-SIBLING", CenterStatus.ACTIVE);
        CampaignCenterAssignment original = service.createAssignment(
                campaign.getId(), center.getId(), day(3), day(6));
        service.cancelFutureAssignment(original.getId());

        CampaignCenterAssignment replacement = service.createAssignment(
                campaign.getId(), center.getId(), day(3), day(6));

        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, replacement.getStatus());
        assertEquals(2, service.getAssignmentsForCampaign(campaign.getId()).size());
    }

    @ParameterizedTest
    @EnumSource(CampaignStatus.class)
    void configurationIgnoresCampaignLifecycleAndAllowsInactiveCenter(CampaignStatus status) {
        VaccinationCampaign campaign = campaign("STATUS-" + status, status);
        VaccinationCenter center = center("STATUS-" + status, CenterStatus.INACTIVE);

        CampaignCenterAssignment assignment = service.createAssignment(
                campaign.getId(), center.getId(), day(1), day(2));

        assertNotNull(assignment.getId());
    }

    @Test
    void assignmentMayExtendOutsideCampaignServiceDates() {
        VaccinationCampaign campaign = campaign("OUTSIDE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("OUTSIDE", CenterStatus.ACTIVE);

        CampaignCenterAssignment assignment = service.createAssignment(
                campaign.getId(), center.getId(), TODAY.minusYears(1), TODAY.plusYears(2));

        assertNotNull(assignment.getId());
    }

    @Test
    void futureUpdateChangesFiniteOrOpenPeriod() {
        VaccinationCampaign campaign = campaign("UPDATE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("UPDATE", CenterStatus.ACTIVE);
        CampaignCenterAssignment assignment = service.createAssignment(
                campaign.getId(), center.getId(), day(3), day(5));

        CampaignCenterAssignment updated = service.updateFutureAssignment(
                assignment.getId(), day(4), null);

        assertEquals(day(4), updated.getActiveFrom());
        assertNull(updated.getActiveTo());
    }

    @Test
    void updateConflictAndInvalidCandidatePreserveExistingPeriod() {
        VaccinationCampaign campaign = campaign("SAFE-UPDATE", CampaignStatus.DRAFT);
        VaccinationCenter center = center("SAFE-UPDATE", CenterStatus.ACTIVE);
        CampaignCenterAssignment target = service.createAssignment(
                campaign.getId(), center.getId(), day(3), day(5));
        service.createAssignment(campaign.getId(), center.getId(), day(7), day(9));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureAssignment(target.getId(), day(6), day(8)));
        assertPeriod(target, day(3), day(5));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureAssignment(target.getId(), day(6), day(6)));
        assertPeriod(target, day(3), day(5));
    }

    @Test
    void updateRejectsStartedCancelledNonfutureCandidateAndMissingTargets() {
        VaccinationCampaign campaign = campaign("UPDATE-RULES", CampaignStatus.DRAFT);
        VaccinationCenter center = center("UPDATE-RULES", CenterStatus.ACTIVE);
        CampaignCenterAssignment started = service.createAssignment(
                campaign.getId(), center.getId(), TODAY, day(2));
        CampaignCenterAssignment cancelled = service.createAssignment(
                campaign.getId(), center.getId(), day(3), day(4));
        service.cancelFutureAssignment(cancelled.getId());
        CampaignCenterAssignment future = service.createAssignment(
                campaign.getId(), center.getId(), day(5), day(6));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureAssignment(started.getId(), day(7), day(8)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureAssignment(cancelled.getId(), day(7), day(8)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureAssignment(future.getId(), TODAY, day(8)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateFutureAssignment(999_999L, day(7), day(8)));
    }

    @Test
    void prospectiveEndShortensCurrentAssignmentAndKeepsActiveStatus() {
        VaccinationCampaign campaign = campaign("END", CampaignStatus.ACTIVE);
        VaccinationCenter center = center("END", CenterStatus.ACTIVE);
        CampaignCenterAssignment assignment = service.createAssignment(
                campaign.getId(), center.getId(), day(-5), day(10));

        CampaignCenterAssignment ended = service.endAssignment(assignment.getId(), day(3));

        assertEquals(day(3), ended.getActiveTo());
        assertEquals(CampaignCenterAssignmentStatus.ACTIVE, ended.getStatus());
        assertTrue(ended.isEffectiveOn(TODAY));
    }

    @Test
    void endingRejectsExtensionNoOpRetroactiveFutureExpiredAndCancelled() {
        VaccinationCampaign campaign = campaign("END-RULES", CampaignStatus.ACTIVE);
        VaccinationCenter center = center("END-RULES", CenterStatus.ACTIVE);
        CampaignCenterAssignment current = service.createAssignment(
                campaign.getId(), center.getId(), day(-5), day(5));
        CampaignCenterAssignment future = service.createAssignment(
                campaign.getId(), center.getId(), day(6), day(8));
        CampaignCenterAssignment expired = service.createAssignment(
                campaign.getId(), center.getId(), day(-10), day(-5));
        CampaignCenterAssignment cancelled = service.createAssignment(
                campaign.getId(), center.getId(), day(9), day(10));
        service.cancelFutureAssignment(cancelled.getId());

        assertThrows(IllegalArgumentException.class, () -> service.endAssignment(current.getId(), day(6)));
        assertThrows(IllegalArgumentException.class, () -> service.endAssignment(current.getId(), day(5)));
        assertThrows(IllegalArgumentException.class, () -> service.endAssignment(current.getId(), TODAY));
        assertThrows(IllegalArgumentException.class, () -> service.endAssignment(future.getId(), day(7)));
        assertThrows(IllegalArgumentException.class, () -> service.endAssignment(expired.getId(), day(1)));
        assertThrows(IllegalArgumentException.class, () -> service.endAssignment(cancelled.getId(), day(1)));
        assertPeriod(current, day(-5), day(5));
    }

    @Test
    void explicitEndAllowsAdjacentReplacement() {
        VaccinationCampaign campaign = campaign("REPLACE", CampaignStatus.ACTIVE);
        VaccinationCenter center = center("REPLACE", CenterStatus.ACTIVE);
        CampaignCenterAssignment current = service.createAssignment(
                campaign.getId(), center.getId(), day(-5), null);
        service.endAssignment(current.getId(), day(2));

        CampaignCenterAssignment replacement = service.createAssignment(
                campaign.getId(), center.getId(), day(2), null);

        assertEquals(day(2), replacement.getActiveFrom());
        assertEquals(2, service.getAssignmentsForCampaign(campaign.getId()).size());
    }

    @Test
    void futureCancellationIsTerminalRetainedAndExactStartIsTooLate() {
        VaccinationCampaign campaign = campaign("CANCEL", CampaignStatus.DRAFT);
        VaccinationCenter center = center("CANCEL", CenterStatus.ACTIVE);
        CampaignCenterAssignment future = service.createAssignment(
                campaign.getId(), center.getId(), day(2), day(4));
        CampaignCenterAssignment current = service.createAssignment(
                campaign.getId(), center.getId(), TODAY, day(1));

        CampaignCenterAssignment cancelled = service.cancelFutureAssignment(future.getId());

        assertEquals(CampaignCenterAssignmentStatus.CANCELLED, cancelled.getStatus());
        assertTrue(assignmentRepository.existsById(cancelled.getId()));
        assertPeriod(cancelled, day(2), day(4));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelFutureAssignment(cancelled.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelFutureAssignment(current.getId()));
    }

    @Test
    void readsAndCoverageHelperUseHalfOpenActiveSemantics() {
        VaccinationCampaign campaign = campaign("READ", CampaignStatus.ACTIVE);
        VaccinationCenter center = center("READ", CenterStatus.ACTIVE);
        CampaignCenterAssignment assignment = service.createAssignment(
                campaign.getId(), center.getId(), day(-2), day(3));

        assertEquals(assignment.getId(), service.getAssignmentById(assignment.getId()).orElseThrow().getId());
        assertTrue(service.getAssignmentById(999_999L).isEmpty());
        assertEquals(assignment.getId(),
                service.getEffectiveAssignmentAt(campaign.getId(), center.getId(), TODAY).orElseThrow().getId());
        assertTrue(service.getEffectiveAssignmentAt(campaign.getId(), center.getId(), day(3)).isEmpty());
        assertTrue(service.hasActiveAssignmentCovering(campaign.getId(), center.getId(), day(-2), day(3)));
        assertFalse(service.hasActiveAssignmentCovering(campaign.getId(), center.getId(), day(-3), day(3)));
        assertFalse(service.hasActiveAssignmentCovering(campaign.getId(), center.getId(), day(-2), day(4)));
        assertThrows(IllegalArgumentException.class,
                () -> service.hasActiveAssignmentCovering(campaign.getId(), center.getId(), TODAY, TODAY));
        assertEquals(1, service.getAssignmentHistoryForCenter(center.getId()).size());
    }

    private void assertPeriod(CampaignCenterAssignment assignment, LocalDate from, LocalDate to) {
        assertEquals(from, assignment.getActiveFrom());
        assertEquals(to, assignment.getActiveTo());
    }

    private VaccinationCampaign campaign(String suffix, CampaignStatus status) {
        VaccineType vaccine = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-" + suffix, "Vaccine " + suffix));
        TargetGroup group = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-" + suffix, "Group " + suffix));
        return campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-" + suffix, "Campaign " + suffix, null, vaccine, group,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"), status));
    }

    private VaccinationCenter center(String suffix, CenterStatus status) {
        return centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-" + suffix, "Center " + suffix, "1 Test Road", "+94 11 000 0000",
                status, 100, 50, "UTC"));
    }

    private static LocalDate day(long offset) {
        return TODAY.plusDays(offset);
    }
}
