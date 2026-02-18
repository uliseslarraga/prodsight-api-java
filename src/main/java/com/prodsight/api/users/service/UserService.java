package com.prodsight.api.users.service;

import com.prodsight.api.common.exception.NotFoundException;
import com.prodsight.api.users.api.dto.CreateUserRequest;
import com.prodsight.api.users.api.dto.UpdateUserRequest;
import com.prodsight.api.users.api.dto.UserResponse;
import com.prodsight.api.users.persistence.UserEntity;
import com.prodsight.api.users.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

  private final UserRepository users;

  public UserService(UserRepository users) {
    this.users = users;
  }

  @Transactional
  public UserResponse createOrGetByEmail(CreateUserRequest req) {
    String normalizedEmail = req.email().trim().toLowerCase();
    return users.findByEmailIgnoreCase(normalizedEmail)
        .map(UserMapper::toResponse)
        .orElseGet(() -> {
          UserEntity created = UserMapper.toEntity(req);
          created.setEmail(normalizedEmail);
          return UserMapper.toResponse(users.save(created));
        });
  }

  @Transactional(readOnly = true)
  public UserEntity requireEntity(UUID userId) {
    return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
  }

  @Transactional(readOnly = true)
  public UserResponse get(UUID userId) {
    return UserMapper.toResponse(requireEntity(userId));
  }

  @Transactional
  public UserResponse update(UUID userId, UpdateUserRequest req) {
    UserEntity u = requireEntity(userId);
    if (req.displayName() != null && !req.displayName().isBlank()) u.setDisplayName(req.displayName().trim());
    if (req.timeZone() != null && !req.timeZone().isBlank()) u.setTimeZone(req.timeZone().trim());
    return UserMapper.toResponse(users.save(u));
  }
}
