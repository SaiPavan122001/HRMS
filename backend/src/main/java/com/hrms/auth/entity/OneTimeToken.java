package com.hrms.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Shared shape for email verification and password reset tokens (only SHA-256 hashes stored). */
@Entity
@Table(name = "one_time_tokens", indexes = {
    @Index(name = "idx_one_time_tokens_token_hash", columnList = "token_hash"),
    @Index(name = "idx_one_time_tokens_user_id", columnList = "user_id")})
public class OneTimeToken {

  public enum Purpose { EMAIL_VERIFICATION, PASSWORD_RESET }

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Purpose purpose;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }

  public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
  public boolean isUsed() { return usedAt != null; }
  public boolean isUsable() { return !isExpired() && !isUsed(); }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public Purpose getPurpose() { return purpose; }
  public void setPurpose(Purpose purpose) { this.purpose = purpose; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getUsedAt() { return usedAt; }
  public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
  public Instant getCreatedAt() { return createdAt; }
}
