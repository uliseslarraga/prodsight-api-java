package com.prodsight.api.events.persistence;

import com.prodsight.api.users.persistence.UserEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.*;

@Entity
@Table(
    name = "activity_events",
    indexes = {
        @Index(name = "idx_activity_events_user_started_at", columnList = "user_id, started_at"),
        @Index(name = "idx_activity_events_user_type_started_at", columnList = "user_id, type, started_at")
    }
)
public class ActivityEventEntity {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid")
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @NotBlank
  @Column(nullable = false)
  private String type;

  @NotBlank
  @Column(nullable = false)
  private String source = "manual";

  @NotNull
  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "duration_seconds", nullable = false)
  private long durationSeconds;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb", nullable = false)
  private List<String> tags = new ArrayList<>();

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> metadata = new HashMap<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
  }

  // getters/setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }

  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

  public Instant getEndedAt() { return endedAt; }
  public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

  public long getDurationSeconds() { return durationSeconds; }
  public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

  public List<String> getTags() { return tags; }
  public void setTags(List<String> tags) { this.tags = (tags == null) ? new ArrayList<>() : tags; }

  public Map<String, Object> getMetadata() { return metadata; }
  public void setMetadata(Map<String, Object> metadata) { this.metadata = (metadata == null) ? new HashMap<>() : metadata; }

  public Instant getCreatedAt() { return createdAt; }
}
