package com.hrms.role;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Legacy quick-role table kept for compatibility with the original Supabase data model. */
@Entity
@Table(name = "user_roles", uniqueConstraints = @UniqueConstraint(name = "uq_user_roles", columnNames = {"user_id", "role"}))
public class UserRoleLegacy {

  public enum AppRole { SUPER_ADMIN, HR, MANAGER, EMPLOYEE }

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AppRole role = AppRole.EMPLOYEE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public UUID getProfileId() { return profileId; }
  public void setProfileId(UUID profileId) { this.profileId = profileId; }
  public AppRole getRole() { return role; }
  public void setRole(AppRole role) { this.role = role; }
  public Instant getCreatedAt() { return createdAt; }
}
