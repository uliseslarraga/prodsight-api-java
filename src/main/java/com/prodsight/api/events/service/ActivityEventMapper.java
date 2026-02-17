package com.prodsight.api.events.service;

import com.prodsight.api.events.api.dto.CreateEventRequest;
import com.prodsight.api.events.api.dto.EventResponse;
import com.prodsight.api.events.api.dto.UpdateEventRequest;
import com.prodsight.api.events.persistence.ActivityEventEntity;
import com.prodsight.api.users.persistence.UserEntity;

import java.util.ArrayList;
import java.util.HashMap;

public final class ActivityEventMapper {
  private ActivityEventMapper() {}

  public static ActivityEventEntity toEntity(UserEntity user, CreateEventRequest req) {
    ActivityEventEntity e = new ActivityEventEntity();
    e.setUser(user);
    e.setType(req.type().trim());
    e.setSource((req.source() == null || req.source().isBlank()) ? "manual" : req.source().trim());
    e.setStartedAt(req.startedAt());
    e.setEndedAt(req.endedAt());
    e.setTags(req.tags() == null ? new ArrayList<>() : req.tags());
    e.setMetadata(req.metadata() == null ? new HashMap<>() : req.metadata());
    return e;
  }

  public static void applyUpdate(ActivityEventEntity e, UpdateEventRequest req) {
    if (req.type() != null && !req.type().isBlank()) e.setType(req.type().trim());
    if (req.source() != null && !req.source().isBlank()) e.setSource(req.source().trim());
    if (req.startedAt() != null) e.setStartedAt(req.startedAt());
    if (req.endedAt() != null) e.setEndedAt(req.endedAt());
    if (req.tags() != null) e.setTags(req.tags());
    if (req.metadata() != null) e.setMetadata(req.metadata());
  }

  public static EventResponse toResponse(ActivityEventEntity e) {
    return new EventResponse(
        e.getId(),
        e.getUser().getId(),
        e.getType(),
        e.getSource(),
        e.getStartedAt(),
        e.getEndedAt(),
        e.getDurationSeconds(),
        e.getTags(),
        e.getMetadata(),
        e.getCreatedAt()
    );
  }
}
