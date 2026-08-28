package com.se2030.vaccinationportal.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.dto.request.ArchiveCampaignRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CancelCampaignRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CompleteCampaignRequest;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.CampaignLifecycleEventRepository;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.campaign.service.VaccinationCampaignService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests verifying MF02-R1 terminal campaign lifecycle operations (Complete, Cancel, Archive).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CampaignTerminalLifecycleIntegrationTests {

    @Autowired private VaccinationCampaignService campaignService;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private CampaignLifecycleEventRepository lifecycleEventRepository;
    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;

    private VaccineType vaccineType;
    private TargetGroup targetGroup;
    private VaccinationCenter center;

    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);
    private static final Instant BOOKING_OPEN = Instant.parse("2026-08-15T08:00:00Z");
    private static final Instant BOOKING_CLOSE = Instant.parse("2026-12-30T18:00:00Z");

    @BeforeEach
    void setUp() {
        vaccineType = vaccineTypeRepository.save(new VaccineType("VAX-TERM-01", "Terminal Test Vaccine", "dose", true));
        targetGroup = targetGroupRepository.save(new TargetGroup("TG-TERM-01", "Terminal Test Group", true));
        center = centerRepository.save(new VaccinationCenter(
                "CTR-TERM-01", "Terminal Test Center", "100 Health Way", "contact@center.gov",
                CenterStatus.ACTIVE, 100, 200, "UTC"
        ));
    }

    private VaccinationCampaign createCampaign(String code, CampaignStatus status) {
        return campaignRepository.save(CampaignTestFixtures.campaign(
                code, "Campaign " + code, "Description",
                vaccineType, targetGroup, START_DATE, END_DATE, BOOKING_OPEN, BOOKING_CLOSE, status
        ));
    }

    private VaccinationSession createSession(VaccinationCampaign campaign, SessionLifecycleStatus status) {
        VaccinationSession session = new VaccinationSession(
                campaign, center,
                Instant.parse("2026-10-01T09:00:00Z"),
                Instant.parse("2026-10-01T17:00:00Z"),
                Instant.parse("2026-09-30T18:00:00Z"),
                "UTC"
        );
        if (status == SessionLifecycleStatus.SCHEDULED) {
            session.schedule();
        } else if (status == SessionLifecycleStatus.IN_PROGRESS) {
            session.schedule();
            session.start();
        } else if (status == SessionLifecycleStatus.COMPLETED) {
            session.schedule();
            session.start();
            session.complete();
        } else if (status == SessionLifecycleStatus.CANCELLED) {
            session.cancel();
        }
        return sessionRepository.save(session);
    }

    // =========================================================================
    // 1. COMPLETE CAMPAIGN
    // =========================================================================

    @Nested
    @DisplayName("Complete Campaign Tests")
    class CompleteCampaignTests {

        @Test
        @DisplayName("Complete transitions ACTIVE campaign to COMPLETED and persists lifecycle event")
        void completeActiveCampaignSucceeds() {
            VaccinationCampaign campaign = createCampaign("CMP-TERM-ACT-01", CampaignStatus.ACTIVE);

            VaccinationCampaign completed = campaignService.completeCampaign(campaign.getId(), "Operations completed", "officer-10");

            assertEquals(CampaignStatus.COMPLETED, completed.getStatus());
            assertTrue(completed.getStatus().isOperationallyClosed());
            assertFalse(completed.getStatus().isIntakePermitted());

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
            assertEquals(1, events.size());
            CampaignLifecycleEvent event = events.get(0);
            assertEquals(CampaignLifecycleChangeType.COMPLETED, event.getChangeType());
            assertEquals(CampaignStatus.ACTIVE, event.getPreviousStatus());
            assertEquals(CampaignStatus.COMPLETED, event.getNewStatus());
            assertEquals("Operations completed", event.getReason());
            assertEquals("officer-10", event.getActorId());
        }

        @Test
        @DisplayName("Complete transitions PUBLISHED and SUSPENDED campaigns to COMPLETED")
        void completePublishedAndSuspendedCampaignsSucceed() {
            VaccinationCampaign pub = createCampaign("CMP-TERM-PUB-01", CampaignStatus.PUBLISHED);
            VaccinationCampaign completedPub = campaignService.completeCampaign(pub.getId(), "Published campaign concluded", "officer-11");
            assertEquals(CampaignStatus.COMPLETED, completedPub.getStatus());

            VaccinationCampaign sus = createCampaign("CMP-TERM-SUS-01", CampaignStatus.SUSPENDED);
            VaccinationCampaign completedSus = campaignService.completeCampaign(sus.getId(), "Suspended campaign concluded", "officer-12");
            assertEquals(CampaignStatus.COMPLETED, completedSus.getStatus());
        }

        @Test
        @DisplayName("Complete fails if unfinished sessions (DRAFT, SCHEDULED, IN_PROGRESS) exist")
        void completeFailsWithUnfinishedSessions() {
            VaccinationCampaign campaign = createCampaign("CMP-TERM-UNF-01", CampaignStatus.ACTIVE);
            createSession(campaign, SessionLifecycleStatus.SCHEDULED);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    campaignService.completeCampaign(campaign.getId(), "Attempt completion", "officer-13")
            );

            assertTrue(ex.getMessage().contains("unfinished session(s) remain"));
            assertTrue(ex.getMessage().contains("SCHEDULED"));
        }

        @Test
        @DisplayName("Complete succeeds if all sessions are COMPLETED or CANCELLED")
        void completeSucceedsWithResolvedSessions() {
            VaccinationCampaign campaign = createCampaign("CMP-TERM-RES-01", CampaignStatus.ACTIVE);
            createSession(campaign, SessionLifecycleStatus.COMPLETED);
            createSession(campaign, SessionLifecycleStatus.CANCELLED);

            VaccinationCampaign completed = campaignService.completeCampaign(campaign.getId(), "All sessions concluded", "officer-14");
            assertEquals(CampaignStatus.COMPLETED, completed.getStatus());
        }

        @Test
        @DisplayName("Complete fails for DRAFT and SCHEDULED campaigns")
        void completeFailsForDraftAndScheduled() {
            VaccinationCampaign draft = createCampaign("CMP-TERM-DFT-01", CampaignStatus.DRAFT);
            assertThrows(IllegalStateException.class, () ->
                    campaignService.completeCampaign(draft.getId(), "Invalid complete", "officer-15")
            );

            VaccinationCampaign scheduled = createCampaign("CMP-TERM-SCH-01", CampaignStatus.SCHEDULED);
            assertThrows(IllegalStateException.class, () ->
                    campaignService.completeCampaign(scheduled.getId(), "Invalid complete", "officer-15")
            );
        }

        @Test
        @DisplayName("Complete rejects blank or invalid reasons")
        void completeRejectsInvalidReasons() {
            VaccinationCampaign campaign = createCampaign("CMP-TERM-RSN-01", CampaignStatus.ACTIVE);

            assertThrows(IllegalArgumentException.class, () ->
                    campaignService.completeCampaign(campaign.getId(), null, "officer-16")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    campaignService.completeCampaign(campaign.getId(), "   ", "officer-16")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    campaignService.completeCampaign(campaign.getId(), "R".repeat(501), "officer-16")
            );
        }
    }

    // =========================================================================
    // 2. CANCEL CAMPAIGN
    // =========================================================================

    @Nested
    @DisplayName("Cancel Campaign Tests")
    class CancelCampaignTests {

        @Test
        @DisplayName("Cancel transitions DRAFT, SCHEDULED, PUBLISHED, ACTIVE, and SUSPENDED campaigns to CANCELLED")
        void cancelNonTerminalCampaignsSucceeds() {
            for (CampaignStatus source : new CampaignStatus[]{
                    CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, CampaignStatus.SUSPENDED
            }) {
                VaccinationCampaign campaign = createCampaign("CMP-CAN-" + source.name(), source);
                VaccinationCampaign cancelled = campaignService.cancelCampaign(campaign.getId(), "Cancelled due to emergency", "admin-20");

                assertEquals(CampaignStatus.CANCELLED, cancelled.getStatus());
                assertTrue(cancelled.getStatus().isOperationallyClosed());

                List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(campaign.getId());
                assertFalse(events.isEmpty());
                CampaignLifecycleEvent lastEvent = events.get(events.size() - 1);
                assertEquals(CampaignLifecycleChangeType.CANCELLED, lastEvent.getChangeType());
                assertEquals(source, lastEvent.getPreviousStatus());
                assertEquals(CampaignStatus.CANCELLED, lastEvent.getNewStatus());
                assertEquals("Cancelled due to emergency", lastEvent.getReason());
            }
        }

        @Test
        @DisplayName("Cancel fails if campaign is already COMPLETED, CANCELLED, or ARCHIVED")
        void cancelFailsOnOperationallyClosedCampaigns() {
            VaccinationCampaign completed = createCampaign("CMP-CAN-COMP-01", CampaignStatus.COMPLETED);
            assertThrows(IllegalStateException.class, () ->
                    campaignService.cancelCampaign(completed.getId(), "Cannot cancel completed", "admin-21")
            );

            VaccinationCampaign cancelled = createCampaign("CMP-CAN-CAN-01", CampaignStatus.CANCELLED);
            assertThrows(IllegalStateException.class, () ->
                    campaignService.cancelCampaign(cancelled.getId(), "Cannot cancel cancelled", "admin-21")
            );

            VaccinationCampaign archived = createCampaign("CMP-CAN-ARC-01", CampaignStatus.ARCHIVED);
            assertThrows(IllegalStateException.class, () ->
                    campaignService.cancelCampaign(archived.getId(), "Cannot cancel archived", "admin-21")
            );
        }

        @Test
        @DisplayName("Cancel rejects blank or invalid reasons")
        void cancelRejectsInvalidReasons() {
            VaccinationCampaign campaign = createCampaign("CMP-CAN-RSN-01", CampaignStatus.ACTIVE);

            assertThrows(IllegalArgumentException.class, () ->
                    campaignService.cancelCampaign(campaign.getId(), null, "admin-22")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    campaignService.cancelCampaign(campaign.getId(), "", "admin-22")
            );
            assertThrows(IllegalArgumentException.class, () ->
                    campaignService.cancelCampaign(campaign.getId(), "C".repeat(501), "admin-22")
            );
        }
    }

    // =========================================================================
    // 3. ARCHIVE CAMPAIGN
    // =========================================================================

    @Nested
    @DisplayName("Archive Campaign Tests")
    class ArchiveCampaignTests {

        @Test
        @DisplayName("Archive transitions COMPLETED and CANCELLED campaigns to ARCHIVED")
        void archiveCompletedAndCancelledSucceeds() {
            VaccinationCampaign comp = createCampaign("CMP-ARC-COMP-01", CampaignStatus.COMPLETED);
            VaccinationCampaign archivedComp = campaignService.archiveCampaign(comp.getId(), "Statutory archival", "admin-30");
            assertEquals(CampaignStatus.ARCHIVED, archivedComp.getStatus());

            VaccinationCampaign can = createCampaign("CMP-ARC-CAN-01", CampaignStatus.CANCELLED);
            VaccinationCampaign archivedCan = campaignService.archiveCampaign(can.getId(), "Retention archival", "admin-30");
            assertEquals(CampaignStatus.ARCHIVED, archivedCan.getStatus());

            List<CampaignLifecycleEvent> events = lifecycleEventRepository.findByCampaignIdOrderByCreatedAtAscIdAsc(can.getId());
            assertEquals(1, events.size());
            CampaignLifecycleEvent event = events.get(0);
            assertEquals(CampaignLifecycleChangeType.ARCHIVED, event.getChangeType());
            assertEquals(CampaignStatus.CANCELLED, event.getPreviousStatus());
            assertEquals(CampaignStatus.ARCHIVED, event.getNewStatus());
            assertEquals("Retention archival", event.getReason());
        }

        @Test
        @DisplayName("Archive fails for non-archivable statuses (DRAFT, SCHEDULED, PUBLISHED, ACTIVE, SUSPENDED, ARCHIVED)")
        void archiveFailsForNonArchivableStatuses() {
            for (CampaignStatus status : new CampaignStatus[]{
                    CampaignStatus.DRAFT, CampaignStatus.SCHEDULED, CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, CampaignStatus.SUSPENDED, CampaignStatus.ARCHIVED
            }) {
                VaccinationCampaign campaign = createCampaign("CMP-ARC-REJ-" + status.name(), status);
                assertThrows(IllegalStateException.class, () ->
                        campaignService.archiveCampaign(campaign.getId(), "Attempt archive", "admin-31")
                );
            }
        }

        @Test
        @DisplayName("Archived records remain fully queryable with all history intact")
        void archivedRecordsRemainQueryable() {
            VaccinationCampaign campaign = createCampaign("CMP-ARC-HIST-01", CampaignStatus.COMPLETED);
            campaignService.archiveCampaign(campaign.getId(), "Historical retention archive", "admin-32");

            VaccinationCampaign fetched = campaignService.getRequiredCampaignById(campaign.getId());
            assertNotNull(fetched);
            assertEquals(CampaignStatus.ARCHIVED, fetched.getStatus());

            List<CampaignLifecycleEvent> events = campaignService.getCampaignLifecycleEvents(campaign.getId());
            assertEquals(1, events.size());
            assertEquals(CampaignLifecycleChangeType.ARCHIVED, events.get(0).getChangeType());
        }
    }
}
