package com.se2030.vaccinationportal.screening.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Middleware interceptor inspecting the user role headers (e.g. X-User-Role / X-Role)
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
        List<String> allowedRoles = Arrays.stream(targetAnnotation.value())
                .map(String::toUpperCase)
                .toList();

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
