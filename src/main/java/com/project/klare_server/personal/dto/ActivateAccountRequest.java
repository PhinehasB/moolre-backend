package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivateAccountRequest(
        @NotBlank(message = "New password is required") String newPassword,
        @NotBlank(message = "Please confirm your password") String confirmPassword) {
}
