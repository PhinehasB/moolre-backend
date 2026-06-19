package com.project.klare_server.payroll.dto;

import java.math.BigDecimal;
import java.util.List;

public record PayrollOverviewResponse(RunSummary run, Schedule schedule, List<PayrollRunResponse> history) {

    public record RunSummary(
            int activeEmployees,
            BigDecimal totalToPay,
            BigDecimal walletBalance,
            int coveragePercent,
            boolean walletCoversInFull,
            BigDecimal shortfall) {
    }

    public record Schedule(
            boolean automaticPayroll,
            int payDate,
            boolean notifyEmployeesBeforePayday,
            int notifyLeadDays,
            String status) {
    }
}
