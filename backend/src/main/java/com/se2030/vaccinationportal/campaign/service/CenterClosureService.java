package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.repository.CenterClosureRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages retained center-closure history and its active-overlap invariant. */
@Service
@Transactional
public class CenterClosureService {

    private final CenterClosureRepository closureRepository;
    private final VaccinationCenterRepository centerRepository;
    private final Clock clock;

    public CenterClosureService(
            CenterClosureRepository closureRepository,
            VaccinationCenterRepository centerRepository,
            Clock clock
    ) {
        this.closureRepository = closureRepository;
        this.centerRepository = centerRepository;
        this.clock = clock;
    }

    public CenterClosure createClosure(Long centerId, Instant startAt, Instant endAt, String reason) {
        requireId(centerId, "Center ID is required");
        CenterClosure.validateCandidate(startAt, endAt, reason);

        VaccinationCenter center = centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vaccination center not found with ID: " + centerId));
        rejectActiveOverlap(centerId, null, startAt, endAt);

        return closureRepository.save(new CenterClosure(center, startAt, endAt, reason));
    }

    @Transactional(readOnly = true)
    public Optional<CenterClosure> getClosureById(Long id) {
        requireId(id, "Center closure ID is required");
        return closureRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CenterClosure> getClosuresForCenter(Long centerId) {
        requireId(centerId, "Center ID is required");
        return closureRepository.findByCenterIdOrderByStartAtAscEndAtAscIdAsc(centerId);
    }

    public CenterClosure updateFutureClosure(
            Long id,
            Instant startAt,
            Instant endAt,
            String reason
    ) {
        requireId(id, "Center closure ID is required");
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

        target.updateDetails(startAt, endAt, reason);
        return closureRepository.save(target);
    }

    public CenterClosure cancelFutureClosure(Long id) {
        requireId(id, "Center closure ID is required");

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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Center closure not found with ID: " + closureId));
    }

    private void lockCenter(Long centerId) {
        centerRepository.findByIdForUpdate(centerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vaccination center not found with ID: " + centerId));
    }

    private CenterClosure findClosure(Long closureId) {
        return closureRepository.findById(closureId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Center closure not found with ID: " + closureId));
    }

    private void rejectActiveOverlap(Long centerId, Long excludedId, Instant startAt, Instant endAt) {
        if (closureRepository.existsOverlappingClosure(
                centerId, CenterClosureStatus.ACTIVE, excludedId, startAt, endAt)) {
            throw new IllegalArgumentException("Center closure overlaps an active closure");
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
