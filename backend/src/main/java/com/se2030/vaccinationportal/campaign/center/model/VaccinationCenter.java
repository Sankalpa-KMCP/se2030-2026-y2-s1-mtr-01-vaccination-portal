package com.se2030.vaccinationportal.campaign.center.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Domain entity representing a physical vaccination center.
 * Defined by canonical specification WBVP-SE2030 section 6.1 / 12 (Table 12.1).
 *
 * <p>Canonical Core Fields:</p>
 * <ul>
 *   <li>{@code id}: System primary key.</li>
 *   <li>{@code code}: Unique business identifier for the center (e.g. "CTR-NCMC").</li>
 *   <li>{@code name}: Official center display name.</li>
 *   <li>{@code address}: Physical facility address.</li>
 *   <li>{@code contact}: Official contact information.</li>
 *   <li>{@code timezone}: Authoritative local scheduling timezone stored as a normalized Java ZoneId.</li>
 *   <li>{@code centerStatus}: Long-term operational status ({@link CenterStatus#ACTIVE} or {@link CenterStatus#INACTIVE}).</li>
 *   <li>{@code safeConcurrentOccupancy}: Simultaneous physical occupancy safety limit.</li>
 *   <li>{@code sessionServiceCeiling}: Optional upper bound on approved capacity for individual sessions (nullable).</li>
 * </ul>
 *
 * <p>Implementation Conventions:</p>
 * <ul>
 *   <li>{@code createdAt} / {@code updatedAt}: Standard MF-02 JPA audit timestamps.</li>
 *   <li>{@code normalizeCode(String)}: Trimming and uppercase conversion using {@link Locale#ROOT}.</li>
 * </ul>
 */
@Entity
@Table(
        name = "vaccination_centers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vaccination_center_code",
                        columnNames = {"code"}
                )
        }
)
public class VaccinationCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Center code is required")
    @Size(max = 32, message = "Center code must not exceed 32 characters")
    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @NotBlank(message = "Center name is required")
    @Size(max = 128, message = "Center name must not exceed 128 characters")
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @NotBlank(message = "Contact information is required")
    @Size(max = 128, message = "Contact information must not exceed 128 characters")
    @Column(name = "contact", nullable = false, length = 128)
    private String contact;

    @NotNull(message = "Center scheduling timezone is required")
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @NotNull(message = "Center status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "center_status", nullable = false, length = 16)
    private CenterStatus centerStatus;

    @NotNull(message = "Safe concurrent occupancy is required")
    @Positive(message = "Safe concurrent occupancy must be a positive integer")
    @Column(name = "safe_concurrent_occupancy", nullable = false)
    private Integer safeConcurrentOccupancy;

    @Positive(message = "Session service ceiling must be a positive integer when specified")
    @Column(name = "session_service_ceiling")
    private Integer sessionServiceCeiling;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VaccinationCenter() {
    }

    public VaccinationCenter(String code,
                             String name,
                             String address,
                             String contact,
                             CenterStatus centerStatus,
                             Integer safeConcurrentOccupancy,
                             Integer sessionServiceCeiling,
                             String timezone) {
        validateParameters(
                code, name, address, contact, centerStatus,
                safeConcurrentOccupancy, sessionServiceCeiling, timezone);
        this.code = normalizeCode(code);
        this.name = name.trim();
        this.address = address.trim();
        this.contact = contact.trim();
        this.timezone = normalizeTimezone(timezone);
        this.centerStatus = centerStatus;
        this.safeConcurrentOccupancy = safeConcurrentOccupancy;
        this.sessionServiceCeiling = sessionServiceCeiling;
    }

    /**
     * Normalizes center code by trimming and converting to uppercase using Locale.ROOT.
     * Implementation convention to prevent whitespace and casing duplicate collisions.
     *
     * @param rawCode Raw center code string
     * @return Trimmed uppercase code
     */
    public static String normalizeCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Center code must not be null or blank");
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 32) {
            throw new IllegalArgumentException("Center code must not exceed 32 characters: '" + normalized + "'");
        }
        return normalized;
    }

    /**
     * Normalizes an explicitly supplied Java scheduling timezone identifier.
     * No environment or country default is consulted.
     */
    public static String normalizeTimezone(String rawTimezone) {
        if (rawTimezone == null || rawTimezone.trim().isEmpty()) {
            throw new IllegalArgumentException("Center scheduling timezone is required");
        }
        String candidate = rawTimezone.trim();
        try {
            return ZoneId.of(candidate).getId();
        } catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    "Center scheduling timezone is invalid: " + candidate, error);
        }
    }

    /**
     * Validates domain invariants for VaccinationCenter.
     */
    public void validateConstraints() {
        validateParameters(
                code, name, address, contact, centerStatus,
                safeConcurrentOccupancy, sessionServiceCeiling, timezone);
    }

    private static void validateParameters(String code,
                                           String name,
                                           String address,
                                           String contact,
                                           CenterStatus centerStatus,
                                           Integer safeConcurrentOccupancy,
                                           Integer sessionServiceCeiling,
                                           String timezone) {
        normalizeCode(code);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Center name is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (contact == null || contact.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact is required");
        }
        normalizeTimezone(timezone);
        if (centerStatus == null) {
            throw new IllegalArgumentException("Center status is required");
        }
        if (safeConcurrentOccupancy == null || safeConcurrentOccupancy <= 0) {
            throw new IllegalArgumentException("Safe concurrent occupancy must be a positive integer: " + safeConcurrentOccupancy);
        }
        if (sessionServiceCeiling != null && sessionServiceCeiling <= 0) {
            throw new IllegalArgumentException("Session service ceiling must be a positive integer when specified: " + sessionServiceCeiling);
        }
    }

    public void updateDetails(String name,
                              String address,
                              String contact,
                              Integer safeConcurrentOccupancy,
                              Integer sessionServiceCeiling) {
        validateParameters(
                code, name, address, contact, centerStatus,
                safeConcurrentOccupancy, sessionServiceCeiling, timezone);
        this.name = name.trim();
        this.address = address.trim();
        this.contact = contact.trim();
        this.safeConcurrentOccupancy = safeConcurrentOccupancy;
        this.sessionServiceCeiling = sessionServiceCeiling;
    }

    public void activate() {
        centerStatus = CenterStatus.ACTIVE;
    }

    public void deactivate() {
        centerStatus = CenterStatus.INACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        this.code = normalizeCode(this.code);
        this.timezone = normalizeTimezone(this.timezone);
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        validateConstraints();
        this.code = normalizeCode(this.code);
        this.timezone = normalizeTimezone(this.timezone);
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getContact() {
        return contact;
    }

    public String getTimezone() {
        return timezone;
    }

    public CenterStatus getCenterStatus() {
        return centerStatus;
    }

    public Integer getSafeConcurrentOccupancy() {
        return safeConcurrentOccupancy;
    }

    public Integer getSessionServiceCeiling() {
        return sessionServiceCeiling;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "VaccinationCenter{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", timezone='" + timezone + '\'' +
                ", centerStatus=" + centerStatus +
                ", safeConcurrentOccupancy=" + safeConcurrentOccupancy +
                ", sessionServiceCeiling=" + sessionServiceCeiling +
                '}';
    }
}
