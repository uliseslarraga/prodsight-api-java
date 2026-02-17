package com.prodsight.api.events.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UpdateEventRequest(
    String type,
    String source,
    Instant startedAt,
    Instant endedAt,
    List<String> tags,
    Map<String, Object> metadata
) {}
