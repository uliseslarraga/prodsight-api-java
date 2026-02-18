package com.prodsight.api.users.api.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    String timeZone,
    Instant createdAt,
    Instant updatedAt
) {}
