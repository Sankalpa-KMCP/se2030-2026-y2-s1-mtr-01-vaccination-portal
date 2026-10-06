package com.se2030.vaccinationportal.screening.questionnaire.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "consent_notice_version",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_consent_notice_version", columnNames = {"notice_code", "version_number"})
    }
)
public class ConsentNoticeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notice_code", nullable = false, length = 64)
    private String noticeCode;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 128)
    private String contentHash;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public ConsentNoticeVersion() {}

    public ConsentNoticeVersion(String noticeCode, Integer versionNumber, String title, String content, String contentHash, Instant publishedAt) {
        this.noticeCode = noticeCode;
        this.versionNumber = versionNumber;
        this.title = title;
        this.content = content;
        this.contentHash = contentHash;
        this.publishedAt = publishedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNoticeCode() { return noticeCode; }
    public void setNoticeCode(String noticeCode) { this.noticeCode = noticeCode; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Instant retiredAt) { this.retiredAt = retiredAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
