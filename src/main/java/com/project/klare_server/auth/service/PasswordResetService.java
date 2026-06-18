package com.project.klare_server.auth.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.domain.PasswordResetToken;
import com.project.klare_server.auth.repository.PasswordResetTokenRepository;
import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.common.error.ApiException;
import com.project.klare_server.common.error.ErrorCode;
import com.project.klare_server.common.security.TokenHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;

    private final PasswordResetTokenRepository repository;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public PasswordResetService(PasswordResetTokenRepository repository, SecurityProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public String issue(BusinessUser user) {
        Instant now = Instant.now();
        repository.invalidateActiveForUser(user, now);

        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String rawToken = encoder.encodeToString(raw);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(now.plus(properties.passwordReset().tokenTtl()));
        repository.save(token);

        return rawToken;
    }

    public BusinessUser consume(String rawToken) {
        PasswordResetToken token = repository.findByTokenHash(hash(rawToken))
                .filter(candidate -> candidate.isUsable(Instant.now()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN, "Invalid or expired reset token"));
        token.setUsedAt(Instant.now());
        return token.getUser();
    }

    private String hash(String rawToken) {
        return TokenHasher.sha256(properties.refreshToken().pepper(), rawToken);
    }
}
