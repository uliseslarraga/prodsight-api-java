package com.prodsight.api.outbox.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, columnDefinition = "uuid")
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "headers", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> headers = Map.of("contentType", "application/json");

  @Column(name = "status", nullable = false)
  private String status = "PENDING";

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount = 0;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt = Instant.now();

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "locked_by")
  private String lockedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "last_error")
  private String lastError;

  // getters/setters

  public UUID getId() { return id; }

  public String getAggregateType() { return aggregateType; }
  public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

  public UUID getAggregateId() { return aggregateId; }
  public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }

  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }

  public Map<String, Object> getPayload() { return payload; }
  public void setPayload(Map<String, Object> payload) { this.payload = payload; }

  public Map<String, Object> getHeaders() { return headers; }
  public void setHeaders(Map<String, Object> headers) { this.headers = headers; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public int getAttemptCount() { return attemptCount; }
  public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

  public Instant getAvailableAt() { return availableAt; }
  public void setAvailableAt(Instant availableAt) { this.availableAt = availableAt; }

  public Instant getLockedAt() { return lockedAt; }
  public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }

  public String getLockedBy() { return lockedBy; }
  public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

  public Instant getCreatedAt() { return createdAt; }

  public Instant getSentAt() { return sentAt; }
  public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

  public String getLastError() { return lastError; }
  public void setLastError(String lastError) { this.lastError = lastError; }
}
