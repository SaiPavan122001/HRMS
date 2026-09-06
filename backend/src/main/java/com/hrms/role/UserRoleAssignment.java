package com.hrms.role;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_role_assignments", uniqueConstraints = @UniqueConstraint(name = "uq_user_role_assignments", columnNames = {"user_id", "role_id"}), indexes = @Index(name = "idx_ura_user_id", columnList = "user_id"))
public class UserRoleAssignment {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private RoleEntity role;

  @Column(name = "assigned_by")
  private UUID assignedBy;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (assignedAt == null) assignedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public RoleEntity getRole() { return role; }
  public void setRole(RoleEntity role) { this.role = role; }
  public UUID getAssignedBy() { return assignedBy; }
  public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }
  public Instant getAssignedAt() { return assignedAt; }
}
