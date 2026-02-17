package com.prodsight.api.idempotency.persistence;

import com.prodsight.api.users.persistence.UserEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "idempotency_keys",
    schema = "prodsight",
    uniqueConstraints = @UniqueConstraint(
        name = "idempotency_keys_user_key_uq",
        columnNames = {"user_id", "idempotency_key"}
    ),
    indexes = @Index(name = "idx_idempotency_keys_created_at", columnList = "created_at")
)
public class IdempotencyKeyEntity {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false)
  private String requestHash;

  @Column(name = "response_code", nullable = false)
  private int responseCode;

  @Type(JsonType.class)
  @Column(name = "response_body", columnDefinition = "jsonb")
  private Map<String, Object> responseBody;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() { this.createdAt = Instant.now(); }

  // getters/setters
  public UUID getId() { return id; }

  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }

  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

  public String getRequestHash() { return requestHash; }
  public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

  public int getResponseCode() { return responseCode; }
  public void setResponseCode(int responseCode) { this.responseCode = responseCode; }

  public Map<String, Object> getResponseBody() { return responseBody; }
  public void setResponseBody(Map<String, Object> responseBody) { this.responseBody = responseBody; }

  public Instant getCreatedAt() { return createdAt; }
}
