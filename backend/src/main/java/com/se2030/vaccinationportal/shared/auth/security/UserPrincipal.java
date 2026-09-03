package com.se2030.vaccinationportal.shared.auth.security;

import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Authenticated Spring Security UserDetails Principal
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String email, String passwordHash, boolean active, Collection<? extends GrantedAuthority> authorities) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    public static UserPrincipal fromUserAccount(UserAccount account) {
        List<SimpleGrantedAuthority> authorities = account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.isLoginEligible(),
                authorities
        );
    }

    public static UserPrincipal fromClaims(Long userId, String email, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles != null
                ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                : Collections.emptyList();

        return new UserPrincipal(userId, email, null, true, authorities);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoleCodes() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    public boolean hasRole(String roleCode) {
        if (roleCode == null) return false;
        return authorities.stream().anyMatch(a -> roleCode.equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}
