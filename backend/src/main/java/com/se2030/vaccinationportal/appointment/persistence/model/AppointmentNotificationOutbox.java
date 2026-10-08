package com.se2030.vaccinationportal.appointment.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "appointment_notification_outbox")
public class AppointmentNotificationOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "appointment_id", nullable = false, updatable = false) private Long appointmentId;
    @Column(name = "citizen_id", nullable = false, updatable = false) private Long citizenId;
    @Column(name = "notification_type", nullable = false, length = 40, updatable = false) private String notificationType;
    @Column(name = "message", nullable = false, length = 500, updatable = false) private String message;
    @Column(name = "status", nullable = false, length = 24) private String status;
    @Column(name = "attempts", nullable = false) private int attempts;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "delivered_at") private Instant deliveredAt;

    protected AppointmentNotificationOutbox() {}
    public AppointmentNotificationOutbox(Long appointmentId, Long citizenId, String notificationType, String message) {
        this.appointmentId = java.util.Objects.requireNonNull(appointmentId);
        this.citizenId = java.util.Objects.requireNonNull(citizenId);
        this.notificationType = java.util.Objects.requireNonNull(notificationType);
        this.message = java.util.Objects.requireNonNull(message);
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getAppointmentId() { return appointmentId; }
    public Long getCitizenId() { return citizenId; }
    public String getNotificationType() { return notificationType; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
