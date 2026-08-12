package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing the physical vaccination center master registry.
 * Defined by canonical specification WBVP-SE2030 section 6.1 / 12 (Table 12.1).
 *
 * <p>Implementation Policies:</p>
 * <ul>
 *   <li>Code uniqueness is strictly validated prior to persistence.</li>
 *   <li>Center code is preserved as an immutable business identifier during update operations.</li>
 *   <li>Centers follow an activation/deactivation lifecycle; hard deletion is prohibited.</li>
 *   <li>Temporary facility closures are managed by {@code CenterClosure}, not by mutating {@code CenterStatus}.</li>
 * </ul>
 */
@Service
@Transactional
public class VaccinationCenterService {

    private static final List<SessionLifecycleStatus> BLOCKING_STATUSES = List.of(
            SessionLifecycleStatus.DRAFT,
            SessionLifecycleStatus.SCHEDULED,
            SessionLifecycleStatus.IN_PROGRESS
    );

    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public VaccinationCenterService(VaccinationCenterRepository centerRepository,
                                    VaccinationSessionRepository sessionRepository,
                                    Clock clock) {
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    /**
     * Creates and persists a new physical vaccination center in {@link CenterStatus#ACTIVE} status.
     *
     * @param code Unique business identifier (e.g. "CTR-NCMC")
     * @param name Center name
     * @param address Physical address
     * @param contact Contact information
     * @param safeConcurrentOccupancy Maximum safe simultaneous building occupancy limit
     * @param sessionServiceCeiling Optional upper bound on single-session approved capacity
     * @param timezone Explicit Java ZoneId used as the center's local scheduling timezone
     * @return Persisted VaccinationCenter entity
     */
    public VaccinationCenter createCenter(String code,
                                          String name,
                                          String address,
                                          String contact,
                                          int safeConcurrentOccupancy,
                                          Integer sessionServiceCeiling,
                                          String timezone) {
        String normalizedCode = VaccinationCenter.normalizeCode(code);
        String normalizedTimezone = VaccinationCenter.normalizeTimezone(timezone);

        if (centerRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Vaccination center already exists with code: " + normalizedCode);
        }

        VaccinationCenter center = new VaccinationCenter(
                normalizedCode,
                name,
                address,
                contact,
                CenterStatus.ACTIVE,
                safeConcurrentOccupancy,
                sessionServiceCeiling,
                normalizedTimezone
        );

        return centerRepository.save(center);
    }

    /**
     * Updates mutable master attributes of an existing vaccination center.
     * The business code is preserved to maintain audit and operational consistency.
     *
     * @param id Center entity ID
     * @param name Updated center name
     * @param address Updated physical address
     * @param contact Updated contact information
     * @param safeConcurrentOccupancy Updated safe concurrent occupancy limit
     * @param sessionServiceCeiling Updated optional session service ceiling
     * @return Updated VaccinationCenter entity
     */
    public VaccinationCenter updateCenter(Long id,
                                          String name,
                                          String address,
                                          String contact,
                                          int safeConcurrentOccupancy,
                                          Integer sessionServiceCeiling) {
        VaccinationCenter center = findAuthoritativeCenter(id);

        center.updateDetails(name, address, contact, safeConcurrentOccupancy, sessionServiceCeiling);

        return centerRepository.save(center);
    }

    /**
     * Activates a vaccination center, making it available for operational planning.
     *
     * @param id Center entity ID
     * @return Updated VaccinationCenter entity with ACTIVE status
     */
    public VaccinationCenter activateCenter(Long id) {
        VaccinationCenter center = findAuthoritativeCenter(id);

        center.activate();
        return centerRepository.save(center);
    }

    /**
     * Deactivates a vaccination center, retiring it from future operational planning.
     * Note: Temporary closures must be modeled via CenterClosure, not deactivation.
     *
     * @param id Center entity ID
     * @return Updated VaccinationCenter entity with INACTIVE status
     */
    public VaccinationCenter deactivateCenter(Long id) {
        VaccinationCenter center = findAuthoritativeCenter(id);

        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCenter(
                id,
                BLOCKING_STATUSES,
                clock.instant()
        );
        if (!blockingSessions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot deactivate vaccination center because " + blockingSessions.size() +
                            " future or in-progress session(s) depend on it"
            );
        }

        center.deactivate();
        return centerRepository.save(center);
    }

    /**
     * Retrieves a vaccination center by its primary key.
     *
     * @param id Center entity ID
     * @return Optional containing the center if found
     */
    @Transactional(readOnly = true)
    public Optional<VaccinationCenter> getCenterById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return centerRepository.findById(id);
    }

    /**
     * Retrieves a vaccination center by its unique business code.
     *
     * @param code Center business code
     * @return Optional containing the center if found
     */
    @Transactional(readOnly = true)
    public Optional<VaccinationCenter> getCenterByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return centerRepository.findByCode(VaccinationCenter.normalizeCode(code));
    }

    /**
     * Lists all registered vaccination centers ordered by code ascending.
     *
     * @return List of all centers
     */
    @Transactional(readOnly = true)
    public List<VaccinationCenter> getAllCenters() {
        return centerRepository.findAllByOrderByCodeAsc();
    }

    /**
     * Lists all active vaccination centers.
     *
     * @return List of centers in ACTIVE status
     */
    @Transactional(readOnly = true)
    public List<VaccinationCenter> getActiveCenters() {
        return centerRepository.findByCenterStatus(CenterStatus.ACTIVE);
    }

    private VaccinationCenter findAuthoritativeCenter(Long id) {
        VaccinationCenter center = centerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vaccination center not found with ID: " + id));
        entityManager.refresh(center);
        return center;
    }
}
