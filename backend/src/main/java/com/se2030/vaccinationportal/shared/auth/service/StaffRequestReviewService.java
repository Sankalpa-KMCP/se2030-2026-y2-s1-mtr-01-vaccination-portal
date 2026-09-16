package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.dto.StaffApprovalRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRejectionRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRequestResponse;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidApprovalRoleException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRejectionReasonException;
import com.se2030.vaccinationportal.shared.auth.exception.StaffRequestNotFoundException;
import com.se2030.vaccinationportal.shared.auth.exception.StaffRequestNotPendingException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StaffRequestReviewService {

    private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";
    private static final Set<String> APPROVABLE_ROLE_CODES = Set.of(
            "ROLE_HEALTHCARE_OFFICER",
            "ROLE_CENTER_MANAGER",
            "ROLE_CAMPAIGN_OFFICER",
            "ROLE_INVENTORY_OFFICER",
            "ROLE_VACCINATOR"
    );

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    public StaffRequestReviewService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }

    public List<StaffRequestResponse> listPendingRequests() {
        return userAccountRepository
                .findAllByAccountStatusAndRequestedRoleIsNotNullOrderByCreatedAtAsc(AccountStatus.PENDING_APPROVAL)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StaffRequestResponse getStaffRequest(Long requestId) {
        return toResponse(userAccountRepository.findStaffRequestById(requestId)
                .orElseThrow(() -> new StaffRequestNotFoundException(requestId)));
    }

    @Transactional
    public StaffRequestResponse approve(Long requestId, StaffApprovalRequest request, Long reviewerUserId) {
        Objects.requireNonNull(request, "StaffApprovalRequest must not be null");
        UserAccount target = loadPendingRequestForUpdate(requestId);
        String approvedRoleCode = normalizeRoleCode(request.roleCode());
        if (!APPROVABLE_ROLE_CODES.contains(approvedRoleCode)) {
            throw new InvalidApprovalRoleException();
        }

        Role approvedRole = roleRepository.findByCode(approvedRoleCode)
                .orElseThrow(InvalidApprovalRoleException::new);
        UserAccount reviewer = loadAdministrator(reviewerUserId);

        target.setAccountStatus(AccountStatus.ACTIVE);
        target.getRoles().clear();
        target.addRole(approvedRole);
        target.setReviewedBy(reviewer);
        target.setReviewedAt(Instant.now());
        target.setRejectionReason(null);

        return toResponse(userAccountRepository.saveAndFlush(target));
    }

    @Transactional
    public StaffRequestResponse reject(Long requestId, StaffRejectionRequest request, Long reviewerUserId) {
        Objects.requireNonNull(request, "StaffRejectionRequest must not be null");
        String rejectionReason = request.reason();
        if (rejectionReason == null || rejectionReason.isBlank() || rejectionReason.length() > 500) {
            throw new InvalidRejectionReasonException();
        }
        UserAccount target = loadPendingRequestForUpdate(requestId);
        UserAccount reviewer = loadAdministrator(reviewerUserId);

        target.setAccountStatus(AccountStatus.REJECTED);
        target.getRoles().clear();
        target.setReviewedBy(reviewer);
        target.setReviewedAt(Instant.now());
        target.setRejectionReason(rejectionReason);

        return toResponse(userAccountRepository.saveAndFlush(target));
    }

    public static Set<String> getApprovableRoleCodes() {
        return APPROVABLE_ROLE_CODES;
    }

    private UserAccount loadPendingRequestForUpdate(Long requestId) {
        UserAccount target = userAccountRepository.findStaffRequestByIdForUpdate(requestId)
                .orElseThrow(() -> new StaffRequestNotFoundException(requestId));
        if (target.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new StaffRequestNotPendingException();
        }
        return target;
    }

    private UserAccount loadAdministrator(Long reviewerUserId) {
        UserAccount reviewer = userAccountRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated administrator account no longer exists"));
        boolean administrator = reviewer.getAccountStatus() == AccountStatus.ACTIVE
                && reviewer.getRoles().stream().map(Role::getCode).anyMatch(ADMIN_ROLE_CODE::equals);
        if (!administrator) {
            throw new AccessDeniedException("Authenticated account is not an active administrator");
        }
        return reviewer;
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private StaffRequestResponse toResponse(UserAccount account) {
        UserAccount reviewer = account.getReviewedBy();
        return new StaffRequestResponse(
                account.getId(),
                account.getFullName(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getRequestedRole() != null ? account.getRequestedRole().getCode() : null,
                account.getAccountStatus(),
                account.getRoles().stream().map(Role::getCode).sorted().toList(),
                account.getCreatedAt(),
                account.getReviewedAt(),
                reviewer != null ? reviewer.getId() : null,
                reviewer != null ? reviewer.getEmail() : null,
                account.getRejectionReason()
        );
    }
}
