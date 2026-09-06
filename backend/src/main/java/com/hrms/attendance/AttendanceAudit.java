package com.hrms.attendance;

import jakarta.persistence.*;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

@Entity
@Table(name = "attendance_audit_logs", indexes = @Index(name = "idx_attendance_audit_attendance", columnList = "attendance_id"))
class AttendanceAuditLog {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "attendance_id", nullable = false)
  private UUID attendanceId;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  @Column(nullable = false, length = 32)
  private String action;

  @Column(name = "old_values", columnDefinition = "jsonb")
  private String oldValues;

  @Column(name = "new_values", columnDefinition = "jsonb")
  private String newValues;

  @Column(columnDefinition = "text")
  private String reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() { if (id == null) id = UUID.randomUUID(); }

  public UUID getId() { return id; }
  public UUID getAttendanceId() { return attendanceId; }
  public void setAttendanceId(UUID id) { this.attendanceId = id; }
  public UUID getChangedBy() { return changedBy; }
  public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getOldValues() { return oldValues; }
  public void setOldValues(String v) { this.oldValues = v; }
  public String getNewValues() { return newValues; }
  public void setNewValues(String v) { this.newValues = v; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Instant getCreatedAt() { return createdAt; }
}

interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, UUID> {
  java.util.List<AttendanceAuditLog> findByAttendanceIdOrderByCreatedAtDesc(UUID attendanceId);
}

