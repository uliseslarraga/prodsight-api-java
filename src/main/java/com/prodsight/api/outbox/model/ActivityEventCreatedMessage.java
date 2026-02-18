package com.prodsight.api.outbox.model;

import java.time.Instant;
import java.util.UUID;

public record ActivityEventCreatedMessage(
    String eventType,
    UUID eventId,
    UUID userId,
    Instant occurredAt,
    int version
) {
  public static ActivityEventCreatedMessage of(UUID eventId, UUID userId) {
    return new ActivityEventCreatedMessage("EventCreated", eventId, userId, Instant.now(), 1);
  }
}
