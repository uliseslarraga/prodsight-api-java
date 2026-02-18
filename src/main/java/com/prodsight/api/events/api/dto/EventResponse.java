package com.prodsight.api.events.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
    UUID id,
    UUID userId,
    String type,
    String source,
    Instant startedAt,
    Instant endedAt,
    long durationSeconds,
    List<String> tags,
    Map<String, Object> metadata,
    Instant createdAt
) {}
