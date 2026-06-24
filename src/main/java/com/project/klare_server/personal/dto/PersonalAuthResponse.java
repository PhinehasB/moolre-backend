package com.project.klare_server.personal.dto;

import java.time.Instant;

public record PersonalAuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        boolean mustChangePassword,
        PersonalAccountResponse account) {
}
