package com.project.klare_server.common.idempotency;

import java.util.UUID;

public record IdempotencyOutcome(
        State state,
        UUID recordId,
        Integer responseStatus,
        String responseContentType,
        byte[] responseBody) {

    public enum State {
        STARTED,
        REPLAY,
        IN_PROGRESS,
        MISMATCH
    }

    static IdempotencyOutcome started(UUID recordId) {
        return new IdempotencyOutcome(State.STARTED, recordId, null, null, null);
    }

    static IdempotencyOutcome replay(IdempotencyRecord record) {
        return new IdempotencyOutcome(
                State.REPLAY, record.getId(), record.getResponseStatus(),
                record.getResponseContentType(), record.getResponseBody());
    }

    static IdempotencyOutcome inProgress() {
        return new IdempotencyOutcome(State.IN_PROGRESS, null, null, null, null);
    }

    static IdempotencyOutcome mismatch() {
        return new IdempotencyOutcome(State.MISMATCH, null, null, null, null);
    }
}
