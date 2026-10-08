package com.se2030.vaccinationportal.campaign.session.planning.adapter;

import com.se2030.vaccinationportal.campaign.api.appointment.TimeSlotDisplayReader;
import com.se2030.vaccinationportal.campaign.api.appointment.TimeSlotDisplaySnapshot;


import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adapts slot and session data into display snapshots for appointment workflows. */
@Component
public class JpaTimeSlotDisplayReader implements TimeSlotDisplayReader {
    private final TimeSlotRepository slots;

    public JpaTimeSlotDisplayReader(TimeSlotRepository slots) {
        this.slots = slots;
    }

    @Override
    public List<TimeSlotDisplaySnapshot> findByIds(List<Long> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) return List.of();
        var found = slots.findByIdIn(slotIds);
        if (found == null) return List.of();
        return found.stream().map(slot -> {
            VaccinationSession session = slot.getSession();
            return new TimeSlotDisplaySnapshot(slot.getId(), slot.getStartTime(), slot.getEndTime(),
                    session == null || session.getCampaign() == null ? null : session.getCampaign().getName(),
                    session == null || session.getCenter() == null ? null : session.getCenter().getName(),
                    session == null || session.getCenter() == null ? null : session.getCenter().getAddress());
        }).toList();
    }
}
