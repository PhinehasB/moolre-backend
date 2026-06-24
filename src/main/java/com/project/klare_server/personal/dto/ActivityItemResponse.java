package com.project.klare_server.personal.dto;

import com.project.klare_server.personal.domain.PersonalTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActivityItemResponse(
        UUID id,
        String type,
        String title,
        String subtitle,
        BigDecimal amount,
        String status,
        Instant createdAt) {

    public static ActivityItemResponse from(PersonalTransaction txn) {
        return new ActivityItemResponse(
                txn.getId(),
                txn.getType().name(),
                txn.getTitle() != null ? txn.getTitle() : txn.getDescription(),
                txn.getSubtitle(),
                txn.getAmount(),
                txn.getStatus().name(),
                txn.getCreatedAt());
    }
}
