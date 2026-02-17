package com.prodsight.api.users.service;

import com.prodsight.api.users.api.dto.CreateUserRequest;
import com.prodsight.api.users.api.dto.UserResponse;
import com.prodsight.api.users.persistence.UserEntity;

public final class UserMapper {
  private UserMapper() {}

  public static UserEntity toEntity(CreateUserRequest req) {
    UserEntity u = new UserEntity();
    u.setEmail(req.email().trim().toLowerCase());
    u.setDisplayName(req.displayName().trim());
    if (req.timeZone() != null && !req.timeZone().isBlank()) u.setTimeZone(req.timeZone().trim());
    return u;
  }

  public static UserResponse toResponse(UserEntity u) {
    return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getTimeZone(), u.getCreatedAt(), u.getUpdatedAt());
  }
}
