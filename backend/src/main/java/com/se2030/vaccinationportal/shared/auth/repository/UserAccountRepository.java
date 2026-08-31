package com.se2030.vaccinationportal.shared.auth.repository;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "requestedRole", "reviewedBy"})
    List<UserAccount> findAllByAccountStatusAndRequestedRoleIsNotNullOrderByCreatedAtAsc(AccountStatus accountStatus);

    @EntityGraph(attributePaths = {"roles", "requestedRole", "reviewedBy"})
    @Query("select account from UserAccount account where account.id = :id and account.requestedRole is not null")
    Optional<UserAccount> findStaffRequestById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from UserAccount account where account.id = :id and account.requestedRole is not null")
    Optional<UserAccount> findStaffRequestByIdForUpdate(@Param("id") Long id);

    @Query("select (count(account) > 0) from UserAccount account join account.roles role where role.code = :roleCode")
    boolean existsByAuthoritativeRoleCode(@Param("roleCode") String roleCode);
}
