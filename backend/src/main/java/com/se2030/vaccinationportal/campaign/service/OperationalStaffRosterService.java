package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.OperationalStaffRosterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination center not found with ID: " + centerId));

        List<OperationalStaffRoster> existingUserRosters = rosterRepository.findByUserIdForUpdateOrderByIdAsc(userId);
        for (OperationalStaffRoster existing : existingUserRosters) {
            if (existing.getCenter().getId().equals(centerId)) {
                throw new IllegalStateException("Staff user " + userId + " is already rostered at center " + centerId);
            }
        }

        OperationalStaffStatus finalStatus = status != null ? status : OperationalStaffStatus.ACTIVE;
        try {
            OperationalStaffRoster roster = new OperationalStaffRoster(center, userId, role, finalStatus);
            return rosterRepository.saveAndFlush(roster);
        } catch (DataIntegrityViolationException ex) {
            if (rosterRepository.existsByCenterIdAndUserId(centerId, userId)) {
                throw new IllegalStateException("Staff user " + userId + " is already rostered at center " + centerId, ex);
            }
            throw ex;
        }
    }

    public OperationalStaffRoster registerStaffMember(Long centerId, Long userId, StaffRole role) {
        return registerStaffMember(centerId, userId, role, OperationalStaffStatus.ACTIVE);
    }

    /**
     * Updates mutable properties (role and status) of a rostered staff member.
     * Center and userId are immutable.
     */
    public OperationalStaffRoster updateStaffRoster(Long centerId,
                                                   Long rosterId,
                                                   StaffRole role,
                                                   OperationalStaffStatus status) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (rosterId == null) {
            throw new IllegalArgumentException("Roster ID is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("Staff role is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Staff status is required");
        }

        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }

        OperationalStaffRoster roster = rosterRepository.findByIdForUpdate(rosterId)
                .orElseThrow(() -> new ResourceNotFoundException("Operational staff roster entry not found with ID: " + rosterId));

        if (!roster.getCenter().getId().equals(centerId)) {
            throw new ResourceNotFoundException("Operational staff roster entry with ID " + rosterId + " does not belong to center " + centerId);
        }

        rosterRepository.findByUserIdForUpdateOrderByIdAsc(roster.getUserId());

        roster.setRole(role);
        roster.setStatus(status);
        return rosterRepository.saveAndFlush(roster);
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
                .orElseThrow(() -> new ResourceNotFoundException("Operational staff roster entry not found with ID: " + rosterId));
        roster.setStatus(newStatus);
        return rosterRepository.save(roster);
    }

    /**
     * Retrieves a staff roster entry by ID with center ownership validation.
     */
    @Transactional(readOnly = true)
    public OperationalStaffRoster getRosterEntry(Long centerId, Long rosterId) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (rosterId == null) {
            throw new IllegalArgumentException("Roster ID is required");
        }

        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }

        OperationalStaffRoster roster = rosterRepository.findById(rosterId)
                .orElseThrow(() -> new ResourceNotFoundException("Operational staff roster entry not found with ID: " + rosterId));

        if (!roster.getCenter().getId().equals(centerId)) {
            throw new ResourceNotFoundException("Operational staff roster entry with ID " + rosterId + " does not belong to center " + centerId);
        }

        return roster;
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
                .orElseThrow(() -> new ResourceNotFoundException("Operational staff roster entry not found with ID: " + rosterId));
    }

    /**
     * Retrieves all staff roster entries for a center.
     */
    @Transactional(readOnly = true)
    public List<OperationalStaffRoster> getStaffRosterForCenter(Long centerId) {
        return getStaffRosterForCenter(centerId, null, null);
    }

    /**
     * Retrieves staff roster entries for a center with optional role and status filters.
     */
    @Transactional(readOnly = true)
    public List<OperationalStaffRoster> getStaffRosterForCenter(Long centerId, StaffRole role, OperationalStaffStatus status) {
        if (centerId == null) {
            throw new IllegalArgumentException("Center ID is required");
        }
        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }

        if (role != null && status != null) {
            return rosterRepository.findByCenterIdAndRoleAndStatusOrderByUserIdAsc(centerId, role, status);
        } else if (role != null) {
            return rosterRepository.findByCenterIdAndRoleOrderByUserIdAsc(centerId, role);
        } else if (status != null) {
            return rosterRepository.findByCenterIdAndStatusOrderByUserIdAsc(centerId, status);
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
