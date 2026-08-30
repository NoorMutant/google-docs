package com.ajaia.docs.web.dto;

import com.ajaia.docs.domain.ShareRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address") String email,
        @NotNull(message = "Pick a role") ShareRole role) {
}
