package com.hrms.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record HrmsEvent(UUID eventId, String type, Map<String, Object> payload, Instant occurredAt) {
  public HrmsEvent(String type, Map<String, Object> payload) {
    this(UUID.randomUUID(), type, payload, Instant.now());
  }
}
