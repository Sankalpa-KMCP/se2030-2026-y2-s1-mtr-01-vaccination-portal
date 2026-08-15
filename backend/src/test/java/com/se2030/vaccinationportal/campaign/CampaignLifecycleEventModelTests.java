package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleChangeType;
import com.se2030.vaccinationportal.campaign.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignLifecycleEventModelTests {

    private static final Instant CREATED_AT = Instant.parse("2026-08-13T08:30:00Z");
    private VaccinationCampaign campaign;

    @BeforeEach
    void setUp() {
        campaign = CampaignTestFixtures.campaign(
                "CMP-EVENT-M", "Campaign Event Model", null,
                new VaccineType("VAX-EVENT-M", "Campaign Event Vaccine"),
                new TargetGroup("TG-EVENT-M", "Campaign Event Group"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-12-30T00:00:00Z"),
                CampaignStatus.SUSPENDED
        );
    }

    @Test
    void validEventNormalizesReasonAndActorAndInitializesAllFields() {
        CampaignLifecycleEvent event = event("  Public-health hold  ", "  admin-17  ", CREATED_AT);

        assertAll(
                () -> assertNull(event.getId()),
                () -> assertEquals(campaign, event.getCampaign()),
                () -> assertEquals(CampaignLifecycleChangeType.SUSPENDED, event.getChangeType()),
                () -> assertEquals(CampaignStatus.PUBLISHED, event.getPreviousStatus()),
                () -> assertEquals(CampaignStatus.SUSPENDED, event.getNewStatus()),
                () -> assertEquals("Public-health hold", event.getReason()),
                () -> assertEquals("admin-17", event.getActorId()),
                () -> assertEquals(CREATED_AT, event.getCreatedAt())
        );
    }

    @Test
    void reasonValidationRejectsNullBlankAndOversize() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> event(null, "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("   ", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("R".repeat(501), "actor", CREATED_AT))
        );
    }

    @Test
    void reasonAtMaximumLengthIsAccepted() {
        assertEquals(500, event("R".repeat(500), "actor", CREATED_AT).getReason().length());
    }

    @Test
    void actorValidationRejectsNullBlankAndOversize() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> event("reason", null, CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("reason", "", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("reason", "   ", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("reason", "A".repeat(65), CREATED_AT))
        );
    }

    @Test
    void actorAtMaximumLengthIsAccepted() {
        assertEquals(64, event("reason", "A".repeat(64), CREATED_AT).getActorId().length());
    }

    @Test
    void requiredReferencesStatusesTypeAndTimestampAreEnforced() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(null, CampaignLifecycleChangeType.SUSPENDED,
                                CampaignStatus.PUBLISHED, CampaignStatus.SUSPENDED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, null,
                                CampaignStatus.PUBLISHED, CampaignStatus.SUSPENDED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.SUSPENDED,
                                null, CampaignStatus.SUSPENDED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.SUSPENDED,
                                CampaignStatus.PUBLISHED, null, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () -> event("reason", "actor", null))
        );
    }

    @Test
    void suspensionEventRejectsAnInconsistentStatusPair() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.SUSPENDED,
                                CampaignStatus.DRAFT, CampaignStatus.SUSPENDED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.SUSPENDED,
                                CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, "reason", "actor", CREATED_AT))
        );
    }

    @Test
    void resumedEventAcceptsValidStatusPairs() {
        assertAll(
                () -> {
                    CampaignLifecycleEvent toPublished = new CampaignLifecycleEvent(
                            campaign, CampaignLifecycleChangeType.RESUMED,
                            CampaignStatus.SUSPENDED, CampaignStatus.PUBLISHED,
                            "Hold lifted before service start", "admin-1", CREATED_AT
                    );
                    assertEquals(CampaignLifecycleChangeType.RESUMED, toPublished.getChangeType());
                    assertEquals(CampaignStatus.SUSPENDED, toPublished.getPreviousStatus());
                    assertEquals(CampaignStatus.PUBLISHED, toPublished.getNewStatus());
                },
                () -> {
                    CampaignLifecycleEvent toActive = new CampaignLifecycleEvent(
                            campaign, CampaignLifecycleChangeType.RESUMED,
                            CampaignStatus.SUSPENDED, CampaignStatus.ACTIVE,
                            "Hold lifted during service period", "admin-1", CREATED_AT
                    );
                    assertEquals(CampaignLifecycleChangeType.RESUMED, toActive.getChangeType());
                    assertEquals(CampaignStatus.SUSPENDED, toActive.getPreviousStatus());
                    assertEquals(CampaignStatus.ACTIVE, toActive.getNewStatus());
                }
        );
    }

    @Test
    void resumedEventRejectsInconsistentStatusPairs() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.RESUMED,
                                CampaignStatus.PUBLISHED, CampaignStatus.ACTIVE, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.RESUMED,
                                CampaignStatus.ACTIVE, CampaignStatus.PUBLISHED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.RESUMED,
                                CampaignStatus.SUSPENDED, CampaignStatus.COMPLETED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.RESUMED,
                                CampaignStatus.SUSPENDED, CampaignStatus.CANCELLED, "reason", "actor", CREATED_AT)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new CampaignLifecycleEvent(campaign, CampaignLifecycleChangeType.RESUMED,
                                CampaignStatus.DRAFT, CampaignStatus.PUBLISHED, "reason", "actor", CREATED_AT))
        );
    }

    @Test
    void eventExposesNoBroadPublicSetters() {
        boolean hasPublicSetter = Arrays.stream(CampaignLifecycleEvent.class.getMethods())
                .anyMatch(method -> Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("set"));

        assertTrue(!hasPublicSetter, "CampaignLifecycleEvent must remain append-only");
    }

    private CampaignLifecycleEvent event(String reason, String actorId, Instant createdAt) {
        return new CampaignLifecycleEvent(
                campaign,
                CampaignLifecycleChangeType.SUSPENDED,
                CampaignStatus.PUBLISHED,
                CampaignStatus.SUSPENDED,
                reason,
                actorId,
                createdAt
        );
    }
}
