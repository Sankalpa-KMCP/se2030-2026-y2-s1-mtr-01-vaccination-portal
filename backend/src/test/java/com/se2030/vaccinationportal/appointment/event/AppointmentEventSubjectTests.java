package com.se2030.vaccinationportal.appointment.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentEventSubjectTests {
    @Configuration
    @EnableTransactionManagement
    static class Config {}

    private AnnotationConfigApplicationContext context;
    private TransactionTemplate transaction;
    private final List<AppointmentEvent> notifications = new ArrayList<>();
    private final List<AppointmentEvent> audits = new ArrayList<>();
    private final AppointmentEvent event = new AppointmentEvent(AppointmentEvent.Type.BOOKED,
            1L, 2L, 3L, 4L, Instant.parse("2026-10-07T00:00:00Z"));

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext();
        context.register(Config.class);
        context.registerBean("notification", AppointmentObserver.class, () -> notifications::add);
        context.registerBean("audit", AppointmentObserver.class, () -> audits::add);
        context.registerBean(AppointmentEventSubject.class);
        context.refresh();
        transaction = new TransactionTemplate(new DataSourceTransactionManager(
                new DriverManagerDataSource("jdbc:h2:mem:observer", "sa", "")));
    }

    @AfterEach
    void close() { context.close(); }

    @Test
    void bothObserversReceiveEventOnlyAfterCommit() {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            assertTrue(notifications.isEmpty());
            assertTrue(audits.isEmpty());
        });
        assertEquals(List.of(event), notifications);
        assertEquals(List.of(event), audits);
    }

    @Test
    void rollbackSuppressesAllNotifications() {
        transaction.executeWithoutResult(status -> {
            context.publishEvent(event);
            status.setRollbackOnly();
        });
        assertTrue(notifications.isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    void eventsOutsideTransactionAreNotDelivered() {
        context.publishEvent(event);
        assertTrue(notifications.isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    void failingObserverDoesNotBlockOtherObserversOrFailCommittedOperation() {
        AppointmentObserver failing = ignored -> { throw new IllegalStateException("delivery failed"); };
        AppointmentEventSubject subject = new AppointmentEventSubject(List.of(failing, notifications::add, audits::add));
        assertDoesNotThrow(() -> subject.notifyObservers(event));
        assertEquals(List.of(event), notifications);
        assertEquals(List.of(event), audits);
    }

    @Test
    void allLifecycleEventsAreDeliveredOnceAfterCommit() {
        transaction.executeWithoutResult(status -> {
            for (AppointmentEvent.Type type : AppointmentEvent.Type.values()) {
                context.publishEvent(new AppointmentEvent(type, 1L, 2L, 3L, 4L, event.occurredAt()));
            }
        });
        assertEquals(List.of(AppointmentEvent.Type.values()),
                notifications.stream().map(AppointmentEvent::type).toList());
        assertEquals(notifications, audits);
    }
}
