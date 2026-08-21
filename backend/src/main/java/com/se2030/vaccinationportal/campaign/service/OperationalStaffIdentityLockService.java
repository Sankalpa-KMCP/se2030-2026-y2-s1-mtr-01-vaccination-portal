package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.OperationalStaffIdentity;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffIdentityRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated concurrency coordinator for operational staff identity anchor locks.
 * Guarantees atomic first-use anchor initialization and deterministic ascending userId lock ordering.
 */
@Service
@Transactional
public class OperationalStaffIdentityLockService {

    private final OperationalStaffIdentityRepository identityRepository;

    public OperationalStaffIdentityLockService(OperationalStaffIdentityRepository identityRepository) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository must not be null");
    }

    /**
     * Ensures an OperationalStaffIdentity anchor exists for the user using an atomic database upsert.
     */
    public void ensureAnchor(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        identityRepository.bootstrapIdentity(userId);
    }

    /**
     * Ensures an anchor exists and acquires a PESSIMISTIC_WRITE lock on the OperationalStaffIdentity row.
     */
    public OperationalStaffIdentity lockUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        identityRepository.bootstrapIdentity(userId);
        return identityRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Operational staff identity not found after bootstrap for user ID: " + userId));
    }

    /**
     * Ensures anchors exist and acquires PESSIMISTIC_WRITE locks on all anchors
     * in deterministic ascending userId order to prevent deadlocks across concurrent multi-user operations.
     */
    public List<OperationalStaffIdentity> lockUsersOrdered(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctSortedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (distinctSortedUserIds.isEmpty()) {
            return List.of();
        }

        for (Long userId : distinctSortedUserIds) {
            identityRepository.bootstrapIdentity(userId);
        }

        List<OperationalStaffIdentity> lockedIdentities = new ArrayList<>(distinctSortedUserIds.size());
        for (Long userId : distinctSortedUserIds) {
            OperationalStaffIdentity identity = identityRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new IllegalStateException("Operational staff identity not found for user ID: " + userId));
            lockedIdentities.add(identity);
        }
        return lockedIdentities;
    }
}
