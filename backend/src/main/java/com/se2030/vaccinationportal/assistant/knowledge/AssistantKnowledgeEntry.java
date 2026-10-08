package com.se2030.vaccinationportal.assistant.knowledge;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable value representation of a verified knowledge topic in the vaccination portal.
 *
 * @param id Stable unique identifier for the topic (e.g. APPOINTMENT_BOOKING)
 * @param module Functional module identifier (e.g. MF-01, MF-02, MF-03, MF-04, MF-05, SCREENING, AUTH)
 * @param title Human-readable topic title
 * @param keywords Keywords and user intent tokens used for deterministic relevance scoring
 * @param explanation Verified operational and workflow explanation backed by code
 * @param isPublic Whether this knowledge is accessible to unauthenticated/anonymous visitors
 * @param allowedRoles Authorities permitted to receive this knowledge (e.g. ROLE_CITIZEN, ROLE_ADMIN)
 * @param relevantRoutes Allowlisted portal routes associated with this topic
 * @param evidenceReference Internal repository reference for maintainers (never exposed to users)
 */
public record AssistantKnowledgeEntry(
        String id,
        String module,
        String title,
        Set<String> keywords,
        String explanation,
        boolean isPublic,
        Set<String> allowedRoles,
        List<String> relevantRoutes,
        String evidenceReference
) {
    public AssistantKnowledgeEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(module, "module must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(keywords, "keywords must not be null");
        Objects.requireNonNull(explanation, "explanation must not be null");
        keywords = Collections.unmodifiableSet(keywords);
        allowedRoles = allowedRoles != null ? Collections.unmodifiableSet(allowedRoles) : Collections.emptySet();
        relevantRoutes = relevantRoutes != null ? Collections.unmodifiableList(relevantRoutes) : Collections.emptyList();
    }
}
