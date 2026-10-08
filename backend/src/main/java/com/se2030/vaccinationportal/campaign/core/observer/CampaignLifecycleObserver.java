package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;

/** Observer contract for synchronous, transactional campaign lifecycle notifications. */
public interface CampaignLifecycleObserver {
    void update(CampaignLifecycleEvent event);
}
