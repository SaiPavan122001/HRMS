package com.hrms.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_id", columnList = "user_id"),
    @Index(name = "idx_notifications_user_status", columnList = "user_id,status")})
public class Notification {

  public enum Type { EMAIL, IN_APP }
  public enum Status { PENDING, SENT, FAILED, READ }

  @Id @GeneratedValue
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Type type = Type.IN_APP;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "message", columnDefinition = "text")
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status = Status.PENDING;

  @Column(name = "reference_type", length = 64)
  private String referenceType;

  @Column(name = "reference_id")
  private UUID referenceId;

  @Column(name = "link", length = 255)
  private String link;

  @Column(name = "email_to", length = 255)
  private String emailTo;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }

  public UUID getId() { return id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public Type getType() { return type; }
  public void setType(Type type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public String getReferenceType() { return referenceType; }
  public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
  public UUID getReferenceId() { return referenceId; }
  public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }
  public String getLink() { return link; }
  public void setLink(String link) { this.link = link; }
  public String getEmailTo() { return emailTo; }
  public void setEmailTo(String emailTo) { this.emailTo = emailTo; }
  public Instant getReadAt() { return readAt; }
  public void setReadAt(Instant readAt) { this.readAt = readAt; }
  public Instant getSentAt() { return sentAt; }
  public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
