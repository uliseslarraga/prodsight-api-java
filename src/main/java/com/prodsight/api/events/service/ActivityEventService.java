package com.prodsight.api.events.service;

import com.prodsight.api.common.exception.NotFoundException;
import com.prodsight.api.events.api.dto.CreateEventRequest;
import com.prodsight.api.events.api.dto.EventResponse;
import com.prodsight.api.events.api.dto.UpdateEventRequest;
import com.prodsight.api.events.persistence.ActivityEventEntity;
import com.prodsight.api.events.persistence.ActivityEventRepository;
import com.prodsight.api.idempotency.service.IdempotencyService;
import com.prodsight.api.users.persistence.UserEntity;
import com.prodsight.api.users.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ActivityEventService {

  private final ActivityEventRepository events;
  private final UserService userService;
  private final IdempotencyService idempotencyService;

  public ActivityEventService(ActivityEventRepository events, UserService userService, IdempotencyService idempotencyService) {
    this.events = events;
    this.userService = userService;
    this.idempotencyService = idempotencyService;
  }

  @Transactional
  public EventResponse create(UUID userId, String idempotencyKey, CreateEventRequest req) {
    UserEntity user = userService.requireEntity(userId);

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      var stored = idempotencyService.findIfExists(userId, idempotencyKey, req);
      if (stored.isPresent()) {
        // For MVP: assume response body matches EventResponse shape.
        // In production you'd store exact response and map back carefully.
        Map<String, Object> body = stored.get().body();
        // Better: store eventId and re-load; keeping simple for now:
        throw new UnsupportedOperationException("Stored-response replay mapping not implemented yet (store eventId and re-load is recommended).");
      }
    }

    ActivityEventEntity entity = ActivityEventMapper.toEntity(user, req);
    ActivityEventEntity saved = events.save(entity);

    EventResponse response = ActivityEventMapper.toResponse(saved);

    // Recommended MVP idempotency implementation: store eventId and statusCode, then re-load on retry
    // Here’s a simple placeholder; implement properly when ready.
    // idempotencyService.store(user, idempotencyKey, req, 201, Map.of("eventId", saved.getId().toString()));

    return response;
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> list(UUID userId, Instant from, Instant to, Pageable pageable) {
    return events.findByUserIdAndStartedAtBetween(userId, from, to, pageable)
        .map(ActivityEventMapper::toResponse);
  }

  @Transactional
  public EventResponse update(UUID userId, UUID eventId, UpdateEventRequest req) {
    ActivityEventEntity e = events.findByIdAndUserId(eventId, userId)
        .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    ActivityEventMapper.applyUpdate(e, req);
    return ActivityEventMapper.toResponse(events.save(e));
  }

  @Transactional
  public void delete(UUID userId, UUID eventId) {
    ActivityEventEntity e = events.findByIdAndUserId(eventId, userId)
        .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    events.delete(e);
  }
}
