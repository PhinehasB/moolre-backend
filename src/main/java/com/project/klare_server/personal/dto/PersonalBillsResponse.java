package com.project.klare_server.personal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PersonalBillsResponse(
        BigDecimal monthlyIncome,
        BigDecimal lockedForBills,
        BigDecimal spendable,
        Integer sweepPercentage,
        String currency,
        List<Bill> obligations) {

    public record Bill(
            UUID id,
            String name,
            String category,
            BigDecimal amount,
            String destination,
            boolean active,
            boolean locked) {
    }
}
