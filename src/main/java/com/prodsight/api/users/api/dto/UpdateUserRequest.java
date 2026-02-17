package com.prodsight.api.users.api.dto;

public record UpdateUserRequest(
    String displayName,
    String timeZone
) {}
