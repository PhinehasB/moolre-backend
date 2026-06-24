package com.project.klare_server.personal.security;

import com.project.klare_server.common.config.properties.SecurityProperties;
import com.project.klare_server.personal.domain.PersonalAccountType;
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
public class PersonalJwtService {

    private static final String ISSUER = "klare-personal";

    private final SecurityProperties.Jwt properties;
    private final SecretKey signingKey;

    public PersonalJwtService(SecurityProperties securityProperties) {
        this.properties = securityProperties.jwt();
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String generateAccessToken(UUID id, PersonalAccountType accountType, UUID companyId, boolean mustChangePassword) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(id.toString())
                .claim("type", accountType.name())
                .claim("mustChangePassword", mustChangePassword)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey, Jwts.SIG.HS512);
        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }
        return builder.compact();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public PersonalTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        PersonalAccountType accountType = PersonalAccountType.valueOf(claims.get("type", String.class));
        String companyId = claims.get("companyId", String.class);
        return new PersonalTokenClaims(
                UUID.fromString(claims.getSubject()),
                accountType,
                companyId == null ? null : UUID.fromString(companyId),
                Boolean.TRUE.equals(claims.get("mustChangePassword", Boolean.class)));
    }

    public record PersonalTokenClaims(
            UUID id, PersonalAccountType accountType, UUID companyId, boolean mustChangePassword) {
    }
}
