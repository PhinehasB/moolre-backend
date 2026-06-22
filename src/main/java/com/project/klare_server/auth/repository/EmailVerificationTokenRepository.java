package com.project.klare_server.auth.repository;

import com.project.klare_server.auth.domain.BusinessUser;
import com.project.klare_server.auth.domain.EmailVerificationToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.user = :user and t.usedAt is null")
    int invalidateActiveForUser(@Param("user") BusinessUser user, @Param("now") Instant now);
}
