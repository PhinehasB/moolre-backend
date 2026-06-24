package com.project.klare_server.personal.dto;

import jakarta.validation.constraints.NotBlank;

public record PersonalRefreshRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
