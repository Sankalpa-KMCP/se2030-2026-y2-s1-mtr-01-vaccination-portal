package com.se2030.vaccinationportal.campaign.api.session;

import java.util.List;

/** Cross-module read port for durable MF-02 session change events. */
public interface SessionChangeReader {
    List<SessionChangeNotice> findAllOrdered();
}
