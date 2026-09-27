package com.se2030.vaccinationportal.assistant.knowledge;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic knowledge catalog containing verified operational documentation across MF-01 through MF-05.
 * Strictly enforces role-aware knowledge retrieval to ensure callers receive only verified workflows
 * appropriate to their assigned authorities.
 */
public final class AssistantKnowledgeCatalog {

    private AssistantKnowledgeCatalog() {
        // utility
    }

    public static final List<AssistantKnowledgeEntry> ALL_ENTRIES;

    static {
        List<AssistantKnowledgeEntry> list = new ArrayList<>();

        // -------------------------------------------------------------
        // MF-01 & SHARED AUTHENTICATION
        // -------------------------------------------------------------
        list.add(new AssistantKnowledgeEntry(
                "CITIZEN_REGISTRATION",
                "MF-01",
                "Citizen Account Registration",
                Set.of("register", "registration", "citizen sign up", "create account", "new citizen", "register as citizen"),
                "Citizens register self-service accounts by providing full name, National Identity Card (NIC), email, mobile number, and password. Once registered, accounts are immediately active and can sign in.",
                true,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/register/citizen", "/login"),
                "src/auth/pages/CitizenRegistrationPage.jsx / CitizenRegistrationController"
        ));

        list.add(new AssistantKnowledgeEntry(
                "STAFF_REGISTRATION_REQUEST",
                "MF-01",
                "Staff Registration Request & Access Approval",
                Set.of("staff registration", "staff access", "staff request", "register staff", "pending request", "approval"),
                "Operational and medical staff submit access requests with professional credentials and desired roles. Staff requests require administrative review and approval before staff accounts can authenticate.",
                true,
                Set.of(AssistantRouteCatalog.ROLE_ADMIN),
                List.of("/register/staff", "/app/admin/staff-requests"),
                "src/auth/pages/StaffRegistrationRequestPage.jsx / StaffRegistrationRequestController"
        ));

        list.add(new AssistantKnowledgeEntry(
                "CREDENTIAL_RECOVERY_AND_VERIFICATION",
                "MF-01",
                "Email Verification & Password Recovery",
                Set.of("verify email", "forgot password", "reset password", "password recovery", "recover account"),
                "Users verify registered email addresses using security tokens. Account password recovery issues a time-bounded reset token that allows setting a new password.",
                true,
                Collections.emptySet(),
                List.of("/forgot-password", "/reset-password", "/verify-email"),
                "src/auth/pages/ForgotPasswordPage.jsx / ResetPasswordPage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "CITIZEN_PROFILE",
                "MF-01",
                "Citizen Profile Management",
                Set.of("profile", "my profile", "update details", "phone number", "personal information", "view profile"),
                "Authenticated citizens can view and update contact details, address, and profile information.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/citizen/profile"),
                "src/features/citizen-profile/pages/CitizenProfilePage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "ADMIN_USER_MANAGEMENT",
                "MF-01",
                "Administrative User & Access Control",
                Set.of("admin", "manage users", "staff requests", "approve staff", "system administration"),
                "System administrators manage user accounts, review pending staff access requests, and enforce role assignments.",
                false,
                AssistantRouteCatalog.ADMIN_ROLES,
                List.of("/app/admin", "/app/admin/staff-requests"),
                "src/auth/pages/AdminDashboardPage.jsx / PendingStaffRequestsPage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "STAFF_ONBOARDING",
                "SHARED",
                "Staff Getting Started Guide",
                Set.of("new staff", "staff dashboard", "getting started", "my role", "what can i do", "staff help", "staff guide"),
                "Signed-in staff can use the Staff Dashboard as their starting point. The assistant can explain role-authorized workflows, show the verified account's staff profile and center-roster association, and summarize current or upcoming session assignments. It remains read-only and cannot approve, assign, check in, or alter records.",
                false,
                AssistantRouteCatalog.STAFF_DASHBOARD_ROLES,
                List.of("/app/staff"),
                "src/auth/pages/StaffDashboardPage.jsx / assistant read-only staff tools"
        ));

        // -------------------------------------------------------------
        // MF-02: CAMPAIGN, CENTER & SESSION MANAGEMENT
        // -------------------------------------------------------------
        list.add(new AssistantKnowledgeEntry(
                "PUBLIC_PORTAL_OVERVIEW",
                "MF-02",
                "Public Vaccination Program & Portal Guide",
                Set.of("how it works", "about portal", "program overview", "vaccination guide", "public info"),
                "Visitors can explore the national vaccination program workflow, governance rules, and portal architecture.",
                true,
                Collections.emptySet(),
                List.of("/how-it-works", "/about"),
                "src/features/public-landing/pages/HowItWorksPage.jsx / AboutPage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "CAMPAIGN_MANAGEMENT",
                "MF-02",
                "Vaccination Campaign Administration",
                Set.of("campaign", "campaigns", "dose programme", "target group", "campaign status", "active campaigns"),
                "Campaign officers and managers create and oversee vaccination campaigns. Campaigns define target demographics, dose programmes, and geographic targets, with statuses spanning DRAFT, ACTIVE, PAUSED, and COMPLETED.",
                false,
                AssistantRouteCatalog.CAMPAIGN_VIEW_ROLES,
                List.of("/app/campaigns"),
                "src/features/campaign-management/pages/CampaignManagementPage.jsx / CampaignController"
        ));

