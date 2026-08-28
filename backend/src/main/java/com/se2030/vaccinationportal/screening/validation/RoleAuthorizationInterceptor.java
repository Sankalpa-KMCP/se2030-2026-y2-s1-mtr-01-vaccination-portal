package com.se2030.vaccinationportal.screening.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Middleware interceptor verifying authenticated Spring Security authorities against
 * {@link RequireRoles} annotations on the handler method or controller class.
 */
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRoles methodAnnotation = handlerMethod.getMethodAnnotation(RequireRoles.class);
        RequireRoles classAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);

        RequireRoles targetAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;

        if (targetAnnotation == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication is required to access this resource.\"}");
            return false;
        }

        Set<String> authenticatedRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(RoleAuthorizationInterceptor::normalizeRole)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toSet());
        Set<String> allowedRoles = Arrays.stream(targetAnnotation.value())
                .map(RoleAuthorizationInterceptor::normalizeRole)
                .collect(Collectors.toSet());

        if (authenticatedRoles.stream().noneMatch(allowedRoles::contains)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\": \"Forbidden\", \"message\": \"Authenticated roles %s are not authorized to access this resource. Required: %s\"}",
                    authenticatedRoles,
                    allowedRoles
            ));
            return false;
        }

        return true;
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }
}
