package com.se2030.vaccinationportal.campaign.model;

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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

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
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VaccinationCenter() {
    }

    public VaccinationCenter(String code,
                             String name,
                             String address,
                             String contact,
                             CenterStatus centerStatus,
                             Integer safeConcurrentOccupancy,
                             Integer sessionServiceCeiling) {
        this.code = normalizeCode(code);
        this.name = name != null ? name.trim() : null;
        this.address = address != null ? address.trim() : null;
        this.contact = contact != null ? contact.trim() : null;
        this.centerStatus = centerStatus;
        this.safeConcurrentOccupancy = safeConcurrentOccupancy;
        this.sessionServiceCeiling = sessionServiceCeiling;
        validateConstraints();
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
     * Validates domain invariants for VaccinationCenter.
     */
    public void validateConstraints() {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Center code is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Center name is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (contact == null || contact.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact is required");
        }
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

    @PrePersist
    protected void onCreate() {
        validateConstraints();
        this.code = normalizeCode(this.code);
        LocalDateTime now = LocalDateTime.now();
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
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = normalizeCode(code);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address != null ? address.trim() : null;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact != null ? contact.trim() : null;
    }

    public CenterStatus getCenterStatus() {
        return centerStatus;
    }

    public void setCenterStatus(CenterStatus centerStatus) {
        this.centerStatus = centerStatus;
    }

    public Integer getSafeConcurrentOccupancy() {
        return safeConcurrentOccupancy;
    }

    public void setSafeConcurrentOccupancy(Integer safeConcurrentOccupancy) {
        this.safeConcurrentOccupancy = safeConcurrentOccupancy;
    }

    public Integer getSessionServiceCeiling() {
        return sessionServiceCeiling;
    }

    public void setSessionServiceCeiling(Integer sessionServiceCeiling) {
        this.sessionServiceCeiling = sessionServiceCeiling;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaccinationCenter that = (VaccinationCenter) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : code);
    }

    @Override
    public String toString() {
        return "VaccinationCenter{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", centerStatus=" + centerStatus +
                ", safeConcurrentOccupancy=" + safeConcurrentOccupancy +
                ", sessionServiceCeiling=" + sessionServiceCeiling +
                '}';
    }
}
