package com.project.klare_server.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_scope_key", columnNames = {"scope", "idempotency_key"}))
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "scope", nullable = false, length = 100)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    @Column(name = "request_path", nullable = false, length = 512)
    private String requestPath;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_content_type", length = 120)
    private String responseContentType;

    @Column(name = "response_body")
    private byte[] responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() {
    }

    public static IdempotencyRecord start(
            String scope, String idempotencyKey, String requestMethod, String requestPath,
            String requestHash, Instant now, Instant expiresAt) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.scope = scope;
        record.idempotencyKey = idempotencyKey;
        record.requestMethod = requestMethod;
        record.requestPath = requestPath;
        record.requestHash = requestHash;
        record.status = IdempotencyStatus.IN_PROGRESS;
        record.createdAt = now;
        record.expiresAt = expiresAt;
        return record;
    }

    public void complete(int responseStatus, String responseContentType, byte[] responseBody) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
    }

    public UUID getId() {
        return id;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }
}
