package com.project.klare_server.personal.dto;

import com.project.klare_server.personal.domain.PersonalTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        BigDecimal amount,
        BigDecimal klareFee,
        BigDecimal total,
        String network,
        String recipient,
        String recipientName,
        String status,
        String currency,
        BigDecimal freeBalance,
        Instant createdAt) {

    public static TransferResponse from(PersonalTransaction txn, String currency, BigDecimal freeBalance) {
        return new TransferResponse(
                txn.getId(),
                txn.getAmount(),
                txn.getKlareFee(),
                txn.getTotal(),
                txn.getNetwork(),
                txn.getRecipient(),
                txn.getRecipientName(),
                txn.getStatus().name(),
                currency,
                freeBalance,
                txn.getCreatedAt());
    }
}
