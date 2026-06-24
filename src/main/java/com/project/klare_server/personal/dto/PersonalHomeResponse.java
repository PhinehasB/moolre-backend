package com.project.klare_server.personal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PersonalHomeResponse(
        String firstName,
        Wallet wallet,
        NextSalary nextSalary,
        List<Bill> bills) {

    public record Wallet(String currency, BigDecimal freeSpendable, BigDecimal lockedSafe) {
    }

    public record NextSalary(
            String sourceName,
            BigDecimal amount,
            LocalDate expectedDate,
            long daysUntil,
            double progress) {
    }

    public record Bill(
            UUID id,
            String name,
            String category,
            BigDecimal amount,
            String frequencyLabel,
            boolean locked,
            boolean autoSwept) {
    }
}
