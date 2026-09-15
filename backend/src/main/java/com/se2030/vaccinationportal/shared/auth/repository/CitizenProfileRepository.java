package com.se2030.vaccinationportal.shared.auth.repository;

import com.se2030.vaccinationportal.shared.auth.model.CitizenProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, Long> {
    Optional<CitizenProfile> findByUser_Id(Long userId);
}
