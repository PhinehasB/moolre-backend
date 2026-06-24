package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.NotBlank;

public record PersonalLogoutRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
