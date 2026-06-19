package com.project.klare_server.auth.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.domain.RefreshToken;
import com.project.klare_server.auth.repository.RefreshTokenRepository;
import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.common.security.TokenHasher;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, SecurityProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    public IssuedToken issue(BusinessUser user, String userAgent, String ipAddress, Duration ttl) {
        return issueUntil(user, userAgent, ipAddress, Instant.now().plus(ttl));
    }

    public IssuedToken issueUntil(BusinessUser user, String userAgent, String ipAddress, Instant expiresAt) {
        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String rawToken = encoder.encodeToString(raw);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(expiresAt);
        token.setUserAgent(userAgent);
        token.setIpAddress(ipAddress);
        refreshTokenRepository.save(token);

        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(BusinessUser user) {
        refreshTokenRepository.revokeAllForUser(user, Instant.now());
    }

    public String hash(String rawToken) {
        return TokenHasher.sha256(properties.refreshToken().pepper(), rawToken);
    }

    public record IssuedToken(String rawToken, Instant expiresAt) {
    }
}
