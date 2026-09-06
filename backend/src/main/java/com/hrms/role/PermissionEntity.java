package com.hrms.role;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(name = "uq_permissions_module_action", columnNames = {"module", "action"}))
public class PermissionEntity {

  @Id @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 64)
  private String module;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(length = 255)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getModule() { return module; }
  public void setModule(String module) { this.module = module; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Instant getCreatedAt() { return createdAt; }
}
