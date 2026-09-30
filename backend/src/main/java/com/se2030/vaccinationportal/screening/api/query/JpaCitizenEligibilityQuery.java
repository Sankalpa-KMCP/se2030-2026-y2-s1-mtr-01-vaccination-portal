package com.se2030.vaccinationportal.screening.api.query;

import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessmentStatus;
import com.se2030.vaccinationportal.screening.repository.EligibilityAssessmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class JpaCitizenEligibilityQuery implements CitizenEligibilityQuery {
    private final EligibilityAssessmentRepository assessments;
    private final Clock clock;

    public JpaCitizenEligibilityQuery(EligibilityAssessmentRepository assessments, Clock clock) {
        this.assessments = assessments;
        this.clock = clock;
    }

    @Override
    public List<EligibilitySummary> latestForCitizen(UserPrincipal principal) {
        if (principal == null) throw new IllegalArgumentException("Authenticated citizen principal is required");
        Long citizenId = principal.getUserId();
        List<EligibilityAssessment> all = assessments.findByCitizenIdOrderByReviewedAtDesc(citizenId);
        if (all == null || all.isEmpty()) return List.of();
        Map<Long, EligibilityAssessment> latest = new LinkedHashMap<>();
        for (EligibilityAssessment assessment : all) {
            latest.putIfAbsent(assessment.getCampaignId(), assessment);
            if (latest.size() >= 5) break;
        }
        // Preserve lifecycle evaluation after retrieval and latest-per-campaign reduction.
        Instant now = clock.instant();
        return latest.values().stream().map(a -> {
            EligibilityAssessmentStatus status = a.getStatus();
            boolean revoked = status == EligibilityAssessmentStatus.REVOKED || a.getRevokedAt() != null;
            boolean expired = status == EligibilityAssessmentStatus.EXPIRED
                    || (a.getValidUntil() != null && now.isAfter(a.getValidUntil()));
            String lifecycle = revoked ? "REVOKED" : (expired ? "EXPIRED" : "ACTIVE");
            return new EligibilitySummary(a.getCampaignId(), status == null ? "UNKNOWN" : status.name(),
                    a.getReviewedAt(), a.getValidUntil(), lifecycle);
        }).toList();
    }
}
