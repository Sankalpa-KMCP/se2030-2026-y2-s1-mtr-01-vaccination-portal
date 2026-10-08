package com.se2030.vaccinationportal.campaign.session.planning.policy;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Checks campaign intake for promotion without reapplying direct-booking restrictions. */
@Component("waitlistPromotionBookabilityStrategy")
public class WaitlistPromotionBookabilityStrategy implements SlotBookabilityStrategy {
    @Override
    public SlotBookability evaluate(TimeSlot slot, Instant now) {
        VaccinationCampaign campaign = slot.getSession().getCampaign();
        if (!campaign.getStatus().isIntakePermitted()) {
            return SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN;
        }
        return SlotBookability.BOOKABLE;
    }
}
