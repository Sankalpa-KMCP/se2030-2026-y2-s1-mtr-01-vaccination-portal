package com.se2030.vaccinationportal.campaign.session.planning.policy;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Checks direct-booking rules in the order used to choose the rejection reason. */
@Component("directBookingBookabilityStrategy")
public class DirectBookingBookabilityStrategy implements SlotBookabilityStrategy {
    @Override
    public SlotBookability evaluate(TimeSlot slot, Instant now) {
        VaccinationSession session = slot.getSession();
        VaccinationCampaign campaign = session.getCampaign();
        if (!campaign.isBookingWindowOpenAt(now)) {
            return SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN;
        }
        if (session.isBookingSuspended()) {
            return SlotBookability.SESSION_BOOKING_SUSPENDED;
        }
        if (!now.isBefore(session.getBookingCloseAt())) {
            return SlotBookability.SESSION_BOOKING_CLOSED;
        }
        if (!slot.getStartTime().isAfter(now)) {
            return SlotBookability.SLOT_IN_PAST;
        }
        if (!slot.hasAvailableCapacity()) {
            return SlotBookability.NO_CAPACITY;
        }
        return SlotBookability.BOOKABLE;
    }
}
