package com.se2030.vaccinationportal.campaign.api.appointment;

import java.util.List;

/** Campaign-owned schedule facts used when presenting appointments. */
public interface TimeSlotDisplayReader {
    List<TimeSlotDisplaySnapshot> findByIds(List<Long> slotIds);
}
