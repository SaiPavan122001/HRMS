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
@Table(name = "attendance_policies")
class AttendancePolicy {

  @Id @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "working_hours_start", nullable = false)
  private LocalTime workingHoursStart = LocalTime.of(9, 0);

  @Column(name = "working_hours_end", nullable = false)
  private LocalTime workingHoursEnd = LocalTime.of(17, 0);

  @Column(name = "break_duration_minutes")
  private int breakDurationMinutes = 60;

  @Column(name = "grace_period_minutes")
  private int gracePeriodMinutes = 15;

  @Column(name = "overtime_threshold_hours", precision = 5, scale = 2)
  private BigDecimal overtimeThresholdHours = BigDecimal.valueOf(8);

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() { if (id == null) id = UUID.randomUUID(); }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public LocalTime getWorkingHoursStart() { return workingHoursStart; }
  public void setWorkingHoursStart(LocalTime t) { this.workingHoursStart = t; }
  public LocalTime getWorkingHoursEnd() { return workingHoursEnd; }
  public void setWorkingHoursEnd(LocalTime t) { this.workingHoursEnd = t; }
  public int getBreakDurationMinutes() { return breakDurationMinutes; }
  public void setBreakDurationMinutes(int m) { this.breakDurationMinutes = m; }
  public int getGracePeriodMinutes() { return gracePeriodMinutes; }
  public void setGracePeriodMinutes(int m) { this.gracePeriodMinutes = m; }
  public BigDecimal getOvertimeThresholdHours() { return overtimeThresholdHours; }
  public void setOvertimeThresholdHours(BigDecimal h) { this.overtimeThresholdHours = h; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

@Entity
@Table(name = "holidays", indexes = @Index(name = "idx_holidays_date", columnList = "date"))
class Holiday {

  @Id @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "is_recurring", nullable = false)
  private boolean recurring = false;

  private Integer year;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() { if (id == null) id = UUID.randomUUID(); }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public LocalDate getDate() { return date; }
  public void setDate(LocalDate date) { this.date = date; }
  public boolean isRecurring() { return recurring; }
  public void setRecurring(boolean recurring) { this.recurring = recurring; }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
