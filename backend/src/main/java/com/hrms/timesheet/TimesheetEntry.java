package com.hrms.timesheet;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "timesheet_entries", indexes = @Index(name = "idx_timesheet_entries_timesheet", columnList = "timesheet_id"))
public class TimesheetEntry {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "timesheet_id", nullable = false)
  private UUID timesheetId;

  @Column(nullable = false)
  private LocalDate date;

  @Column(nullable = false, precision = 4, scale = 2)
  private BigDecimal hours = BigDecimal.ZERO;

  @Column(name = "project_name", length = 128)
  private String projectName;

  @Column(name = "task_description", columnDefinition = "text")
  private String taskDescription;

  @Column(name = "is_billable", nullable = false)
  private boolean billable = false;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() { if (id == null) id = UUID.randomUUID(); }

  public UUID getId() { return id; }
  public UUID getTimesheetId() { return timesheetId; }
  public void setTimesheetId(UUID id) { this.timesheetId = id; }
  public LocalDate getDate() { return date; }
  public void setDate(LocalDate d) { this.date = d; }
  public BigDecimal getHours() { return hours; }
  public void setHours(BigDecimal h) { this.hours = h; }
  public String getProjectName() { return projectName; }
  public void setProjectName(String p) { this.projectName = p; }
  public String getTaskDescription() { return taskDescription; }
  public void setTaskDescription(String t) { this.taskDescription = t; }
  public boolean isBillable() { return billable; }
  public void setBillable(boolean b) { this.billable = b; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
