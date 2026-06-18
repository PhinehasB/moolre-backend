package com.project.klare_server.common.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByScopeAndIdempotencyKey(String scope, String idempotencyKey);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
