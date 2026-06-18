package com.project.klare_server.common.config.properties;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "klare.security")
public record SecurityProperties(Jwt jwt, RefreshToken refreshToken, Login login, PasswordReset passwordReset, Cors cors) {

    public record Jwt(
            String secret,
            @DefaultValue("klare-business") String issuer,
            @DefaultValue("PT15M") Duration accessTokenTtl,
            @DefaultValue("P30D") Duration refreshTokenTtl,
            @DefaultValue("PT12H") Duration sessionTokenTtl) {
    }

    public record RefreshToken(String pepper) {
    }

    public record Login(
            @DefaultValue("5") int maxFailedAttempts,
            @DefaultValue("PT15M") Duration lockDuration) {
    }

    public record PasswordReset(@DefaultValue("PT30M") Duration tokenTtl) {
    }

    public record Cors(
            List<String> allowedOrigins,
            @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}) List<String> allowedMethods,
            @DefaultValue({"Authorization", "Content-Type", "Idempotency-Key", "X-Requested-With"}) List<String> allowedHeaders,
            @DefaultValue({"Idempotency-Replayed"}) List<String> exposedHeaders,
            @DefaultValue("true") boolean allowCredentials,
            @DefaultValue("3600") long maxAge) {
    }
}
