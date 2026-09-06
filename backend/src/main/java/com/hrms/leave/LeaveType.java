package com.hrms.leave;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "leave_types")
public class LeaveType {

  @Id @GeneratedValue
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "max_days_per_year")
  private Integer maxDaysPerYear;

  @Column(name = "is_carry_forward", nullable = false)
  private boolean carryForward = false;

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
  public Integer getMaxDaysPerYear() { return maxDaysPerYear; }
  public void setMaxDaysPerYear(Integer m) { this.maxDaysPerYear = m; }
  public boolean isCarryForward() { return carryForward; }
  public void setCarryForward(boolean c) { this.carryForward = c; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
