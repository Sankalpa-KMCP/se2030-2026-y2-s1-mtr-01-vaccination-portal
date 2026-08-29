package com.se2030.vaccinationportal.screening.integration;

import com.se2030.vaccinationportal.appointment.dependency.EligibilityLookupPort;
import com.se2030.vaccinationportal.appointment.dependency.EligibilityStatus;
import com.se2030.vaccinationportal.screening.service.EligibilityReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class Mf01EligibilityLookupAdapterTests {

    private static final Long CITIZEN_ID = 101L;
    private static final Long CAMPAIGN_ID = 202L;

    @Autowired
    private EligibilityLookupPort eligibilityLookupPort;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private EligibilityReviewService eligibilityReviewService;

    @Test
    void currentlyEligibleCitizenMapsToEligible() {
        when(eligibilityReviewService.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(true);

        EligibilityStatus result = eligibilityLookupPort.getCurrentEligibility(CITIZEN_ID, CAMPAIGN_ID);

        assertThat(result).isEqualTo(EligibilityStatus.ELIGIBLE);
        verify(eligibilityReviewService).isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID);
    }

    @Test
    void ineligibleCitizenMapsToNotEligible() {
        assertNotEligibleWhenMf01DecisionIsFalse();
    }

    @Test
    void revokedAssessmentDecisionMapsToNotEligible() {
        assertNotEligibleWhenMf01DecisionIsFalse();
    }

    @Test
    void expiredAssessmentDecisionMapsToNotEligible() {
        assertNotEligibleWhenMf01DecisionIsFalse();
    }

    @Test
    void campaignEndDateDecisionMapsToNotEligible() {
        assertNotEligibleWhenMf01DecisionIsFalse();
    }

    @Test
    void applicationContextProvidesExactlyOneProductionEligibilityLookupPortBean() {
        assertThat(applicationContext.getBeansOfType(EligibilityLookupPort.class))
                .hasSize(1)
                .containsValue(eligibilityLookupPort);
        assertThat(eligibilityLookupPort).isInstanceOf(Mf01EligibilityLookupAdapter.class);
    }

    private void assertNotEligibleWhenMf01DecisionIsFalse() {
        when(eligibilityReviewService.isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID))
                .thenReturn(false);

        EligibilityStatus result = eligibilityLookupPort.getCurrentEligibility(CITIZEN_ID, CAMPAIGN_ID);

        assertThat(result).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
        verify(eligibilityReviewService).isCitizenCurrentlyEligible(CITIZEN_ID, CAMPAIGN_ID);
    }
}
