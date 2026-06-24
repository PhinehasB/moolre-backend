package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalAccountType;
import com.project.klare_server.personal.domain.PersonalRefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalRefreshTokenRepository extends JpaRepository<PersonalRefreshToken, UUID> {

    Optional<PersonalRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update PersonalRefreshToken t set t.revokedAt = :now
            where t.accountId = :accountId and t.accountType = :accountType and t.revokedAt is null
            """)
    void revokeAllForAccount(
            @Param("accountId") UUID accountId,
            @Param("accountType") PersonalAccountType accountType,
            @Param("now") Instant now);
}
