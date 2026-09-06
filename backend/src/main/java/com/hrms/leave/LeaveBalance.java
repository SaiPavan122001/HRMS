package com.hrms.leave;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(name = "uq_leave_balances", columnNames = {"profile_id", "leave_type_id", "year"}), indexes = @Index(name = "idx_leave_balances_profile", columnList = "profile_id"))
public class LeaveBalance {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(name = "leave_type_id", nullable = false)
  private UUID leaveTypeId;

  @Column(nullable = false)
  private int year;

  @Column(name = "total_days", nullable = false, precision = 4, scale = 2)
  private BigDecimal totalDays = BigDecimal.ZERO;

  @Column(name = "used_days", nullable = false, precision = 4, scale = 2)
  private BigDecimal usedDays = BigDecimal.ZERO;

  @Column(name = "remaining_days", nullable = false, precision = 4, scale = 2)
  private BigDecimal remainingDays = BigDecimal.ZERO;

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
  public int getYear() { return year; }
  public void setYear(int year) { this.year = year; }
  public BigDecimal getTotalDays() { return totalDays; }
  public void setTotalDays(BigDecimal d) { this.totalDays = d; }
  public BigDecimal getUsedDays() { return usedDays; }
  public void setUsedDays(BigDecimal d) { this.usedDays = d; }
  public BigDecimal getRemainingDays() { return remainingDays; }
  public void setRemainingDays(BigDecimal d) { this.remainingDays = d; }
  public Long getVersion() { return version; }
}
