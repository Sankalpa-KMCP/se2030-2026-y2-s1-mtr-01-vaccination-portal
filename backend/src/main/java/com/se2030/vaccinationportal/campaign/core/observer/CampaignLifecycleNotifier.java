package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Concrete Subject. Callbacks run in the caller's transaction; failures are propagated. */
@Component
public class CampaignLifecycleNotifier implements CampaignLifecycleSubject {
    private final CopyOnWriteArrayList<CampaignLifecycleObserver> observers = new CopyOnWriteArrayList<>();

    public CampaignLifecycleNotifier(List<CampaignLifecycleObserver> initialObservers) {
        Objects.requireNonNull(initialObservers, "Initial observers are required").forEach(this::addObserver);
    }

    @Override
    public void addObserver(CampaignLifecycleObserver observer) {
        observers.addIfAbsent(Objects.requireNonNull(observer, "Observer is required"));
    }

    @Override
    public void removeObserver(CampaignLifecycleObserver observer) {
        observers.remove(Objects.requireNonNull(observer, "Observer is required"));
    }

    @Override
    public void notifyObservers(CampaignLifecycleEvent event) {
        Objects.requireNonNull(event, "Lifecycle event is required");
        // CopyOnWriteArrayList iterators preserve a stable snapshot and registration order.
        for (CampaignLifecycleObserver observer : observers) {
            observer.update(event);
        }
    }
}
