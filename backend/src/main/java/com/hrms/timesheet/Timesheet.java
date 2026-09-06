package com.hrms.timesheet;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "timesheets", uniqueConstraints = @UniqueConstraint(name = "uq_timesheets_profile_week", columnNames = {"profile_id", "week_start_date"}), indexes = @Index(name = "idx_timesheets_profile_week", columnList = "profile_id,week_start_date"))
public class Timesheet {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(name = "week_start_date", nullable = false)
  private LocalDate weekStartDate;

  @Column(name = "week_end_date", nullable = false)
  private LocalDate weekEndDate;

  @Column(name = "total_hours", precision = 5, scale = 2)
  private BigDecimal totalHours = BigDecimal.ZERO;

  @Column(nullable = false, length = 20)
  private String status = "draft";

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by")
  private UUID approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private Long version;

  @PrePersist
  void prePersist() { if (id == null) id = UUID.randomUUID(); }

  public UUID getId() { return id; }
  public UUID getProfileId() { return profileId; }
  public void setProfileId(UUID id) { this.profileId = id; }
  public LocalDate getWeekStartDate() { return weekStartDate; }
  public void setWeekStartDate(LocalDate d) { this.weekStartDate = d; }
  public LocalDate getWeekEndDate() { return weekEndDate; }
  public void setWeekEndDate(LocalDate d) { this.weekEndDate = d; }
  public BigDecimal getTotalHours() { return totalHours; }
  public void setTotalHours(BigDecimal h) { this.totalHours = h; }
  public String getStatus() { return status; }
  public void setStatus(String s) { this.status = s; }
  public Instant getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(Instant t) { this.submittedAt = t; }
  public UUID getApprovedBy() { return approvedBy; }
  public void setApprovedBy(UUID id) { this.approvedBy = id; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant t) { this.approvedAt = t; }
  public String getRejectionReason() { return rejectionReason; }
  public void setRejectionReason(String r) { this.rejectionReason = r; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
