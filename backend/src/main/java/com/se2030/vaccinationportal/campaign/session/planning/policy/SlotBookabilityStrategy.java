package com.se2030.vaccinationportal.campaign.session.planning.policy;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import java.time.Instant;

/** Read-only policy for a slot whose session and campaign have been resolved by the context. */
public interface SlotBookabilityStrategy {
    SlotBookability evaluate(TimeSlot slot, Instant now);
}
