package com.project.klare_server.common.idempotency;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdempotencyCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupTask.class);

    private final IdempotencyRecordRepository repository;

    public IdempotencyCleanupTask(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Purged {} expired idempotency records", removed);
        }
    }
}
