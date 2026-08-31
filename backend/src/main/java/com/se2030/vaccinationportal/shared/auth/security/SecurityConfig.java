package com.se2030.vaccinationportal.shared.auth.security;

import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Staged Configuration for AUTH-1
 * Preserves accessibility for existing operational endpoints during transitional foundation rollout.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final UserAccountRepository userAccountRepository;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            UserAccountRepository userAccountRepository
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.userAccountRepository = userAccountRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userAccountRepository.findByEmailIgnoreCase(UserAccount.normalizeEmail(email))
                .map(UserPrincipal::fromUserAccount)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Public Authentication Endpoint
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/auth/register/citizen",
                                "/api/v1/auth/register/staff"
                        ).permitAll()

                        // Protected Current User Profile Endpoint
                        .requestMatchers("/api/v1/auth/me").authenticated()

                        // Administrator-only staff account review endpoints
                        .requestMatchers("/api/v1/admin/staff-requests/**").hasRole("ADMIN")

                        // Protected Screening Endpoints (prevents unauthenticated header spoofing at HTTP boundary)
                        .requestMatchers("/api/v1/screening/**").authenticated()

                        // Protected Session Change Control Confirmation Endpoints (F4C)
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/sessions/*/cancel",
                                "/api/v1/sessions/*/reschedule",
                                "/api/v1/sessions/*/transfer"
                        ).authenticated()

                        // Protected Session Recurrence Confirmation Endpoints (F4D)
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/recurrence-rules/*/change",
                                "/api/v1/recurrence-rules/*/cancel"
                        ).authenticated()

                        // Public Certificate Verification & Documentation
                        .requestMatchers("/api/v1/certificates/verify/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Functional REST endpoints delegate method-level security to @PreAuthorize
                        .requestMatchers("/api/v1/**").permitAll()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
