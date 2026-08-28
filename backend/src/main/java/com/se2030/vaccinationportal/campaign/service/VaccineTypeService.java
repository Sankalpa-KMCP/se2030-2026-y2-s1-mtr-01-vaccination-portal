package com.se2030.vaccinationportal.campaign.service;

import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing read-only master data access for Vaccine Types.
 */
@Service
@Transactional(readOnly = true)
public class VaccineTypeService {

    private final VaccineTypeRepository vaccineTypeRepository;

    public VaccineTypeService(VaccineTypeRepository vaccineTypeRepository) {
        this.vaccineTypeRepository = vaccineTypeRepository;
    }

    /**
     * Retrieve all currently active vaccine types.
     *
     * @return List of active VaccineType entities
     */
    public List<VaccineType> getActiveVaccineTypes() {
        return vaccineTypeRepository.findByActiveTrue();
    }
}
