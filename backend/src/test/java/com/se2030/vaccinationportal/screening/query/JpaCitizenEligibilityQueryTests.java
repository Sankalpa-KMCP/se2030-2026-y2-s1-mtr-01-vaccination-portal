package com.se2030.vaccinationportal.screening.query;

import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.eligibility.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.eligibility.repository.EligibilityAssessmentRepository;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JpaCitizenEligibilityQueryTests {
    private static final Instant DEADLINE = Instant.parse("2026-01-01T12:00:00Z");
    private final UserPrincipal principal = UserPrincipal.fromClaims(42L, "citizen@example.com", List.of("ROLE_CITIZEN"));
    private final EligibilityAssessmentRepository assessments = mock(EligibilityAssessmentRepository.class);
    private final Clock clock = mock(Clock.class);
    private final AtomicReference<Instant> currentTime = new AtomicReference<>(DEADLINE.minusSeconds(1));
    private final JpaCitizenEligibilityQuery query = new JpaCitizenEligibilityQuery(assessments, clock);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenAnswer(ignored -> currentTime.get());
    }

    @Test
    void eligibilityExpiresWhenValidityCrossesDuringAssessmentRetrieval() {
        EligibilityAssessment assessment = assessment();
        when(assessments.findByCitizenIdOrderByReviewedAtDesc(42L)).thenAnswer(ignored -> {
            assertThat(currentTime.get()).isBefore(DEADLINE);
            verifyNoInteractions(clock);
            currentTime.set(DEADLINE.plusSeconds(1));
            return List.of(assessment);
        });

        assertExpired();
    }

    @Test
    void eligibilityComparisonTimeIsSampledAfterLatestPerCampaignReduction() {
        EligibilityAssessment assessment = mock(EligibilityAssessment.class);
        when(assessment.getStatus()).thenReturn(EligibilityAssessmentStatus.ELIGIBLE);
        when(assessment.getValidUntil()).thenReturn(DEADLINE);
        when(assessment.getCampaignId()).thenAnswer(ignored -> {
            currentTime.set(DEADLINE.plusSeconds(1));
            return 10L;
        });
        when(assessments.findByCitizenIdOrderByReviewedAtDesc(42L)).thenAnswer(ignored -> {
            assertThat(currentTime.get()).isBefore(DEADLINE);
            verifyNoInteractions(clock);
            return List.of(assessment);
        });

        assertExpired();
    }

    private void assertExpired() {
        assertThat(query.latestForCitizen(principal)).singleElement().satisfies(summary -> {
            assertThat(summary.campaignId()).isEqualTo(10L);
            assertThat(summary.recordedStatus()).isEqualTo("ELIGIBLE");
            assertThat(summary.lifecycleState()).isEqualTo("EXPIRED");
        });
        verify(clock, times(1)).instant();
    }

    private static EligibilityAssessment assessment() {
        EligibilityAssessment assessment = new EligibilityAssessment();
        assessment.setCampaignId(10L);
        assessment.setStatus(EligibilityAssessmentStatus.ELIGIBLE);
        assessment.setValidUntil(DEADLINE);
        return assessment;
    }
}
