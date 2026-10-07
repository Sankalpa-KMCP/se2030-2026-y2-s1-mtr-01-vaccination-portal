package com.se2030.vaccinationportal.campaign.core.repository;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for append-only campaign lifecycle audit events.
 */
@Repository
public interface CampaignLifecycleEventRepository extends JpaRepository<CampaignLifecycleEvent, Long> {

    /**
     * Retrieves campaign lifecycle history ordered deterministically by creation time and ID.
     */
    List<CampaignLifecycleEvent> findByCampaignIdOrderByCreatedAtAscIdAsc(Long campaignId);
}
