package com.project.klare_server.personal.service;

import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.common.security.TokenHasher;
import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalRefreshToken;
import com.project.klare_server.personal.repository.PersonalRefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalRefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final PersonalRefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public PersonalRefreshTokenService(PersonalRefreshTokenRepository refreshTokenRepository, SecurityProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    public IssuedToken issue(UUID accountId, PersonalAccountType accountType, String userAgent, String ipAddress, Duration ttl) {
        return issueUntil(accountId, accountType, userAgent, ipAddress, Instant.now().plus(ttl));
    }

    public IssuedToken issueUntil(UUID accountId, PersonalAccountType accountType, String userAgent, String ipAddress, Instant expiresAt) {
        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String rawToken = encoder.encodeToString(raw);

        PersonalRefreshToken token = new PersonalRefreshToken();
        token.setAccountId(accountId);
        token.setAccountType(accountType);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(expiresAt);
        token.setUserAgent(userAgent);
        token.setIpAddress(ipAddress);
        refreshTokenRepository.save(token);

        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForAccount(UUID accountId, PersonalAccountType accountType) {
        refreshTokenRepository.revokeAllForAccount(accountId, accountType, Instant.now());
    }

    public String hash(String rawToken) {
        return TokenHasher.sha256(properties.refreshToken().pepper(), rawToken);
    }

    public record IssuedToken(String rawToken, Instant expiresAt) {
    }
}
