package com.project.klare_server.personal.dto;

import com.project.klare_server.personal.domain.PersonalSalaryEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingSalaryResponse(
        UUID id,
        String sourceName,
        BigDecimal amount,
        BigDecimal spendableAmount,
        BigDecimal safeAmount,
        String currency,
        Instant paidAt) {

    public static PendingSalaryResponse from(PersonalSalaryEvent event, String currency) {
        return new PendingSalaryResponse(
                event.getId(),
                event.getSourceName(),
                event.getAmount(),
                event.getSpendableAmount(),
                event.getSafeAmount(),
                currency,
                event.getPaidAt());
    }
}
