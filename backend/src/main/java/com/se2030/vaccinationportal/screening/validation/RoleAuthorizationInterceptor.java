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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Middleware interceptor inspecting the user roles from authenticated SecurityContext
 * (with fallback to X-User-Role / X-Role headers for standalone testing)
 * and verifying against @RequireRoles annotations on the handler method or controller class.
 */
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String ROLE_HEADER = "X-User-Role";
    public static final String ALT_ROLE_HEADER = "X-Role";

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

        List<String> allowedRoles = Arrays.stream(targetAnnotation.value())
                .map(String::toUpperCase)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .toList();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Set<String> userRoles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(String::toUpperCase)
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .collect(Collectors.toSet());

            boolean isAuthorized = allowedRoles.stream().anyMatch(userRoles::contains);
            if (!isAuthorized) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                        "{\"error\": \"Forbidden\", \"message\": \"User roles %s are not authorized to access this resource. Required: %s\"}",
                        userRoles,
                        allowedRoles
                ));
                return false;
            }
            return true;
        }

        // Fallback for standalone MockMvc testing when Spring Security context is not initialized
        String userRole = request.getHeader(ROLE_HEADER);
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = request.getHeader(ALT_ROLE_HEADER);
        }

        if (userRole == null || userRole.trim().isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Missing authorization role header (X-User-Role)\"}");
            return false;
        }

        String normalizedRole = userRole.trim().toUpperCase();
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring(5);
        }

        if (!allowedRoles.contains(normalizedRole)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\": \"Forbidden\", \"message\": \"User role '%s' is not authorized to access this resource. Required: %s\"}",
                    normalizedRole,
                    allowedRoles
            ));
            return false;
        }

        return true;
    }
}
