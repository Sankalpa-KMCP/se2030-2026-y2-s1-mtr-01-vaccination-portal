package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.knowledge.AssistantKnowledgeCatalog;
import com.se2030.vaccinationportal.assistant.knowledge.AssistantKnowledgeContextBuilder;
import com.se2030.vaccinationportal.assistant.knowledge.AssistantKnowledgeEntry;
import com.se2030.vaccinationportal.assistant.policy.AssistantRefusalPolicy;
import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Assistant Knowledge Catalog & Context Builder Tests")
class AssistantKnowledgeCatalogTests {

    @Test
    @DisplayName("Public/anonymous users receive only public knowledge entries")
    void testPublicUsersReceiveOnlyPublicKnowledge() {
        // Public queries
        List<AssistantKnowledgeEntry> publicMatches = AssistantKnowledgeCatalog.findRelevantEntries(
                "How does the portal work and how can a citizen register?",
                Collections.emptySet(),
                5
        );
        assertThat(publicMatches).isNotEmpty();
        for (AssistantKnowledgeEntry entry : publicMatches) {
            assertTrue(entry.isPublic(), "Anonymous caller must only receive public knowledge entries");
        }

        // Staff operational query by anonymous user
        List<AssistantKnowledgeEntry> staffMatches = AssistantKnowledgeCatalog.findRelevantEntries(
                "vaccine inventory batch stock levels and adjustments",
                Collections.emptySet(),
                5
        );
        assertTrue(staffMatches.isEmpty(), "Anonymous caller must not receive staff inventory entries");
    }

    @Test
    @DisplayName("Citizen questions retrieve citizen-safe workflow information and no staff operational data")
    void testCitizenQuestionsRetrieveCitizenSafeWorkflows() {
        Set<String> citizenRoles = Set.of(AssistantRouteCatalog.ROLE_CITIZEN);

        // Booking query
        List<AssistantKnowledgeEntry> bookingEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "How do I book an appointment for vaccination?",
                citizenRoles,
                3
        );
        assertThat(bookingEntries).isNotEmpty();
        assertEquals("APPOINTMENT_BOOKING", bookingEntries.get(0).id());
        assertThat(bookingEntries.get(0).relevantRoutes()).contains("/app/appointments/book");
        assertThat(bookingEntries.get(0).explanation()).doesNotContain("/app/appointments/book");

        // Cancellation / Rescheduling query
        List<AssistantKnowledgeEntry> apptMgmt = AssistantKnowledgeCatalog.findRelevantEntries(
                "How to cancel or reschedule my appointment?",
                citizenRoles,
                3
        );
        assertThat(apptMgmt).isNotEmpty();
        assertThat(apptMgmt.stream().anyMatch(e -> e.id().equals("APPOINTMENT_MANAGEMENT"))).isTrue();

