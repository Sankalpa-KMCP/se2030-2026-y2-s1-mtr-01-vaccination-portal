package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.EligibilityAssessmentResponse;
import com.se2030.vaccinationportal.screening.model.EligibilityAssessment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EligibilityAssessmentMapper {

    public EligibilityAssessmentResponse toResponse(EligibilityAssessment assessment) {
        return new EligibilityAssessmentResponse(
                assessment.getId(),
                assessment.getSubmission().getId(),
                assessment.getCitizenId(),
                assessment.getCampaignId(),
                assessment.getReviewerId(),
                assessment.getStatus(),
                assessment.getPreliminaryResult(),
                assessment.getReason(),
                assessment.getClinicalNotes(),
                assessment.getReviewedAt(),
                assessment.getValidUntil(),
                assessment.getRevokedAt(),
                assessment.getRevocationReason(),
                assessment.getRescreeningAuthorizedAt(),
                assessment.getRescreeningAuthorizedBy(),
                assessment.getRescreeningReason(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
        );
    }

    public List<EligibilityAssessmentResponse> toResponses(List<EligibilityAssessment> assessments) {
        return assessments.stream().map(this::toResponse).toList();
    }
}
