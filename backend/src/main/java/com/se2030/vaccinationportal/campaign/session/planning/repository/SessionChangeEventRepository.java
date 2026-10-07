package com.se2030.vaccinationportal.campaign.session.planning.repository;

import com.se2030.vaccinationportal.campaign.session.planning.model.SessionChangeEvent;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for append-only session change audit events.
 */
@Repository
public interface SessionChangeEventRepository extends JpaRepository<SessionChangeEvent, Long> {

    /**
     * Retrieves all change audit events for a session, ordered deterministically by creation time and ID.
     *
     * @param sessionId ID of the target VaccinationSession
     * @return Deterministically ordered list of change events
     */
    List<SessionChangeEvent> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    /**
     * Retrieves all change audit events for a collection of sessions, ordered deterministically by creation time and ID.
     *
     * @param sessionIds Collection of session IDs to query
     * @return Deterministically ordered list of change events
     */
    @Query("SELECT e FROM SessionChangeEvent e WHERE e.session.id IN :sessionIds ORDER BY e.createdAt ASC, e.id ASC")
    List<SessionChangeEvent> findBySessionIdIn(@Param("sessionIds") Collection<Long> sessionIds);
}
