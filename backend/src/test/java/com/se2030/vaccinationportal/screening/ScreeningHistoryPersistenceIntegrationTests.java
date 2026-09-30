package com.se2030.vaccinationportal.screening;

import com.se2030.vaccinationportal.screening.questionnaire.model.AnswerOption;
import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireItem;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.PreliminaryScreeningResult;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmissionStatus;


import com.se2030.vaccinationportal.campaign.questionnaire.model.CampaignQuestionnaireAssignment;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.screening.query.service.ScreeningHistoryService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** H2-only fixtures roll back; no local MySQL data or production seeds are involved. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScreeningHistoryPersistenceIntegrationTests {
    @Autowired private EntityManager entityManager;
    @Autowired private ScreeningHistoryService historyService;

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void persistedHistoricalAnswersSurviveCampaignReassignmentAndVersionRetirement() {
        UserPrincipal principal = UserPrincipal.fromClaims(6L, "history-fixture@portal.test", List.of("ROLE_CITIZEN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        VaccineType vaccine = persist(new VaccineType("HISTORY-VAX", "History vaccine fixture", "dose", true));
        TargetGroup target = persist(new TargetGroup("HISTORY-TARGET", "History fixture", true));
        VaccinationCampaign campaign = persist(new VaccinationCampaign(
                "HISTORY-CAMPAIGN", "Historical campaign", null, vaccine, target,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1),
                Instant.parse("2024-12-01T00:00:00Z"), Instant.parse("2025-01-31T00:00:00Z")));
        Questionnaire questionnaire = persist(new Questionnaire("HISTORY", "History questionnaire", null));
        QuestionnaireVersion original = persist(new QuestionnaireVersion(questionnaire, 1, QuestionnaireVersionStatus.PUBLISHED));
        QuestionnaireVersion replacement = persist(new QuestionnaireVersion(questionnaire, 2, QuestionnaireVersionStatus.PUBLISHED));
        ScreeningQuestion oldQuestion = persist(new ScreeningQuestion("HISTORY-OLD", "Original choice question?", QuestionType.SINGLE_CHOICE, true, true));
        ScreeningQuestion newQuestion = persist(new ScreeningQuestion("HISTORY-NEW", "Replacement question?", QuestionType.TEXT, true, true));
        persist(new QuestionnaireItem(original, oldQuestion, 4, null));
        persist(new QuestionnaireItem(replacement, newQuestion, 1, null));
        persist(new AnswerOption(oldQuestion, "ORIGINAL", "Original selected label", "original-value", 1));
        ConsentNoticeVersion consent = persist(new ConsentNoticeVersion("HISTORY-CONSENT", 1,
                "Fixture consent", "Fixture text", "fixture-hash", Instant.parse("2024-12-01T00:00:00Z")));
        ScreeningSubmission submission = new ScreeningSubmission(6L, campaign.getId(), original, consent,
                Instant.parse("2025-01-01T10:00:00Z"));
        submission.setStatus(ScreeningSubmissionStatus.PENDING_REVIEW);
        submission.setPreliminaryResult(PreliminaryScreeningResult.PRELIMINARY_PASS);
        submission.setSubmittedAt(Instant.parse("2025-01-01T10:00:00Z"));
        submission.addAnswer(new ScreeningAnswer(submission, oldQuestion, "original-value"));
        persist(submission);
        CampaignQuestionnaireAssignment originalAssignment = persist(new CampaignQuestionnaireAssignment(
                campaign, original.getId(), LocalDate.of(2024, 12, 1), null));
        Long submissionId = submission.getId();
        Long originalId = original.getId();
        Long replacementId = replacement.getId();
        Long assignmentId = originalAssignment.getId();
        Long campaignId = campaign.getId();
        entityManager.flush();
        entityManager.clear();

        var before = historyService.getHistoryDetails(submissionId);
        entityManager.find(CampaignQuestionnaireAssignment.class, assignmentId).closeAt(LocalDate.of(2025, 1, 2));
        persist(new CampaignQuestionnaireAssignment(entityManager.find(VaccinationCampaign.class, campaignId),
                replacementId, LocalDate.of(2025, 1, 2), null));
        QuestionnaireVersion retired = entityManager.find(QuestionnaireVersion.class, originalId);
        retired.setStatus(QuestionnaireVersionStatus.RETIRED);
        retired.setRetiredAt(Instant.parse("2025-01-02T00:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        var after = historyService.getHistoryDetails(submissionId);
        entityManager.clear();
        assertThat(after).isEqualTo(before);
        assertThat(after.questionnaireVersionId()).isEqualTo(originalId).isNotEqualTo(replacementId);
        assertThat(after.questionnaireVersionNumber()).isEqualTo(1);
        assertThat(after.answers()).singleElement().satisfies(answer -> {
            assertThat(answer.questionText()).isEqualTo("Original choice question?");
            assertThat(answer.questionCode()).isEqualTo("HISTORY-OLD");
            assertThat(answer.displayOrder()).isEqualTo(4);
            assertThat(answer.typedAnswer()).isEqualTo("original-value");
            assertThat(answer.displayAnswer()).isEqualTo("Original selected label");
        });
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
