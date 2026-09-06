package com.hrms.attendance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(name = "uq_attendance_profile_date", columnNames = {"profile_id", "date"}), indexes = @Index(name = "idx_attendance_profile_date", columnList = "profile_id,date"))
public class Attendance {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(name = "check_in_time")
  private Instant checkInTime;

  @Column(name = "check_out_time")
  private Instant checkOutTime;

  @Column(name = "break_start_time")
  private Instant breakStartTime;

  @Column(name = "break_end_time")
  private Instant breakEndTime;

  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "total_hours", precision = 4, scale = 2)
  private BigDecimal totalHours;

  @Column(nullable = false, length = 20)
  private String status = "present";

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "is_corrected", nullable = false)
  private boolean corrected = false;

  @Column(name = "corrected_by")
  private UUID correctedBy;

  @Column(name = "correction_reason", columnDefinition = "text")
  private String correctionReason;

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
  public void setProfileId(UUID profileId) { this.profileId = profileId; }
  public Instant getCheckInTime() { return checkInTime; }
  public void setCheckInTime(Instant checkInTime) { this.checkInTime = checkInTime; }
  public Instant getCheckOutTime() { return checkOutTime; }
  public void setCheckOutTime(Instant checkOutTime) { this.checkOutTime = checkOutTime; }
  public Instant getBreakStartTime() { return breakStartTime; }
  public void setBreakStartTime(Instant breakStartTime) { this.breakStartTime = breakStartTime; }
  public Instant getBreakEndTime() { return breakEndTime; }
  public void setBreakEndTime(Instant breakEndTime) { this.breakEndTime = breakEndTime; }
  public LocalDate getDate() { return date; }
  public void setDate(LocalDate date) { this.date = date; }
  public BigDecimal getTotalHours() { return totalHours; }
  public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public boolean isCorrected() { return corrected; }
  public void setCorrected(boolean corrected) { this.corrected = corrected; }
  public UUID getCorrectedBy() { return correctedBy; }
  public void setCorrectedBy(UUID correctedBy) { this.correctedBy = correctedBy; }
  public String getCorrectionReason() { return correctionReason; }
  public void setCorrectionReason(String correctionReason) { this.correctionReason = correctionReason; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
