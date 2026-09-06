package com.hrms.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users", indexes = {@Index(name = "idx_users_email", columnList = "email")})
public class User {

  public enum Provider { LOCAL, GOOGLE, GITHUB }
  public enum Status { ACTIVE, SUSPENDED, DEACTIVATED }

  @Id @GeneratedValue
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Email @Column(nullable = false, unique = true)
  private String email;

  /** BCrypt hash. Null for OAuth-only accounts. */
  @Column
  private String password;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  @Column(nullable = false)
  private boolean emailVerified = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Provider provider = Provider.LOCAL;

  @Column(name = "provider_id", length = 128)
  private String providerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

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

  public boolean isLocked() {
    return lockedUntil != null && lockedUntil.isAfter(Instant.now());
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public boolean isEmailVerified() { return emailVerified; }
  public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
  public Provider getProvider() { return provider; }
  public void setProvider(Provider provider) { this.provider = provider; }
  public String getProviderId() { return providerId; }
  public void setProviderId(String providerId) { this.providerId = providerId; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public int getFailedLoginAttempts() { return failedLoginAttempts; }
  public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
  public Instant getLockedUntil() { return lockedUntil; }
  public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
  public Instant getLastLoginAt() { return lastLoginAt; }
  public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