        list.add(new AssistantKnowledgeEntry(
                "CENTER_OPERATIONS",
                "MF-02",
                "Vaccination Center Operations",
                Set.of("center", "centers", "center operations", "operating hours", "center closures", "occupancy ceiling"),
                "Authorized center managers manage physical vaccination centers, operating hours, scheduled closures, and maximum occupancy thresholds.",
                false,
                AssistantRouteCatalog.CENTER_VIEW_ROLES,
                List.of("/app/center-operations"),
                "src/features/center-operations/pages/CenterOperationsPage.jsx / CenterController"
        ));

        list.add(new AssistantKnowledgeEntry(
                "SESSION_MANAGEMENT",
                "MF-02",
                "Session Scheduling & Time Slots",
                Set.of("session", "sessions", "time slots", "slot capacity", "recurrence", "session readiness"),
                "Session managers configure vaccination sessions and generate recurring time slot series. Operational readiness checks verify center availability and staff rostering before sessions are opened for booking.",
                false,
                AssistantRouteCatalog.SESSION_VIEW_ROLES,
                List.of("/app/sessions"),
                "src/features/session-management/pages/SessionsPage.jsx / SessionController"
        ));

        list.add(new AssistantKnowledgeEntry(
                "OPERATIONAL_ANALYTICS",
                "MF-02",
                "Operational Analytics & Metrics",
                Set.of("analytics", "metrics", "reports", "operational reporting", "turnout", "performance"),
                "Supervisors and campaign officers inspect operational vaccination metrics, appointment utilization, and campaign performance dashboards.",
                false,
                AssistantRouteCatalog.ANALYTICS_VIEW_ROLES,
                List.of("/app/analytics"),
                "src/features/analytics/components/OperationalAnalyticsView.jsx"
        ));

