package com.se2030.vaccinationportal.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.se2030.vaccinationportal.appointment.api.query.CitizenAppointmentQuery;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.assistant.tool.AssistantToolContext;
import com.se2030.vaccinationportal.assistant.tool.MyAppointmentsAssistantTool;
import com.se2030.vaccinationportal.assistant.tool.MyEligibilityAssistantTool;
import com.se2030.vaccinationportal.assistant.tool.MyStaffProfileAssistantTool;
import com.se2030.vaccinationportal.assistant.tool.MyStaffScheduleAssistantTool;
import com.se2030.vaccinationportal.campaign.api.staff.StaffContextQuery;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.session.planning.model.StaffAssignment;
import com.se2030.vaccinationportal.campaign.center.model.StaffRole;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.screening.api.query.CitizenEligibilityQuery;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssistantProviderQueryIntegrationTests {
    @Autowired private EntityManager entityManager;
    @Autowired private ApplicationContext context;
    @Autowired private CitizenAppointmentQuery appointments;
    @Autowired private CitizenEligibilityQuery eligibility;
    @Autowired private StaffContextQuery staff;
    @Autowired private MyAppointmentsAssistantTool appointmentsTool;
    @Autowired private MyEligibilityAssistantTool eligibilityTool;
    @Autowired private MyStaffProfileAssistantTool profileTool;
    @Autowired private MyStaffScheduleAssistantTool scheduleTool;

    @Test
    void realAppointmentToolUsesProviderScopedAppointmentsWaitlistAndCampaignFacts() {
        assertThat(context.getBeansOfType(CitizenAppointmentQuery.class)).hasSize(1);
        VaccinationCampaign campaign = campaign("A");
        VaccinationCenter center = center("A");
        Instant start = Instant.now().plusSeconds(86400);
        VaccinationSession session = persist(new VaccinationSession(campaign, center, start,
                start.plusSeconds(7200), start.minusSeconds(3600), "UTC"));
        TimeSlot slot = persist(new TimeSlot(session, start, start.plusSeconds(3600), 10));
        persist(new Appointment(501L, campaign.getId(), slot.getId()));
        persist(new Appointment(502L, campaign.getId(), slot.getId()));
        persist(new WaitlistEntry(501L, campaign.getId(), session.getId()));
        persist(new WaitlistEntry(502L, campaign.getId(), session.getId()));
        entityManager.flush();
        entityManager.clear();

        var result = appointmentsTool.execute(citizen(501L), "my appointments");
        assertThat(result.contextSnippet()).contains("Center: Center A", "Campaign: Campaign A", "Status: CONFIRMED", "Status: WAITING");
        assertThat(result.contextSnippet()).doesNotContain("502");
        assertThat(result.resultCount()).isEqualTo(2);
        assertThat(appointments.appointmentsForCitizen(citizen(999L).principal()).activeOrUpcoming()).isEmpty();
    }

    @Test
    void realEligibilityToolReadsOnlyTheSignedInCitizensLatestAssessment() {
        assertThat(context.getBeansOfType(CitizenEligibilityQuery.class)).hasSize(1);
        var questionnaire = persist(new Questionnaire("AQ", "Assistant questionnaire", null));
        var version = persist(new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.PUBLISHED));
        var consent = persist(new ConsentNoticeVersion("ASSISTANT", 1, "Consent", "Consent text",
                "hash", Instant.now().minusSeconds(3600)));
        ScreeningSubmission own = persist(new ScreeningSubmission(601L, 701L, version, consent, Instant.now()));
        ScreeningSubmission other = persist(new ScreeningSubmission(602L, 702L, version, consent, Instant.now()));
        persist(new EligibilityAssessment(own, 601L, 701L, 801L, EligibilityAssessmentStatus.ELIGIBLE,
                PreliminaryScreeningResult.PRELIMINARY_PASS, "approved", Instant.now(), Instant.now().plusSeconds(86400)));
        persist(new EligibilityAssessment(other, 602L, 702L, 801L, EligibilityAssessmentStatus.INELIGIBLE,
                PreliminaryScreeningResult.PRELIMINARY_REVIEW_REQUIRED, "private", Instant.now(), null));
        entityManager.flush();
        entityManager.clear();

        var result = eligibilityTool.execute(citizen(601L));
        assertThat(result.contextSnippet()).contains("Campaign ID: 701", "Recorded Status: ELIGIBLE");
        assertThat(result.contextSnippet()).doesNotContain("702", "private");
        assertThat(eligibility.latestForCitizen(citizen(999L).principal())).isEmpty();
    }

    @Test
    void realStaffToolsUseOnlyTheSignedInStaffRosterAndSchedule() {
        assertThat(context.getBeansOfType(StaffContextQuery.class)).hasSize(1);
        VaccinationCampaign campaign = campaign("S");
        VaccinationCenter center = center("S");
        Instant start = Instant.now().plusSeconds(86400);
        VaccinationSession session = persist(new VaccinationSession(campaign, center, start,
                start.plusSeconds(7200), start.minusSeconds(3600), "UTC"));
        OperationalStaffRoster own = persist(new OperationalStaffRoster(center, 901L, StaffRole.VACCINATOR));
        OperationalStaffRoster other = persist(new OperationalStaffRoster(center, 902L, StaffRole.SUPERVISOR));
        persist(new StaffAssignment(session, own, StaffRole.VACCINATOR, start, start.plusSeconds(3600)));
        persist(new StaffAssignment(session, other, StaffRole.SUPERVISOR, start.plusSeconds(3600), start.plusSeconds(7200)));
        entityManager.flush();
        entityManager.clear();

        var profile = profileTool.execute(staffContext(901L));
        var schedule = scheduleTool.execute(staffContext(901L));
        assertThat(profile.contextSnippet()).contains("Verified staff account ID: 901", "VACCINATOR", "Center S")
                .doesNotContain("SUPERVISOR", "902");
        assertThat(schedule.contextSnippet()).contains("Campaign S", "Center S", "VACCINATOR")
                .doesNotContain("SUPERVISOR", "902");
        assertThat(schedule.resultCount()).isEqualTo(1);
        assertThat(staff.scheduleForUser(staffContext(999L).principal(), Instant.now(), false)).isEmpty();
    }

    private VaccinationCampaign campaign(String suffix) {
        VaccineType type = persist(new VaccineType("ASST-VAX-" + suffix, "Assistant vaccine"));
        TargetGroup group = persist(new TargetGroup("ASST-GROUP-" + suffix, "Assistant group"));
        LocalDate from = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        return persist(new VaccinationCampaign("ASST-CAMP-" + suffix, "Campaign " + suffix, null,
                type, group, from, from.plusDays(30), Instant.now(), Instant.now().plusSeconds(86400 * 25)));
    }

    private VaccinationCenter center(String suffix) {
        return persist(new VaccinationCenter("ASST-CENTER-" + suffix, "Center " + suffix,
                "Test road", "test@example.test", CenterStatus.ACTIVE, 10, 10, "UTC"));
    }

    private static AssistantToolContext citizen(Long id) {
        return new AssistantToolContext("query-test", UserPrincipal.fromClaims(id, "citizen@example.test",
                List.of("ROLE_CITIZEN")), Set.of("ROLE_CITIZEN"));
    }

    private static AssistantToolContext staffContext(Long id) {
        return new AssistantToolContext("query-test", UserPrincipal.fromClaims(id, "staff@example.test",
                List.of("ROLE_HEALTHCARE_OFFICER")), Set.of("ROLE_HEALTHCARE_OFFICER"));
    }

    private <T> T persist(T value) {
        entityManager.persist(value);
        entityManager.flush();
        return value;
    }
}
