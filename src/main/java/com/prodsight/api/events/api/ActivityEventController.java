package com.prodsight.api.events.api;

import com.prodsight.api.common.constants.Headers;
import com.prodsight.api.events.api.dto.CreateEventRequest;
import com.prodsight.api.events.api.dto.EventResponse;
import com.prodsight.api.events.api.dto.UpdateEventRequest;
import com.prodsight.api.events.service.ActivityEventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/events")
public class ActivityEventController {

  private final ActivityEventService service;

  public ActivityEventController(ActivityEventService service) {
    this.service = service;
  }

  @PostMapping
  public EventResponse create(
      @PathVariable UUID userId,
      @RequestHeader(value = Headers.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
      @Valid @RequestBody CreateEventRequest req
  ) {
    return service.create(userId, idempotencyKey, req);
  }

  @GetMapping
  public Page<EventResponse> list(
      @PathVariable UUID userId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int page
  ) {
    var pageable = PageRequest.of(page, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "startedAt"));
    return service.list(userId, from, to, pageable);
  }

  @PatchMapping("/{eventId}")
  public EventResponse update(
      @PathVariable UUID userId,
      @PathVariable UUID eventId,
      @RequestBody UpdateEventRequest req
  ) {
    return service.update(userId, eventId, req);
  }

  @DeleteMapping("/{eventId}")
  public void delete(@PathVariable UUID userId, @PathVariable UUID eventId) {
    service.delete(userId, eventId);
  }
}
