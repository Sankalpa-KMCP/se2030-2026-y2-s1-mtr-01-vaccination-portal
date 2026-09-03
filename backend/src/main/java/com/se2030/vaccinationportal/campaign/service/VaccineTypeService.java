package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing master data lifecycle management for Vaccine Types.
 */
@Service
@Transactional
public class VaccineTypeService {

    private final VaccineTypeRepository vaccineTypeRepository;

    public VaccineTypeService(VaccineTypeRepository vaccineTypeRepository) {
        this.vaccineTypeRepository = vaccineTypeRepository;
    }

    /**
     * Retrieve vaccine types, optionally including inactive records.
     *
     * @param includeInactive whether to return both active and inactive records
     * @return List of VaccineType entities
     */
    @Transactional(readOnly = true)
    public List<VaccineType> getVaccineTypes(boolean includeInactive) {
        if (includeInactive) {
            return vaccineTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        return vaccineTypeRepository.findByActiveTrue();
    }

    /**
     * Retrieve all currently active vaccine types (backward compatible helper).
     *
     * @return List of active VaccineType entities
     */
    @Transactional(readOnly = true)
    public List<VaccineType> getActiveVaccineTypes() {
        return getVaccineTypes(false);
    }

    /**
     * Create a new VaccineType record.
     */
    public VaccineType createVaccineType(String code, String name, String doseUnit) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Vaccine type code is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Vaccine type name is required");
        }
        String normalizedCode = code.trim();
        if (vaccineTypeRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Vaccine type already exists with code: " + normalizedCode);
        }

        String unit = (doseUnit != null && !doseUnit.trim().isEmpty()) ? doseUnit.trim() : "dose";
        VaccineType vaccineType = new VaccineType(normalizedCode, name.trim(), unit, true);
        return vaccineTypeRepository.save(vaccineType);
    }

    /**
     * Update mutable fields of an existing VaccineType.
     */
    public VaccineType updateVaccineType(Long id, String name, String doseUnit) {
        if (id == null) {
            throw new IllegalArgumentException("Vaccine type ID is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Vaccine type name is required");
        }

        VaccineType vaccineType = vaccineTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VaccineType not found with ID: " + id));

        vaccineType.setName(name.trim());
        if (doseUnit != null && !doseUnit.trim().isEmpty()) {
            vaccineType.setDoseUnit(doseUnit.trim());
        }
        return vaccineTypeRepository.save(vaccineType);
    }

    /**
     * Activate a VaccineType record.
     */
    public VaccineType activateVaccineType(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Vaccine type ID is required");
        }
        VaccineType vaccineType = vaccineTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VaccineType not found with ID: " + id));
        vaccineType.setActive(true);
        return vaccineTypeRepository.save(vaccineType);
    }

    /**
     * Deactivate a VaccineType record.
     */
    public VaccineType deactivateVaccineType(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Vaccine type ID is required");
        }
        VaccineType vaccineType = vaccineTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VaccineType not found with ID: " + id));
        vaccineType.setActive(false);
        return vaccineTypeRepository.save(vaccineType);
    }
}
