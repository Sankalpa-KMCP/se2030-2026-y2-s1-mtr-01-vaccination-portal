package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;

/** Centralizes object-level ownership checks for citizen-accessible MF-01 endpoints. */
@Service
public class CitizenScreeningAccessService {

    private static final String CITIZEN_ROLE = "ROLE_CITIZEN";

    private final ScreeningSubmissionRepository submissionRepository;

    public CitizenScreeningAccessService(ScreeningSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public void requireCitizenIdAccess(
            Long citizenId,
            String... privilegedRoles
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!requiresCitizenOwnership(authentication, privilegedRoles)) {
            return;
        }

        UserPrincipal principal = requireUserPrincipal(authentication);
        if (!Objects.equals(principal.getUserId(), citizenId)) {
            throw new AccessDeniedException("Citizens may only access their own screening data");
        }
    }

    @Transactional(readOnly = true)
    public void requireSubmissionAccess(
            Long submissionId,
            String... privilegedRoles
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!requiresCitizenOwnership(authentication, privilegedRoles)) {
            return;
        }

        ScreeningSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Screening submission not found with ID: " + submissionId
                ));
        UserPrincipal principal = requireUserPrincipal(authentication);
        if (!Objects.equals(principal.getUserId(), submission.getCitizenId())) {
            throw new AccessDeniedException("Citizens may only access their own screening data");
        }
    }

    private boolean requiresCitizenOwnership(Authentication authentication, String... privilegedRoles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated account is required");
        }
        if (hasAnyRole(authentication, privilegedRoles) || hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            return false;
        }
        if (hasRole(authentication, CITIZEN_ROLE)) {
            return true;
        }
        throw new AccessDeniedException("Authenticated role is not authorized for citizen screening data");
    }

    private UserPrincipal requireUserPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Authenticated citizen identity is unavailable");
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        return Arrays.stream(roles).anyMatch(role -> hasRole(authentication, role));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
