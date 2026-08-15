package com.se2030.vaccinationportal.screening.validation;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc configuration registering the MF01 role authorization interceptor.
 */
@Configuration
public class ScreeningWebMvcConfig implements WebMvcConfigurer {

    private final RoleAuthorizationInterceptor authorizationInterceptor;

    public ScreeningWebMvcConfig(RoleAuthorizationInterceptor authorizationInterceptor) {
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/api/v1/screening/**");
    }
}
