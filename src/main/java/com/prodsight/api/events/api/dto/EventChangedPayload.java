package com.prodsight.api.events.api.dto;
import java.time.Instant;
import java.util.UUID;

public record EventChangedPayload(
    String eventType,
    UUID eventId,
    UUID userId,
    Instant occurredAt,
    int version
) {
  public static EventChangedPayload created(UUID eventId, UUID userId) {
    return new EventChangedPayload("EventCreated", eventId, userId, Instant.now(), 1);
  }

  public static EventChangedPayload updated(UUID eventId, UUID userId) {
    return new EventChangedPayload("EventUpdated", eventId, userId, Instant.now(), 1);
  }

  public static EventChangedPayload deleted(UUID eventId, UUID userId) {
    return new EventChangedPayload("EventDeleted", eventId, userId, Instant.now(), 1);
  }
}
