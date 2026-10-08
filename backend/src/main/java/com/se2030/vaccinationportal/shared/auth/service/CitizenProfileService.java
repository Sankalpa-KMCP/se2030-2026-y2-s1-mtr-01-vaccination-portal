package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenProfileResponse;
import com.se2030.vaccinationportal.shared.auth.dto.UpdateCitizenProfileRequest;
import com.se2030.vaccinationportal.shared.auth.model.CitizenProfile;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CitizenProfileService {

    private static final String CITIZEN_ROLE_CODE = "ROLE_CITIZEN";

    private final UserAccountRepository userAccountRepository;
    private final CitizenProfileRepository citizenProfileRepository;

    public CitizenProfileService(
            UserAccountRepository userAccountRepository,
            CitizenProfileRepository citizenProfileRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.citizenProfileRepository = citizenProfileRepository;
    }

    public CitizenProfileResponse getOwnProfile(Long authenticatedUserId) {
        UserAccount account = loadCitizenAccount(authenticatedUserId);
        CitizenProfile profile = citizenProfileRepository.findByUser_Id(account.getId()).orElse(null);
        return toResponse(account, profile);
    }

    @Transactional
    public CitizenProfileResponse updateOwnProfile(
            Long authenticatedUserId,
            UpdateCitizenProfileRequest request
    ) {
        Objects.requireNonNull(request, "UpdateCitizenProfileRequest must not be null");
        UserAccount account = loadCitizenAccount(authenticatedUserId);
        account.setFullName(request.fullName());
        account.setPhoneNumber(request.phoneNumber());

        CitizenProfile profile = citizenProfileRepository.findByUser_Id(account.getId())
                .orElseGet(() -> new CitizenProfile(account));
        profile.updateDetails(request.dateOfBirth(), request.gender(), request.address());

        userAccountRepository.save(account);
        CitizenProfile savedProfile = citizenProfileRepository.saveAndFlush(profile);
        return toResponse(account, savedProfile);
    }

    private UserAccount loadCitizenAccount(Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new BadCredentialsException("Authenticated citizen identity is unavailable");
        }
        UserAccount account = userAccountRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated account no longer exists"));
        boolean citizen = account.getRoles().stream()
                .map(Role::getCode)
                .anyMatch(CITIZEN_ROLE_CODE::equals);
        if (!citizen) {
            throw new AccessDeniedException("Authenticated account is not a citizen");
        }
        return account;
    }

    private CitizenProfileResponse toResponse(UserAccount account, CitizenProfile profile) {
        List<String> roleCodes = account.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();
        Instant updatedAt = account.getUpdatedAt();
        if (profile != null && profile.getUpdatedAt() != null
                && (updatedAt == null || profile.getUpdatedAt().isAfter(updatedAt))) {
            updatedAt = profile.getUpdatedAt();
        }
        return new CitizenProfileResponse(
                account.getId(),
                account.getFullName(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getAccountStatus(),
                roleCodes,
                account.getCreatedAt(),
                profile != null ? profile.getDateOfBirth() : null,
                profile != null ? profile.getGender() : null,
                profile != null ? profile.getAddress() : null,
                updatedAt
        );
    }
}