        // Staff-only query by citizen: citizen must NOT receive staff entries
        List<AssistantKnowledgeEntry> staffEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "center check-in queue management arrival reversal",
                citizenRoles,
                3
        );
        for (AssistantKnowledgeEntry entry : staffEntries) {
            assertNotEquals("STAFF_CHECK_IN", entry.id(), "Citizen must never receive staff check-in operations");
        }
    }

    @Test
    @DisplayName("Complete generated context for an anonymous caller never leaks protected routes")
    void testAnonymousCallerContextNeverContainsProtectedRoutes() {
        // Query multiple topics that might touch staff, screening, appointment, inventory, or checkin concepts
        List<String> testQueries = List.of(
                "How do staff register and get approved?",
                "Am I eligible for vaccination and who decides?",
                "How do I book an appointment?",
                "Where is the vaccine inventory managed?",
                "How do checkin queues work?"
        );

        for (String query : testQueries) {
            List<AssistantKnowledgeEntry> entries = AssistantKnowledgeCatalog.findRelevantEntries(
                    query,
                    Collections.emptySet(),
                    5
            );
            String context = AssistantKnowledgeContextBuilder.buildKnowledgeContext(entries, Collections.emptySet());

            assertThat(context).doesNotContain("/app/admin/staff-requests");
            assertThat(context).doesNotContain("/app/screening/eligibility");
            assertThat(context).doesNotContain("/app/appointments/book");
            assertThat(context).doesNotContain("/app/inventory");
            assertThat(context).doesNotContain("/app/checkin/queue");
        }
    }

    @Test
    @DisplayName("Anonymous staff-registration guidance explains approval is required but does not reveal admin route")
    void testAnonymousStaffRegistrationGuidanceHidesAdminRoute() {
        List<AssistantKnowledgeEntry> entries = AssistantKnowledgeCatalog.findRelevantEntries(
                "staff registration request approval",
                Collections.emptySet(),
                3
        );
        assertThat(entries).isNotEmpty();
        AssistantKnowledgeEntry staffReg = entries.stream()
                .filter(e -> e.id().equals("STAFF_REGISTRATION_REQUEST"))
                .findFirst()
                .orElseThrow();

        assertThat(staffReg.explanation()).contains("administrative review and approval");
        assertThat(staffReg.explanation()).doesNotContain("/app/admin/staff-requests");

        String context = AssistantKnowledgeContextBuilder.buildKnowledgeContext(entries, Collections.emptySet());
        assertThat(context).contains("/register/staff");
        assertThat(context).doesNotContain("/app/admin/staff-requests");
    }

    @Test
    @DisplayName("Eligibility querying: citizen receives /app/screening/eligibility while anonymous caller receives governance fact only")
    void testEligibilityQueryingRoleDifferences() {
        // 1. Anonymous caller
        List<AssistantKnowledgeEntry> anonEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "who decides eligibility am I eligible",
                Collections.emptySet(),
                3
        );
        assertThat(anonEntries).isNotEmpty();
        String anonContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(anonEntries, Collections.emptySet());
        assertThat(anonContext).contains("Final clinical eligibility is determined solely by an authorized Healthcare Officer");
        assertThat(anonContext).doesNotContain("/app/screening/eligibility");

        // 2. Citizen caller
        List<AssistantKnowledgeEntry> citizenEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "who decides eligibility am I eligible",
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                3
        );
        assertThat(citizenEntries).isNotEmpty();
        String citizenContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(citizenEntries, Set.of(AssistantRouteCatalog.ROLE_CITIZEN));
        assertThat(citizenContext).contains("Final clinical eligibility is determined solely by an authorized Healthcare Officer");
        assertThat(citizenContext).contains("/app/screening/eligibility");
    }

    @Test
    @DisplayName("Inventory route: Inventory Officer can receive /app/inventory, Citizen cannot")
    void testInventoryRouteRoleExclusion() {
        // Inventory officer
        List<AssistantKnowledgeEntry> invEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "inventory batch stock levels",
                Set.of(AssistantRouteCatalog.ROLE_INVENTORY_OFFICER),
                3
        );
        assertThat(invEntries).isNotEmpty();
        String invContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(invEntries, Set.of(AssistantRouteCatalog.ROLE_INVENTORY_OFFICER));
        assertThat(invContext).contains("/app/inventory");

        // Citizen
        List<AssistantKnowledgeEntry> citizenEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "inventory batch stock levels",
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                3
        );
        assertTrue(citizenEntries.stream().noneMatch(e -> e.id().equals("INVENTORY_MANAGEMENT")));
        String citizenContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(citizenEntries, Set.of(AssistantRouteCatalog.ROLE_CITIZEN));
        assertThat(citizenContext).doesNotContain("/app/inventory");
    }

    @Test
    @DisplayName("Internal evidenceReference values never enter model context for any catalog entry")
    void testEvidenceReferencesNeverEnterModelContext() {
        for (AssistantKnowledgeEntry entry : AssistantKnowledgeCatalog.ALL_ENTRIES) {
            if (entry.evidenceReference() != null && !entry.evidenceReference().isBlank()) {
                String context = AssistantKnowledgeContextBuilder.buildKnowledgeContext(
                        List.of(entry),
                        Set.of(AssistantRouteCatalog.ROLE_SUPER_ADMIN, AssistantRouteCatalog.ROLE_CITIZEN)
                );
                assertThat(context).doesNotContain(entry.evidenceReference());
            }
        }
    }

    @Test
    @DisplayName("Staff-only information is filtered strictly by assigned authority")
    void testStaffInformationFilteredByAuthority() {
        // Inventory officer receives inventory management
        List<AssistantKnowledgeEntry> invEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "vaccine batch stock adjustments and receipts",
                Set.of(AssistantRouteCatalog.ROLE_INVENTORY_OFFICER),
                3
        );
        assertThat(invEntries).isNotEmpty();
        assertEquals("INVENTORY_MANAGEMENT", invEntries.get(0).id());

        // Healthcare officer does NOT receive inventory management
        List<AssistantKnowledgeEntry> hcoInvEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "vaccine batch stock adjustments",
                Set.of(AssistantRouteCatalog.ROLE_HEALTHCARE_OFFICER),
                3
        );
        assertTrue(hcoInvEntries.stream().noneMatch(e -> e.id().equals("INVENTORY_MANAGEMENT")));

        // Healthcare officer receives screening review queue
        List<AssistantKnowledgeEntry> reviewEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "clinical screening review queue",
                Set.of(AssistantRouteCatalog.ROLE_HEALTHCARE_OFFICER),
                3
        );
        assertThat(reviewEntries).isNotEmpty();
        assertEquals("HEALTHCARE_OFFICER_SCREENING_REVIEW", reviewEntries.get(0).id());

        // Vaccinator receives vaccination drafts
        List<AssistantKnowledgeEntry> vaccinatorEntries = AssistantKnowledgeCatalog.findRelevantEntries(
                "record vaccination drafts administered dose",
                Set.of(AssistantRouteCatalog.ROLE_VACCINATOR),
                3
        );
        assertThat(vaccinatorEntries).isNotEmpty();
        assertEquals("VACCINATION_ADMINISTRATION_DRAFTS", vaccinatorEntries.get(0).id());
    }

    @Test
    @DisplayName("SUPER_ADMIN role expansion grants access to all staff modules, but not citizen-owned profile")
    void testSuperAdminRoleExpansion() {
        Set<String> superAdminRoles = Set.of(AssistantRouteCatalog.ROLE_SUPER_ADMIN);

        // Super Admin can access inventory
        List<AssistantKnowledgeEntry> inv = AssistantKnowledgeCatalog.findRelevantEntries("inventory stock", superAdminRoles, 3);
        assertTrue(inv.stream().anyMatch(e -> e.id().equals("INVENTORY_MANAGEMENT")));

        // Super Admin can access campaign administration
        List<AssistantKnowledgeEntry> camp = AssistantKnowledgeCatalog.findRelevantEntries("campaign management", superAdminRoles, 3);
        assertTrue(camp.stream().anyMatch(e -> e.id().equals("CAMPAIGN_MANAGEMENT")));

        // Super Admin can access screening review
        List<AssistantKnowledgeEntry> rev = AssistantKnowledgeCatalog.findRelevantEntries("screening review queue", superAdminRoles, 3);
        assertTrue(rev.stream().anyMatch(e -> e.id().equals("HEALTHCARE_OFFICER_SCREENING_REVIEW")));

        // Super Admin without ROLE_CITIZEN does NOT receive citizen private profile
        List<AssistantKnowledgeEntry> profile = AssistantKnowledgeCatalog.findRelevantEntries("my citizen profile update personal details", superAdminRoles, 3);
        assertTrue(profile.stream().noneMatch(e -> e.id().equals("CITIZEN_PROFILE")));
    }

    @Test
    @DisplayName("Screening eligibility entry explicitly affirms that only an authorized Healthcare Officer decides eligibility")
    void testScreeningEligibilityGovernanceAffirmation() {
        List<AssistantKnowledgeEntry> entries = AssistantKnowledgeCatalog.findRelevantEntries(
                "who decides eligibility am I eligible",
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                3
        );
        assertThat(entries).isNotEmpty();
        AssistantKnowledgeEntry entry = entries.stream()
                .filter(e -> e.id().equals("ELIGIBILITY_GOVERNANCE"))
                .findFirst()
                .orElseThrow();

        assertThat(entry.explanation()).contains("determined solely by an authorized Healthcare Officer");
        assertThat(entry.explanation()).contains("software and AI assistant do not infer, calculate, or approve medical eligibility");
    }

    @Test
    @DisplayName("Unknown topics produce empty entries and safe uncertainty guidance in context builder")
    void testUnknownTopicsProduceSafeContext() {
        List<AssistantKnowledgeEntry> empty = AssistantKnowledgeCatalog.findRelevantEntries(
                "how to bake sourdough bread in space",
                Collections.emptySet(),
                3
        );
        assertTrue(empty.isEmpty());

        String context = AssistantKnowledgeContextBuilder.buildKnowledgeContext(empty, Collections.emptySet());
        assertThat(context).contains(AssistantKnowledgeContextBuilder.DELIMITER_START);
        assertThat(context).contains("No specific knowledge catalogue topics were retrieved for this inquiry.");
        assertThat(context).contains("admit uncertainty clearly");
        assertThat(context).contains(AssistantKnowledgeContextBuilder.DELIMITER_END);
    }

    @Test
    @DisplayName("Knowledge context builder filters routes against caller authorities")
    void testContextBuilderRouteFiltering() {
        AssistantKnowledgeEntry testEntry = new AssistantKnowledgeEntry(
                "TEST_TOPIC",
                "TEST",
                "Test Topic",
                Set.of("test"),
                "This is test content.",
                true,
                Collections.emptySet(),
                List.of("/how-it-works", "/app/admin/staff-requests"),
                "test-evidence"
        );

        // Anonymous caller context: receives /how-it-works, but /app/admin/staff-requests is stripped!
        String anonContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(
                List.of(testEntry),
                Collections.emptySet()
        );
        assertThat(anonContext).contains("/how-it-works");
        assertThat(anonContext).doesNotContain("/app/admin/staff-requests");
        // Evidence reference must never be in the context
        assertThat(anonContext).doesNotContain("test-evidence");

        // Admin caller context: receives both /how-it-works and /app/admin/staff-requests
        String adminContext = AssistantKnowledgeContextBuilder.buildKnowledgeContext(
                List.of(testEntry),
                Set.of(AssistantRouteCatalog.ROLE_ADMIN)
        );
        assertThat(adminContext).contains("/how-it-works");
        assertThat(adminContext).contains("/app/admin/staff-requests");
    }

    @Test
    @DisplayName("Imperative mutation 'Book an appointment for me' remains refused by AssistantRefusalPolicy")
    void testMutationRequestRemainsRefusedByPolicy() {
        var refusal = AssistantRefusalPolicy.evaluatePolicy(
                "Book an appointment for me for tomorrow",
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                "req-mut"
        );
        assertTrue(refusal.isPresent());
        assertThat(refusal.get().answer()).contains("cannot perform actions");
    }

    @Test
    @DisplayName("Medical / clinical diagnosis questions still receive deterministic medical refusal")
    void testClinicalQuestionsStillRefused() {
        var refusal = AssistantRefusalPolicy.evaluatePolicy(
                "Can you diagnose my fever and tell me which vaccine to take?",
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                "req-clin"
        );
        assertTrue(refusal.isPresent());
        assertThat(refusal.get().answer()).contains("cannot provide medical advice, symptom diagnoses");
    }
}
