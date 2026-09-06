package com.hrms.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional outbox dispatcher.
 * Business services call append() inside their transaction; this poller publishes
 * to Kafka and marks rows processed. Publish failures are retried on the next pass.
 */
@Service
public class OutboxService {

  private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
  private final OutboxRepository repository;
  private final KafkaTemplate<String, HrmsEvent> kafkaTemplate;
  private final ObjectMapper mapper;
  private final String eventsTopic;

  public OutboxService(OutboxRepository repository, KafkaTemplate<String, HrmsEvent> kafkaTemplate,
                       ObjectMapper mapper,
                       @org.springframework.beans.factory.annotation.Value("${spring.kafka.topic.hrms-events}") String eventsTopic) {
    this.repository = repository;
    this.kafkaTemplate = kafkaTemplate;
    this.mapper = mapper;
    this.eventsTopic = eventsTopic;
  }

  /** Must be called within the caller's transaction. */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void append(String type, String aggregateType, UUID aggregateId, Map<String, Object> payload) {
    try {
      OutboxEvent event = new OutboxEvent();
      event.setType(type);
      event.setAggregateType(aggregateType);
      event.setAggregateId(aggregateId);
      event.setPayload(mapper.writeValueAsString(new HrmsEvent(type, payload)));
      repository.save(event);
    } catch (Exception e) {
      log.error("Failed to append outbox event {}: {}", type, e.getMessage());
    }
  }

  @Transactional
  public void dispatchPending() {
    for (OutboxEvent event : repository.findTop100ByProcessedAtIsNullOrderByCreatedAtAsc()) {
      try {
        HrmsEvent hrmsEvent = mapper.readValue(event.getPayload(), HrmsEvent.class);
        kafkaTemplate.send(eventsTopic, String.valueOf(event.getAggregateId()), hrmsEvent).get();
        event.setProcessedAt(java.time.Instant.now());
        repository.save(event);
      } catch (Exception e) {
        event.setRetries(event.getRetries() + 1);
        repository.save(event);
        log.warn("Outbox dispatch failed for event {} (type {}): {}", event.getId(), event.getType(), e.getMessage());
      }
    }
  }

  /** Purge processed outbox rows older than 7 days. */
  @Transactional
  public void purgeProcessed() {
    repository.deleteByProcessedAtBefore(java.time.Instant.now().minus(java.time.Duration.ofDays(7)));
  }
}

