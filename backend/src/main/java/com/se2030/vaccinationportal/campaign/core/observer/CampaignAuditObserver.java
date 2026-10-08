package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLifecycleEventRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Concrete Observer. Persists audit history within the campaign service's transaction. */
@Component
public class CampaignAuditObserver implements CampaignLifecycleObserver {
    private final CampaignLifecycleEventRepository eventRepository;

    public CampaignAuditObserver(CampaignLifecycleEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void update(CampaignLifecycleEvent event) {
        eventRepository.save(Objects.requireNonNull(event, "Lifecycle event is required"));
    }
}
