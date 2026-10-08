package com.se2030.vaccinationportal.shared.auth.repository;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
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

    /**
     * Atomically records one failed login for the account. When the incremented
     * count reaches the configured threshold the counter is reset and the timed
     * lock is established in the same statement, so the transition cannot lose
     * increments or be skipped under concurrent failed logins. Accounts with an
     * active timed lock match no rows: failures during an active lock neither
     * count toward the next lock nor extend the current one.
     *
     * @return the number of accounts updated (1 when recorded, 0 while an active timed lock exists)
     */
    @Modifying
    @Query("""
            update UserAccount account
            set account.failedLoginAttempts = case when account.failedLoginAttempts + 1 >= :maxAttempts then 0 else account.failedLoginAttempts + 1 end,
                account.lockedUntil = case when account.failedLoginAttempts + 1 >= :maxAttempts then :lockedUntil else account.lockedUntil end
            where account.id = :id
              and (account.lockedUntil is null or account.lockedUntil <= :now)
            """)
    int recordFailedLoginAttempt(@Param("id") Long id,
                                 @Param("maxAttempts") int maxAttempts,
                                 @Param("lockedUntil") Instant lockedUntil,
                                 @Param("now") Instant now);

    /**
     * Resets the failed-login counter after a successful login.
     *
     * @return the number of accounts updated (0 when the counter was already zero)
     */
    @Modifying
    @Query("""
            update UserAccount account
            set account.failedLoginAttempts = 0
            where account.id = :id
              and account.failedLoginAttempts <> 0
            """)
    int resetFailedLoginAttempts(@Param("id") Long id);

    /**
     * Clears an elapsed timed lock together with the failure counter, implementing the
     * canonical "unlock by System after expiry" transition. Permanent LOCKED account
     * status is intentionally untouched.
     *
     * @return the number of accounts updated (1 when an elapsed timed lock was cleared)
     */
    @Modifying
    @Query("""
            update UserAccount account
            set account.lockedUntil = null, account.failedLoginAttempts = 0
            where account.id = :id
              and account.lockedUntil is not null
              and account.lockedUntil <= :now
            """)
    int clearExpiredTimedLock(@Param("id") Long id, @Param("now") Instant now);
}
