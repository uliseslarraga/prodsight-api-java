package com.prodsight.api.events.service;

import com.prodsight.api.common.exception.NotFoundException;
import com.prodsight.api.events.api.dto.CreateEventRequest;
import com.prodsight.api.events.api.dto.EventChangedPayload;
import com.prodsight.api.events.api.dto.EventResponse;
import com.prodsight.api.events.api.dto.UpdateEventRequest;
import com.prodsight.api.events.persistence.ActivityEventEntity;
import com.prodsight.api.events.persistence.ActivityEventRepository;
import com.prodsight.api.idempotency.service.IdempotencyService;
import com.prodsight.api.outbox.model.ActivityEventCreatedMessage;
import com.prodsight.api.outbox.service.OutboxWriter;
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
  private final OutboxWriter outboxWriter;

  public ActivityEventService(ActivityEventRepository events, UserService userService, OutboxWriter outboxWriter) {
    this.events = events;
    this.userService = userService;
    this.outboxWriter = outboxWriter;
  }

  @Transactional
  public EventResponse create(UUID userId, String idempotencyKey, CreateEventRequest req) {
    UserEntity user = userService.requireEntity(userId);

    ActivityEventEntity entity = ActivityEventMapper.toEntity(user, req);

    // 1) Business write
    ActivityEventEntity saved = events.save(entity);

    // At this point, saved.getId() is available (UUID)
    // 2) Outbox write in SAME TX
    outboxWriter.enqueue(
    		  "ActivityEvent",
    		  saved.getId(),
    		  "EventCreated",
    		  ActivityEventCreatedMessage.of(saved.getId(), userId)
    		);


    // 3) return response
    return ActivityEventMapper.toResponse(saved);
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
    ActivityEventEntity saved = events.save(e);

    outboxWriter.enqueue(
        "ActivityEvent",
        saved.getId(),
        "EventUpdated",
        EventChangedPayload.updated(saved.getId(), userId)
    );

    return ActivityEventMapper.toResponse(saved);
  }

  @Transactional
  public void delete(UUID userId, UUID eventId) {
    ActivityEventEntity e = events.findByIdAndUserId(eventId, userId)
        .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
    events.delete(e);
  }
}
