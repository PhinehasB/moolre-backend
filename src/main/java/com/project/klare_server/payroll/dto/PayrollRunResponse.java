package com.project.klare_server.payroll.dto;

import com.project.klare_server.payroll.domain.PayrollRun;
import com.project.klare_server.payroll.domain.PayrollRunStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public record PayrollRunResponse(
        UUID id,
        String period,
        int periodYear,
        int periodMonth,
        LocalDate runDate,
        int employees,
        int successRate,
        BigDecimal totalPaid,
        PayrollRunStatus status) {

    public static PayrollRunResponse from(PayrollRun run) {
        String period = Month.of(run.getPeriodMonth()).getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                + " " + run.getPeriodYear();
        LocalDate runDate = (run.getCompletedAt() != null ? run.getCompletedAt() : run.getCreatedAt())
                .atZone(ZoneOffset.UTC).toLocalDate();
        int successRate = run.getEmployeeCount() == 0 ? 0
                : Math.round(run.getSuccessCount() * 100f / run.getEmployeeCount());
        return new PayrollRunResponse(
                run.getId(), period, run.getPeriodYear(), run.getPeriodMonth(), runDate,
                run.getEmployeeCount(), successRate, run.getTotalAmount(), run.getStatus());
    }
}
