package com.hrms.events;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Transactional outbox: written in the same DB transaction as the business change, dispatched to Kafka asynchronously. */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_unprocessed", columnList = "processedAt")})
public class OutboxEvent {

  @Id @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 64)
  private String type;

  @Column(name = "aggregate_type", length = 64)
  private String aggregateType;

  @Column(name = "aggregate_id")
  private UUID aggregateId;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(nullable = false)
  private int retries = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }

  public UUID getId() { return id; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getAggregateType() { return aggregateType; }
  public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
  public UUID getAggregateId() { return aggregateId; }
  public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public Instant getProcessedAt() { return processedAt; }
  public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
  public int getRetries() { return retries; }
  public void setRetries(int retries) { this.retries = retries; }
  public Instant getCreatedAt() { return createdAt; }
}
