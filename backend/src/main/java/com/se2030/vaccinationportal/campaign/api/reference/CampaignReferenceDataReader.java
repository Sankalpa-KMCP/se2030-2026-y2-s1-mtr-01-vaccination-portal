package com.se2030.vaccinationportal.campaign.api.reference;

import java.util.List;

/** Read-only campaign reference facts used by inventory. */
public interface CampaignReferenceDataReader {
    boolean centerExists(Long centerId);

    boolean vaccineTypeExists(Long vaccineTypeId);

    List<ReferenceSnapshot> centers();

    List<ReferenceSnapshot> vaccineTypes();
}
