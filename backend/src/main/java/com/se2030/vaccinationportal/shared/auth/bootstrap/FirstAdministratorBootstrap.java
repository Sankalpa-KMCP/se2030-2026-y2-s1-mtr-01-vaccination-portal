package com.se2030.vaccinationportal.shared.auth.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class FirstAdministratorBootstrap implements ApplicationRunner {

    private final FirstAdministratorBootstrapService bootstrapService;
    private final String email;
    private final String password;
    private final String fullName;
    private final String phoneNumber;

    public FirstAdministratorBootstrap(
            FirstAdministratorBootstrapService bootstrapService,
            @Value("${app.bootstrap.admin.email:}") String email,
            @Value("${app.bootstrap.admin.password:}") String password,
            @Value("${app.bootstrap.admin.full-name:}") String fullName,
            @Value("${app.bootstrap.admin.phone:}") String phoneNumber
    ) {
        this.bootstrapService = bootstrapService;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<String> values = List.of(email, password, fullName, phoneNumber);
        boolean anyConfigured = values.stream().anyMatch(StringUtils::hasText);
        if (!anyConfigured) {
            return;
        }
        if (!values.stream().allMatch(StringUtils::hasText)) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD, BOOTSTRAP_ADMIN_FULL_NAME, "
                            + "and BOOTSTRAP_ADMIN_PHONE must all be provided together");
        }
        bootstrapService.bootstrap(email, password, fullName, phoneNumber);
    }
}
