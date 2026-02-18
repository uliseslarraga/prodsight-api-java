package com.prodsight.api.users.api;

import com.prodsight.api.users.api.dto.CreateUserRequest;
import com.prodsight.api.users.api.dto.UpdateUserRequest;
import com.prodsight.api.users.api.dto.UserResponse;
import com.prodsight.api.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
    UserResponse res = userService.createOrGetByEmail(req);
    return ResponseEntity.created(URI.create("/api/v1/users/" + res.id())).body(res);
  }

  @GetMapping("/{userId}")
  public UserResponse get(@PathVariable UUID userId) {
    return userService.get(userId);
  }

  @PatchMapping("/{userId}")
  public UserResponse update(@PathVariable UUID userId, @RequestBody UpdateUserRequest req) {
    return userService.update(userId, req);
  }
}
