package com.se2030.vaccinationportal.campaign.session.planning.adapter;

import com.se2030.vaccinationportal.campaign.api.session.SessionChangeNotice;
import com.se2030.vaccinationportal.campaign.api.session.SessionChangeReader;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import com.se2030.vaccinationportal.campaign.session.planning.repository.SessionChangeEventRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Keeps MF-02 persistence details behind the campaign API boundary. */
@Component
public class SessionChangeReaderAdapter implements SessionChangeReader {
    private final SessionChangeEventRepository repository;
    private final TimeSlotRepository slots;

    public SessionChangeReaderAdapter(SessionChangeEventRepository repository, TimeSlotRepository slots) {
        this.repository = repository;
        this.slots = slots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionChangeNotice> findAllOrdered() {
        return repository.findAllByOrderByCreatedAtAscIdAsc().stream().map(this::toNotice).toList();
    }

    private SessionChangeNotice toNotice(SessionChangeEvent event) {
        return new SessionChangeNotice(
                event.getId(), event.getSession().getId(),
                slots.findBySessionId(event.getSession().getId()).stream()
                        .map(slot -> slot.getId()).sorted().toList(),
                event.getChangeType().name(),
                event.getOldSnapshot().getCenterId(), event.getNewSnapshot().getCenterId(),
                event.getOldSnapshot().getStartAt(), event.getNewSnapshot().getStartAt(),
                event.getReason(), event.getActorId(), event.getCreatedAt());
    }
}
