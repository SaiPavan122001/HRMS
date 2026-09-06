package com.hrms.role;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_permissions", uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions", columnNames = {"role_id", "permission_id"}))
public class RolePermission implements Serializable {

  @Id @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private RoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "permission_id", nullable = false)
  private PermissionEntity permission;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() { return id; }
  public RoleEntity getRole() { return role; }
  public void setRole(RoleEntity role) { this.role = role; }
  public PermissionEntity getPermission() { return permission; }
  public void setPermission(PermissionEntity permission) { this.permission = permission; }
  public Instant getCreatedAt() { return createdAt; }
}
