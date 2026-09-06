package com.hrms.leave;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "leave_requests", indexes = {
    @Index(name = "idx_leave_requests_profile_status", columnList = "profile_id,status"),
    @Index(name = "idx_leave_requests_status", columnList = "status")})
public class LeaveRequest {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(name = "leave_type_id", nullable = false)
  private UUID leaveTypeId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "days_requested", nullable = false, precision = 4, scale = 2)
  private BigDecimal daysRequested;

  @Column(columnDefinition = "text")
  private String reason;

  @Column(nullable = false, length = 20)
  private String status = "pending";

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
  public UUID getLeaveTypeId() { return leaveTypeId; }
  public void setLeaveTypeId(UUID id) { this.leaveTypeId = id; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate d) { this.startDate = d; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate d) { this.endDate = d; }
  public BigDecimal getDaysRequested() { return daysRequested; }
  public void setDaysRequested(BigDecimal d) { this.daysRequested = d; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public UUID getApprovedBy() { return approvedBy; }
  public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
  public String getRejectionReason() { return rejectionReason; }
  public void setRejectionReason(String reason) { this.rejectionReason = reason; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
