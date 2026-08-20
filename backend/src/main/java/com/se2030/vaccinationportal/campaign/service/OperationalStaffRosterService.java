package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain service managing operational staff rostering at vaccination centers.
 * Provides the stable operational locking anchor for staff scheduling.
 */
@Service
@Transactional
public class OperationalStaffRosterService {

    private final OperationalStaffRosterRepository rosterRepository;
    private final VaccinationCenterRepository centerRepository;

    public OperationalStaffRosterService(OperationalStaffRosterRepository rosterRepository,
                                         VaccinationCenterRepository centerRepository) {
        this.rosterRepository = Objects.requireNonNull(rosterRepository, "rosterRepository must not be null");
        this.centerRepository = Objects.requireNonNull(centerRepository, "centerRepository must not be null");
    }

    /**
     * Registers a staff member in the operational roster for a center.
     */
    public OperationalStaffRoster registerStaffMember(Long centerId,
                                                      Long userId,
                                                      StaffRole role,
                                                      OperationalStaffStatus status) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Staff role is required");
        }

        VaccinationCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Vaccination center not found: " + centerId));

        if (rosterRepository.existsByCenterIdAndUserId(centerId, userId)) {
            throw new IllegalStateException("Staff user " + userId + " is already rostered at center " + centerId);
        }

        OperationalStaffRoster roster = new OperationalStaffRoster(center, userId, role, status);
        return rosterRepository.save(roster);
    }

    public OperationalStaffRoster registerStaffMember(Long centerId, Long userId, StaffRole role) {
        return registerStaffMember(centerId, userId, role, OperationalStaffStatus.ACTIVE);
    }

    /**
     * Updates the operational status of a rostered staff member.
     */
    public OperationalStaffRoster updateStaffStatus(Long rosterId, OperationalStaffStatus newStatus) {
        if (rosterId == null) {
            throw new IllegalArgumentException("Roster ID is required");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Staff status is required");
        }
        OperationalStaffRoster roster = rosterRepository.findById(rosterId)
                .orElseThrow(() -> new IllegalArgumentException("Operational staff roster entry not found: " + rosterId));
        roster.setStatus(newStatus);
        return rosterRepository.save(roster);
    }

    /**
     * Retrieves a staff roster entry by ID.
     */
    @Transactional(readOnly = true)
    public OperationalStaffRoster getRosterEntry(Long rosterId) {
        if (rosterId == null) {
            throw new IllegalArgumentException("Roster ID is required");
        }
        return rosterRepository.findById(rosterId)
                .orElseThrow(() -> new IllegalArgumentException("Operational staff roster entry not found: " + rosterId));
    }

    /**
     * Retrieves all staff roster entries for a center.
     */
    @Transactional(readOnly = true)
    public List<OperationalStaffRoster> getStaffRosterForCenter(Long centerId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        return rosterRepository.findByCenterIdOrderByUserIdAsc(centerId);
    }

    /**
     * Retrieves all staff roster entries for a user across centers.
     */
    @Transactional(readOnly = true)
    public List<OperationalStaffRoster> getRosterEntriesForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        return rosterRepository.findByUserIdOrderByCenterIdAsc(userId);
    }
}
