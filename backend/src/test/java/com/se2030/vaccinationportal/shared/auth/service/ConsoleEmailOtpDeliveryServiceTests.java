package com.se2030.vaccinationportal.shared.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ConsoleEmailOtpDeliveryServiceTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("app.security.auth.delivery-mode=console")
            .withUserConfiguration(ConsoleEmailOtpDeliveryService.class);

    @Test
    void consoleOtpDeliveryIsUnavailableOutsideLocalProfile() {
        contextRunner.run(context ->
                assertEquals(0, context.getBeansOfType(ConsoleEmailOtpDeliveryService.class).size()));
    }

    @Test
    void localProfileEnablesConsoleOtpDelivery() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .run(context -> assertInstanceOf(ConsoleEmailOtpDeliveryService.class,
                        context.getBean(EmailOtpDeliveryService.class)));
    }
}
