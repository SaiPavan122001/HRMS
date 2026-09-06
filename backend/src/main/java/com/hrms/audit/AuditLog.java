package com.hrms.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_logs_entity", columnList = "entity_type,entity_id")})
public class AuditLog {

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "user_email", length = 255)
  private String userEmail;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(name = "old_values", columnDefinition = "jsonb")
  private String oldValues;

  @Column(name = "new_values", columnDefinition = "jsonb")
  private String newValues;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() { if (id == null) id = UUID.randomUUID(); }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public String getUserEmail() { return userEmail; }
  public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public UUID getEntityId() { return entityId; }
  public void setEntityId(UUID entityId) { this.entityId = entityId; }
  public String getOldValues() { return oldValues; }
  public void setOldValues(String oldValues) { this.oldValues = oldValues; }
  public String getNewValues() { return newValues; }
  public void setNewValues(String newValues) { this.newValues = newValues; }
  public String getIpAddress() { return ipAddress; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
  public String getCorrelationId() { return correlationId; }
  public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
  public Instant getCreatedAt() { return createdAt; }
}
