package com.project.klare_server.auth.dto;

import java.time.Instant;

public record AuthenticationResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        AuthUserResponse user,
        CompanyResponse company) {
}
