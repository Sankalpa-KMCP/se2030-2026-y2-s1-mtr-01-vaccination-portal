package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;

/** Subject contract: register observers and notify them when a lifecycle change occurs. */
public interface CampaignLifecycleSubject {
    void addObserver(CampaignLifecycleObserver observer);
    void removeObserver(CampaignLifecycleObserver observer);
    void notifyObservers(CampaignLifecycleEvent event);
}
