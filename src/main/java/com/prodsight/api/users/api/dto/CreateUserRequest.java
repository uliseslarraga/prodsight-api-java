package com.prodsight.api.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @Email @NotBlank String email,
    @NotBlank String displayName,
    String timeZone
) {}
