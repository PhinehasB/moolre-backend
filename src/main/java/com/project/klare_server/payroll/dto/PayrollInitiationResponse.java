package com.project.klare_server.payroll.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayrollInitiationResponse(
        UUID runId,
        int employeeCount,
        BigDecimal totalAmount,
        BigDecimal walletBalance,
        String maskedPhone,
        Instant codeExpiresAt) {
}
