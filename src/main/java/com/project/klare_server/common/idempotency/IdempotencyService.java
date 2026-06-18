package com.project.klare_server.common.idempotency;

import com.project.klare_server.common.config.properties.IdempotencyProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final IdempotencyProperties properties;

    public IdempotencyService(IdempotencyRecordRepository repository, IdempotencyProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public String hashRequest(String method, String path, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            if (body != null) {
                digest.update(body);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyOutcome begin(String scope, String key, String method, String path, String requestHash) {
        Instant now = Instant.now();
        Optional<IdempotencyRecord> existing = repository.findByScopeAndIdempotencyKey(scope, key);
        if (existing.isPresent()) {
            return classify(existing.get(), requestHash);
        }
        try {
            IdempotencyRecord record = IdempotencyRecord.start(
                    scope, key, method, path, requestHash, now, now.plus(properties.ttl()));
            IdempotencyRecord saved = repository.saveAndFlush(record);
            return IdempotencyOutcome.started(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            return repository.findByScopeAndIdempotencyKey(scope, key)
                    .map(record -> classify(record, requestHash))
                    .orElseGet(IdempotencyOutcome::inProgress);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID recordId, int responseStatus, String responseContentType, byte[] responseBody) {
        repository.findById(recordId).ifPresent(record ->
                record.complete(responseStatus, responseContentType, responseBody));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(UUID recordId) {
        repository.deleteById(recordId);
    }

    private IdempotencyOutcome classify(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            return IdempotencyOutcome.mismatch();
        }
        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return IdempotencyOutcome.replay(record);
        }
        return IdempotencyOutcome.inProgress();
    }
}
