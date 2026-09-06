package com.hrms.role;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "roles")
public class RoleEntity {

  @Id @GeneratedValue
  private UUID id;

  @Column(nullable = false, unique = true, length = 64)
  private String name;

  @Column(length = 255)
  private String description;

  @Column(name = "is_system_role", nullable = false)
  private boolean systemRole = false;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private Long version;

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public boolean isSystemRole() { return systemRole; }
  public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
