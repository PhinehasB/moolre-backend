package com.project.klare_server.personal.dto;

import java.math.BigDecimal;

public record BillPaymentResponse(
        int paidCount,
        int skippedCount,
        BigDecimal totalPaid,
        String currency,
        BigDecimal freeBalance,
        BigDecimal lockedBalance) {
}
