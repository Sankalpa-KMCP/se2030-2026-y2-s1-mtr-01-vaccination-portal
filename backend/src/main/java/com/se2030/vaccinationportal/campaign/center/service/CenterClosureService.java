package com.se2030.vaccinationportal.campaign.center.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.center.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.center.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages center closures without overlapping active closures or blocking existing sessions. */
@Service
@Transactional
public class CenterClosureService {

    private static final List<SessionLifecycleStatus> BLOCKING_STATUSES = List.of(
            SessionLifecycleStatus.DRAFT,
            SessionLifecycleStatus.SCHEDULED,
            SessionLifecycleStatus.IN_PROGRESS
    );

    private final CenterClosureRepository closureRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationSessionRepository sessionRepository;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public CenterClosureService(
            CenterClosureRepository closureRepository,
            VaccinationCenterRepository centerRepository,
            VaccinationSessionRepository sessionRepository,
            Clock clock
    ) {
        this.closureRepository = closureRepository;
        this.centerRepository = centerRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    public CenterClosure createClosure(Long centerId, Instant startAt, Instant endAt, String reason) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        CenterClosure.validateCandidate(startAt, endAt, reason);

        VaccinationCenter center = lockCenter(centerId);
        rejectActiveOverlap(centerId, null, startAt, endAt);
        validateClosureSessionExclusion(centerId, startAt, endAt);

        return closureRepository.save(new CenterClosure(center, startAt, endAt, reason));
    }

    @Transactional(readOnly = true)
    public Optional<CenterClosure> getClosureById(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Center closure ID is required");
        }
        return closureRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CenterClosure> getClosuresForCenter(Long centerId) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        if (!centerRepository.existsById(centerId)) {
            throw new ResourceNotFoundException("Vaccination center not found with ID: " + centerId);
        }
        return closureRepository.findByCenterIdOrderByStartAtAscEndAtAscIdAsc(centerId);
    }

    public CenterClosure updateFutureClosure(
            Long centerId,
            Long id,
            Instant startAt,
            Instant endAt,
            String reason
    ) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        if (id == null) {
            throw new ResourceNotFoundException("Center closure ID is required");
        }

        Long actualCenterId = closureRepository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Center closure not found with ID: " + id));

        if (!centerId.equals(actualCenterId)) {
            throw new ResourceNotFoundException("Center closure not found with ID: " + id + " for center ID: " + centerId);
        }

        return updateFutureClosure(id, startAt, endAt, reason);
    }

    public CenterClosure updateFutureClosure(
            Long id,
            Instant startAt,
            Instant endAt,
            String reason
    ) {
        if (id == null) {
            throw new ResourceNotFoundException("Center closure ID is required");
        }
        CenterClosure.validateCandidate(startAt, endAt, reason);

        Long centerId = findCenterId(id);
        lockCenter(centerId);
        CenterClosure target = findClosure(id);
        verifyOwnership(target, centerId);
        requireActive(target);

        Instant now = clock.instant();
        if (!now.isBefore(target.getStartAt())) {
            throw new IllegalArgumentException("A started center closure cannot be updated");
        }
        if (!now.isBefore(startAt)) {
            throw new IllegalArgumentException("Updated center closure must start in the future");
        }
        rejectActiveOverlap(centerId, id, startAt, endAt);
        validateClosureSessionExclusion(centerId, startAt, endAt);

        target.updateDetails(startAt, endAt, reason);
        return closureRepository.save(target);
    }

    public CenterClosure cancelFutureClosure(Long centerId, Long id) {
        if (centerId == null) {
            throw new ResourceNotFoundException("Center ID is required");
        }
        if (id == null) {
            throw new ResourceNotFoundException("Center closure ID is required");
        }

        Long actualCenterId = closureRepository.findCenterIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Center closure not found with ID: " + id));

        if (!centerId.equals(actualCenterId)) {
            throw new ResourceNotFoundException("Center closure not found with ID: " + id + " for center ID: " + centerId);
        }

        return cancelFutureClosure(id);
    }

    public CenterClosure cancelFutureClosure(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Center closure ID is required");
        }

        Long centerId = findCenterId(id);
        lockCenter(centerId);
        CenterClosure target = findClosure(id);
        verifyOwnership(target, centerId);
        requireActive(target);

        Instant now = clock.instant();
        if (!now.isBefore(target.getStartAt())) {
            throw new IllegalArgumentException("A started center closure cannot be cancelled");
        }

        target.cancel();
        return closureRepository.save(target);
    }

    /**
     * Read-side overlap check for later session integration. An atomic session write must first
     * acquire the same center-row lock used by closure writes.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveClosureOverlap(Long centerId, Instant startAt, Instant endAt) {
        requireId(centerId, "Center ID is required");
        validateInterval(startAt, endAt);
        return closureRepository.existsOverlappingClosure(
                centerId, CenterClosureStatus.ACTIVE, null, startAt, endAt);
    }

    private Long findCenterId(Long closureId) {
        return closureRepository.findCenterIdById(closureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Center closure not found with ID: " + closureId));
    }

    private VaccinationCenter lockCenter(Long centerId) {
        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vaccination center not found with ID: " + centerId));
        if (entityManager != null) {
            entityManager.refresh(center);
        }
        return center;
    }

    private CenterClosure findClosure(Long closureId) {
        return closureRepository.findById(closureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Center closure not found with ID: " + closureId));
    }

    private void rejectActiveOverlap(Long centerId, Long excludedId, Instant startAt, Instant endAt) {
        if (closureRepository.existsOverlappingClosure(
                centerId, CenterClosureStatus.ACTIVE, excludedId, startAt, endAt)) {
            throw new IllegalArgumentException("Center closure overlaps an active closure");
        }
    }

    private void validateClosureSessionExclusion(Long centerId, Instant startAt, Instant endAt) {
        List<VaccinationSession> blockingSessions = sessionRepository.findBlockingSessionsForCenter(
                centerId,
                BLOCKING_STATUSES,
                clock.instant()
        );
        for (VaccinationSession session : blockingSessions) {
            if (session.getStartAt().isBefore(endAt) && session.getEndAt().isAfter(startAt)) {
                throw new IllegalArgumentException(
                        "Active center closure overlaps an active or scheduled session (" +
                                session.getOccurrenceKey() + ")"
                );
            }
        }
    }

    private static void requireActive(CenterClosure closure) {
        if (closure.getStatus() != CenterClosureStatus.ACTIVE) {
            throw new IllegalArgumentException("Only an active center closure can be changed");
        }
    }

    private static void verifyOwnership(CenterClosure closure, Long centerId) {
        if (!centerId.equals(closure.getCenter().getId())) {
            throw new IllegalArgumentException("Center closure ownership changed during update");
        }
    }

    private static void validateInterval(Instant startAt, Instant endAt) {
        if (startAt == null) {
            throw new IllegalArgumentException("Closure start is required");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("Closure end is required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Closure start must be before closure end");
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