        // -------------------------------------------------------------
        // SCREENING & MEDICAL ELIGIBILITY (MF-01 / CLINICAL GOVERNANCE)
        // -------------------------------------------------------------
        list.add(new AssistantKnowledgeEntry(
                "SCREENING_SUBMISSION",
                "SCREENING",
                "Citizen Pre-Vaccination Screening",
                Set.of("screening", "pre-screening", "screening questionnaire", "submit screening", "medical questionnaire", "health questionnaire"),
                "Citizens complete mandatory pre-vaccination screening questionnaires for active campaigns. Questionnaires gather preliminary medical intake answers required before booking.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/screening"),
                "src/features/screening/pages/CitizenScreeningCampaignsPage.jsx / CitizenScreeningPage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "SCREENING_HISTORY_AND_CORRECTION",
                "SCREENING",
                "Screening History & Answer Correction",
                Set.of("screening history", "correct screening", "update answers", "resubmit screening", "screening status"),
                "Citizens inspect past questionnaire submissions. If an intake submission requires clarification or remains pending review, citizens can submit corrections.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/screening/history"),
                "src/features/screening/pages/CitizenScreeningHistoryPage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "HEALTHCARE_OFFICER_SCREENING_REVIEW",
                "SCREENING",
                "Clinical Eligibility Review Queue",
                Set.of("screening review", "review queue", "clinical review", "evaluate screening", "officer review"),
                "Healthcare Officers evaluate submitted screening questionnaires in the clinical review queue. Officers assess clinical safety, request corrections, or issue medical eligibility clearances.",
                false,
                AssistantRouteCatalog.SCREENING_STAFF_ROLES,
                List.of("/app/staff/screening/reviews"),
                "src/features/screening/pages/HealthcareOfficerScreeningReviewQueuePage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "ELIGIBILITY_GOVERNANCE",
                "SCREENING",
                "Clinical Eligibility Determination Governance",
                Set.of("eligibility", "am i eligible", "who decides eligibility", "eligibility check", "medical clearance"),
                "Final clinical eligibility is determined solely by an authorized Healthcare Officer following professional review of screening answers. The portal software and AI assistant do not infer, calculate, or approve medical eligibility. Citizens can view their verified eligibility status once reviewed.",
                true,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/screening/eligibility"),
                "ScreeningSubmissionService / EligibilityReviewService"
        ));

        // -------------------------------------------------------------
        // MF-03: APPOINTMENT, WAITLIST & CHECK-IN MANAGEMENT
        // -------------------------------------------------------------
        list.add(new AssistantKnowledgeEntry(
                "APPOINTMENT_BOOKING",
                "MF-03",
                "Appointment Booking Workflow",
                Set.of("book", "booking", "book appointment", "reserve slot", "schedule appointment", "how do i book", "how to book"),
                "Citizens can book vaccination appointments once an eligible screening review is cleared. Slots are allocated with concurrency-safe atomic capacity checks preventing overbooking. If a citizen holds an active waitlist entry for the campaign, they must explicitly leave the waitlist before booking directly.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/appointments/book"),
                "src/features/appointment-management/pages/AppointmentBookingPage.jsx / AppointmentBookingService"
        ));

        list.add(new AssistantKnowledgeEntry(
                "APPOINTMENT_MANAGEMENT",
                "MF-03",
                "Appointment Management, Rescheduling & Cancellation",
                Set.of("my appointments", "cancel appointment", "reschedule", "change appointment", "view appointments", "24 hours notice"),
                "Citizens can view their scheduled appointments. Cancellations and atomic rescheduling require at least 24 hours notice before the scheduled slot start time.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/appointments/mine"),
                "src/features/appointment-management/pages/MyAppointmentsPage.jsx / AppointmentBookingService"
        ));

        list.add(new AssistantKnowledgeEntry(
                "WAITLIST_MANAGEMENT",
                "MF-03",
                "Session Waitlist Queue",
                Set.of("waitlist", "full session", "join waitlist", "queue", "waitlist position"),
                "When all time slots in a vaccination session are completely full, eligible citizens can join the session waitlist. If capacity opens due to cancellation, waitlisted citizens are promoted in sequence. A citizen holding an active waitlist entry cannot directly book an appointment for that campaign without first leaving the waitlist.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/appointments/waitlist"),
                "src/features/appointment-management/pages/WaitlistPage.jsx / WaitlistService"
        ));

        list.add(new AssistantKnowledgeEntry(
                "STAFF_CHECK_IN",
                "MF-03",
                "Center Check-In Queue Management",
                Set.of("check-in", "check in queue", "patient arrival", "reversal", "mark arrived"),
                "Vaccination center staff and vaccinators manage patient check-in at the center. Arriving citizens are verified against booked appointments, and check-ins can be reversed if necessary.",
                false,
                AssistantRouteCatalog.CHECK_IN_ROLES,
                List.of("/app/checkin/queue"),
                "src/features/appointment-management/pages/CheckInManagementPage.jsx / AppointmentController"
        ));

        // -------------------------------------------------------------
        // MF-04: VACCINE INVENTORY & BATCH MANAGEMENT
        // -------------------------------------------------------------
        list.add(new AssistantKnowledgeEntry(
                "INVENTORY_MANAGEMENT",
                "MF-04",
                "Vaccine Batch & Center Inventory Management",
                Set.of("inventory", "stock", "batches", "manufacturers", "stock adjustments", "stock receipts"),
                "Inventory officers and administrators oversee vaccine manufacturers, batch lot numbers, center stock levels, receipt logs, and adjustment transactions. Real-time stock counts are restricted to authorized personnel.",
                false,
                AssistantRouteCatalog.INVENTORY_ROLES,
                List.of("/app/inventory"),
                "src/features/inventory-management/pages/InventoryManagementPage.jsx / InventoryManagementController"
        ));

        // -------------------------------------------------------------
        // MF-05: VACCINATION ADMINISTRATION & CERTIFICATES
        // -------------------------------------------------------------
        list.add(new AssistantKnowledgeEntry(
                "VACCINATION_ADMINISTRATION_DRAFTS",
                "MF-05",
                "Vaccination Administration & Finalization",
                Set.of("vaccination drafts", "record dose", "administer vaccine", "drafts", "finalize vaccination"),
                "Authorized vaccinators record clinical administration details as drafts. Upon confirmation, records are finalized and locked against tampering, triggering certificate generation.",
                false,
                AssistantRouteCatalog.VACCINATOR_ROLES,
                List.of("/app/staff/vaccination-drafts"),
                "src/features/vaccination-administration/pages/VaccinationDraftsPage.jsx / VaccinationDraftController"
        ));

        list.add(new AssistantKnowledgeEntry(
                "CITIZEN_VACCINATION_RECORDS",
                "MF-05",
                "Citizen Vaccination History & Records",
                Set.of("vaccination history", "my doses", "vaccination records", "doses received", "past vaccines"),
                "Citizens can view their finalized vaccination records, dose numbers, dates, and administered vaccine products.",
                false,
                Set.of(AssistantRouteCatalog.ROLE_CITIZEN),
                List.of("/app/citizen/vaccinations"),
                "src/features/vaccination-administration/pages/CitizenVaccinationsPage.jsx"
        ));

        list.add(new AssistantKnowledgeEntry(
                "DIGITAL_CERTIFICATE_VERIFICATION",
                "MF-05",
                "Public Digital Certificate Verification",
                Set.of("certificate", "verify certificate", "digital certificate", "qr code", "certificate token"),
                "Public visitors and verification authorities can verify issued vaccination certificates using a unique verification token or QR reference. The portal returns tamper-verified confirmation without exposing private medical history.",
                true,
                Collections.emptySet(),
                List.of("/verify-certificate"),
                "src/features/vaccination-administration/pages/PublicCertificateVerificationPage.jsx / DigitalCertificateController"
        ));

        ALL_ENTRIES = Collections.unmodifiableList(list);
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
            "at", "by", "for", "with", "about", "against", "between", "into",
            "through", "during", "before", "after", "above", "below", "to", "from",
            "up", "down", "in", "out", "on", "off", "over", "under", "again",
            "further", "once", "here", "there", "where", "why", "how",

            "all", "any", "both", "each", "few", "more", "most", "other", "some",
            "such", "no", "nor", "not", "only", "own", "same", "so", "than",
            "too", "very", "s", "t", "can", "will", "just", "don", "should",
            "now", "do", "does", "did", "doing", "i", "my", "me", "we", "our",
            "you", "your", "he", "she", "it", "they", "them", "what", "which", "is"
    );

    /**
     * Deterministically finds and ranks relevant knowledge entries for the caller.
     *
     * @param query User's inquiry text
     * @param callerAuthorities Raw authorities held by the caller
     * @param maxResults Maximum number of entries to return (typically 2-3)
     * @return Deterministically ordered list of relevant authorized knowledge entries
     */
    public static List<AssistantKnowledgeEntry> findRelevantEntries(
            String query,
            Set<String> callerAuthorities,
            int maxResults
    ) {
        if (query == null || query.isBlank() || maxResults <= 0) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT).trim();
        List<String> queryTokens = Arrays.stream(normalizedQuery.split("[^a-zA-Z0-9]+"))
                .filter(token -> !token.isBlank() && !STOP_WORDS.contains(token))
                .toList();

        Set<String> expandedAuthorities = AssistantRouteCatalog.expandAuthorities(callerAuthorities);
        boolean isAnonymous = expandedAuthorities.isEmpty();

        List<ScoredEntry> scoredList = new ArrayList<>();

        for (AssistantKnowledgeEntry entry : ALL_ENTRIES) {
            // Role-based visibility enforcement:
            // 1. Anonymous callers can ONLY receive public entries.
            if (isAnonymous && !entry.isPublic()) {
                continue;
            }

            // 2. Authenticated callers receive public entries OR entries permitted for their authorities.
            if (!isAnonymous) {
                boolean hasRoleAccess = entry.isPublic()
                        || entry.allowedRoles().stream().anyMatch(expandedAuthorities::contains);
                if (!hasRoleAccess) {
                    continue;
                }
            }

            int score = calculateRelevanceScore(entry, normalizedQuery, queryTokens);
            if (score > 0) {
                scoredList.add(new ScoredEntry(entry, score));
            }
        }

        // Sort deterministically:
        // 1. Relevance score descending
        // 2. Stable entry ID ascending (tie-breaking)
        scoredList.sort(Comparator
                .comparingInt(ScoredEntry::score).reversed()
                .thenComparing(e -> e.entry().id()));

        return scoredList.stream()
                .limit(maxResults)
                .map(ScoredEntry::entry)
                .toList();
    }

    private static int calculateRelevanceScore(
            AssistantKnowledgeEntry entry,
            String normalizedQuery,
            List<String> queryTokens
    ) {
        int score = 0;

        // Exact phrase matching in keywords (high weight)
        for (String keyword : entry.keywords()) {
            String lowerKw = keyword.toLowerCase(Locale.ROOT);
            if (normalizedQuery.contains(lowerKw)) {
                score += 8;
            } else {
                // Partial token overlap in keywords
                for (String token : queryTokens) {
                    if (lowerKw.contains(token)) {
                        score += 2;
                    }
                }
            }
        }

        // Title overlap
        String lowerTitle = entry.title().toLowerCase(Locale.ROOT);
        for (String token : queryTokens) {
            if (lowerTitle.contains(token)) {
                score += 3;
            }
        }

        return score;
    }

    private record ScoredEntry(AssistantKnowledgeEntry entry, int score) {
    }
}
