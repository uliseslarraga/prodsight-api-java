package com.prodsight.api.events.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CreateEventRequest(
    @NotBlank String type,
    String source,
    @NotNull Instant startedAt,
    Instant endedAt,
    List<String> tags,
    Map<String, Object> metadata
) {}
