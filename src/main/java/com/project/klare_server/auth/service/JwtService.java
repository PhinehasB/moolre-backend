package com.project.klare_server.auth.service;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.domain.BusinessUserRole;
import com.project.klare_server.common.config.properties.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecurityProperties.Jwt properties;
    private final SecretKey signingKey;

    public JwtService(SecurityProperties securityProperties) {
        this.properties = securityProperties.jwt();
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String generateAccessToken(BusinessUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("companyId", user.getCompany().getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public AccessTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AccessTokenClaims(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                BusinessUserRole.valueOf(claims.get("role", String.class)),
                UUID.fromString(claims.get("companyId", String.class)));
    }

    public record AccessTokenClaims(UUID userId, String email, BusinessUserRole role, UUID companyId) {
    }
}
