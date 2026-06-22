package com.project.klare_server.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(
        Instant date,
        String description,
        String reference,
        String status,
        String direction,
        BigDecimal amount) {
}
